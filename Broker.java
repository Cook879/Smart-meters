import java.io.ObjectInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Broker class
 */
public class Broker extends UnicastRemoteObject implements BrokerInterface {
	
	/**
	 * To serialize
	 */
	private static final long serialVersionUID = -8209183005251194195L;
	
	private String name;
	
	// Customers mapped to object stubs and to their histories
	private Map<String, MeterInterface> customers;
	private Map<String, List<Integer>> histories;
	
	// For saving - transient as non-serializable 
	private transient DataPersistence dp;
	
	/**
	 * Constructs a new Broker
	 * 
	 * @param name String
	 * @throws RemoteException
	 */
	public Broker(String name) throws RemoteException {
		super();
		
		this.name = name;
		
		customers = new HashMap<String, MeterInterface>();
		histories = new HashMap<String, List<Integer>>();
		
		dp = new DataPersistence();
		dp.saveBroker(this);
	}
	
	/**
	 * Handles deserialization of the object
	 * 
	 * @Override
	 * @param inputStream ObjectInputStream
	 */
	private void readObject(ObjectInputStream inputStream) {
		try {
			inputStream.defaultReadObject();
	
			dp = new DataPersistence();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Receives a request from a meter to find a better deal
	 * 
	 * @param id String final : ID of the Meter
	 * @param m MeterInterface
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean receiveRequest(final String id, MeterInterface m) throws RemoteException {
		
		print("Broker " + name + " has a new customer " + id + ".\n" );
			
		// Add meter to the customer list
		customers.put(id, m);
		dp.saveBroker(this);

		// Create new thread so we can run the process with a delay without blocking the meter (who's waiting for the return)
		Thread thread = new Thread () {
			  public void run () {
				  // Sleep for testing purposes only
				  try {
						Thread.sleep(30000);
				  } catch (InterruptedException e) {
					  e.printStackTrace();
				  }
				  
				  // If the getHistory() method is successful
				  if( getHistory(id) ) {
					  
					  // Sleep for testing purposes only
					  try {
						  Thread.sleep(60000);
					  } catch (InterruptedException e) {
						  e.printStackTrace();
					  }
					  
					  // Get the best deal
					  String powerCompany = calculateBestDeal(id);
					
					  // Send it
					  sendDeal(id, powerCompany);
				  } else {
					  // Hopefully shouldn't ever reach here.
					  print("Sorry - an error has occured with customer" + id + ".\n");
				  }
			}
		};
		thread.start();
		
		return true;		
	}
	
	/**
	 * Calculates the best deal for a given meter
	 * 
	 * @param id String : of the meter
	 * @return String : name of the best power company
	 */
	private String calculateBestDeal(String id) {
		print("Calculating best deal for meter " + id + ".\n");
		
		// Get the history of the meter
		List<Integer> readings = histories.get(id);
		
		try {			
			// Obtain list of companies
			@SuppressWarnings("unchecked")
			RemoteListInterface<String> companyListRLI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/companyList");
			ArrayList<String> companyList = (ArrayList<String>) companyListRLI.accessList();
			
			Random r = new Random();
			
			// Cost of the history on the best tariff and name of the company
			int bestTariff = Integer.MAX_VALUE;
			String bestTariffName = null;
		
			for( String company : companyList ) {
				try {
					Thread.sleep(10000);
					PowerCompanyInterface pc = (PowerCompanyInterface) Naming.lookup("rmi://localhost/" + company);
					
					print("Updating tariff details for " + company);

					// Get tariff details
					Tariff t = pc.getTariffDetails();
					int dayCost = t.getDayCost();
					int nightCost = t.getNightCost();
					int discount = t.getDiscount();
					int threshold = t.getThreshold();
					
					// Calculate total
					int total = 0;
					for( Integer reading : readings ) {
						if( reading == 0 ) {
							// Do nothing
						} else {
							// Get a random amount (less than half) of readings that occurred at night
							int nightUnits = r.nextInt(reading/2);
							int dayUnits = reading - nightUnits;

							int semiTotal = (dayUnits*dayCost) + (nightUnits*nightCost);

							// Apply discount if neccesary 
							if( reading >= threshold) {
								semiTotal = semiTotal * discount;
							}
							total += semiTotal;
						}
						
					}
					
					print("Power Company " + company + " would have cost meter " + id + " £" + total + " for their previous readings.\n");
					
					// If better, update best details
					if( total < bestTariff ) {
						bestTariff = total;
						bestTariffName = company;
					}
					
				} catch (Exception e) {
					if( connectionLost(e) ) {
						// We don't want to waste time trying to connect to a lost power company
						// Just ignore and move on
						continue;
					} else {
						e.printStackTrace();
					}
				}
			}
			
			print("Best tariff for meter " + id + " is " + bestTariffName);
			return bestTariffName;
		} catch (Exception e) {
			// Lost connection to server - vital we regain it to continue
			if( connectionLost(e) ) {
				reconnectServer();
				calculateBestDeal(id);
			} else {
				e.printStackTrace();
			}
		}		
		return null;
	}

	/**
	 * Sends a deal to the customer 
	 * 
	 * @param id String
	 * @param pcName String
	 * @return boolean
	 */
	private boolean sendDeal(String m, String pcName) {			
		try {
			// Gets the details of the chosen tariff again
			PowerCompanyInterface pc = (PowerCompanyInterface) Naming.lookup("rmi://localhost/" + pcName);
	
			// Sends the offer to the client
			print("Sending offer to meter " + m);
			
			boolean accepted = customers.get(m).receiveOffer(pcName, pc.getTariffDetails().toString());
			
			// Processes response
			if( accepted ) {
				print(m + " has accepted the deal you suggested with " + pcName + ".\n");
				// Unregister from old - while returns a boolean should always be true.
				unregister(m);
				// Register with new - while returns a boolean should always be true.
				register(m, pc, pcName);
				// Update customer's listings.
				customers.get(m).setPowerCompany(pc);
			} else
				print(m + " has rejected the deal you suggest with " + pcName + ".\n");
				
			// No longer a customer
			customers.remove(m);
			histories.remove(m);
		} catch (Exception e) {
			if( connectionLost(e)) {
				// Can't be sure what we lost connection to so try both
				reconnectPowerCompany(pcName);
				reconnectMeter(m);
				sendDeal(m, pcName);
				return true;
			} else {
				print("Sorry an error has occured \n");
				return false;
			}
		}
			
		dp.saveBroker(this);
		return true;
	}

	/**
	 * Gets the history of the meter via a PULL request
	 * 
	 * @param id String : Meter id
	 * @return boolean
	 */
	private boolean getHistory(String id) {
		try {
			ArrayList<Integer> history = customers.get(id).getHistory(name);
			
			print("Broker " + name + " has received " + id + "'s readings history"+ ".\n");

			histories.put(id, history);
			
			// Update saved version of broker
			dp.saveBroker(this);
			
			return true;
		} catch (RemoteException e) {
			// Reconnect and try again
			if( connectionLost(e) ) {
				reconnectMeter(id);
				getHistory(id);
				return true;
			} else {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	/**
	 * Unregisters a meter from a power company
	 * 
	 * @param id String : id of the meter
	 * @return boolean
	 */
	private boolean unregister(String id) {
		try {
			MeterInterface m = customers.get(id);
			
			// Can only unregister if m has a power company already
			if( m.hasPowerCompany() ) {
				String pcName = m.getPowerCompanyName();
				PowerCompanyInterface pc = (PowerCompanyInterface) Naming.lookup("rmi://localhost/" + pcName);

				print("Unregistering customer " + id + " from " + pcName+ ".\n");
				
				// Always returns true
				return pc.removeCustomer(id);
			} else
				return true;
		} catch (Exception e) {
			if( connectionLost(e) ) {
				// Reconnect to both meter and power company
				reconnectMeter(id);
				try {
					reconnectPowerCompany(customers.get(id).getPowerCompanyName());
				} catch (RemoteException e1) {
					// shouldn't be an issue
				}
				
				// Restart method
				unregister(id);
				
				return true;
			} else {
				// Should hopefully not reach here
				e.printStackTrace();
				return false;
			}
		}
	}
	
	/**
	 * Registers a meter with it's new power company
	 * 
	 * @param id String : id of the meter
	 * @param pc PowerCompanyInterface
	 * @param pcName String
	 * @return
	 */
	private boolean register(String id, PowerCompanyInterface pc, String pcName) {
		
		try {
			print("Registering customer " + id + " to " + pcName + ".\n");
			
			// Always returns true
			return pc.addCustomer(id, customers.get(id), customers.get(id).getLatestReading());
		} catch (RemoteException e) {
			// If we lost connection to one of the entities, reconnect and restart
			if( connectionLost(e) ) {
				reconnectMeter(id);
				try {
					reconnectPowerCompany(customers.get(id).getPowerCompanyName());
				} catch (RemoteException e1) {
					e1.printStackTrace();
				}
				
				register(id, pc, pcName);
				return true;
			} else {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	/**
	 * Accessor for the name variable
	 * 
	 * @Override
	 * @return String
	 * @throws RemoteException
	 */
	public String getName() throws RemoteException {
		return name;
	}
	
	/**
	 * Prints output to the command line, but prepends with the date and broker name
	 * 
	 * @param s String
	 */
	private void print(String s) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[Broker " + name + "][" + timeStamp + "] " + s);
	}
		
	/**
	 * Attempts to reconnect to a power company
	 * 
	 * @param name String
	 */
	private void reconnectPowerCompany(String name) {
		print("Connection lost to power company " + name + " - trying to re-establish contact.");
		
		boolean connected = false;
		
		// Keep trying till no more errors!
		while( !connected ) { 
			try {
				PowerCompanyInterface pc = (PowerCompanyInterface) Naming.lookup(name);
				pc.ping();
				
				print("Connection re-established to power company " + name + " - restarting current process.");
				connected = true;
			} catch (Exception e) {
				// Still getting errors == try again
				if( connectionLost(e) )
					continue;
				// Else break 'cos something has gone wrong
				e.printStackTrace();
				break;
			}
		}
	}
	
	/**
	 * Attempts to reconnect to the central server
	 */
	private void reconnectServer() {
		print("Connection lost to server - trying to re-establish contact.");
		
		boolean connected = false;
		
		while( !connected ) { 
			try {
				@SuppressWarnings("unchecked")
				RemoteListInterface<String> companyListRTI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/companyList");
				companyListRTI.accessList();
				print("Connection re-established to central server - restarting current process.");		
				connected = true;
			} catch (Exception e) {
				if( connectionLost(e) )
					continue;
				e.printStackTrace();
				break;
			}
		}
	}

	/**
	 * Attempts to reconnect to a meter
	 * 
	 * @param id String
	 */
	private void reconnectMeter(String id) {
		print("Connection lost to meter " + id + " - trying to re-establish contact.");
		
		boolean connected = false;
				
		while( !connected ) { 
			try {
				MeterInterface m = (MeterInterface) Naming.lookup(id);
				
				// Test the newly found object
				m.ping();
				
				// Update our records
				customers.put(id, m);
				
				print("Connection re-established to meter " + id + " - restarting current process.");	
				connected = true;
			} catch (Exception e) {
				if( connectionLost(e) )
					continue;
				e.printStackTrace();
				break;
			}
		}
	}

	/**
	 * Checks if the exception is a type of RemoteException
	 * 
	 * @param e Exception
	 * @return boolean
	 */
	private boolean connectionLost(Exception e) {
		return e.getClass().getSuperclass().equals(new RemoteException("").getClass());
	}

	/**
	 * A basic method to allow other entities to test their connection with the broker
	 * 
	 * @Override
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean ping() throws RemoteException {
		return true;
	}
}
