import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Meter extends UnicastRemoteObject implements MeterInterface {


	private static final long serialVersionUID = 1731042652319629745L;

	private String id;
	
	// Readings history
	private ArrayList<Integer> meterReadings;
	
	// Power Company details
	private PowerCompanyInterface powerCompany = null;
	private String pcName;
	
	// Broker details
	private BrokerInterface broker = null;
	private String brokerName;
	
	private transient Timer timer;
	private transient DataPersistence dp;
	
	/**
	 * Constructor
	 * 
	 * @param id String
	 * @throws RemoteException 
	 */
	protected Meter(String id) throws RemoteException  {
		this.id = id;
		print("Setting up meter " + id);
		
		// Add a first meter reading of 0
		meterReadings = new ArrayList<Integer>();
		meterReadings.add(0);
		
		timer = new Timer();
		
		dp = new DataPersistence();
		dp.saveMeter(this);
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
	
			timer = new Timer();
			dp = new DataPersistence();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Runs the meter (command line interface)
	 */
	public void runMeter() {
		print("SmartMeter " + id + " started\n");
					
		Scanner in = new Scanner(System.in);
		boolean operating = true;
			
		// Command line interface
		while(operating) {
			print("The Meter accepts the following commands");
			print("1. Register with a power company");
			print("2. Unregister with current power company");
			print("3. Send request to find a better deal to broker");
			print("4. Exit\n");
			print("Type a number:");
			
			String input = in.nextLine();
			
			switch(input) {
				case "1":
					registerPowerCompany();
					break;
						
				case "2":
					unregisterPowerCompany();
					break;
					
				case "3":
					requestBroker();
					break;
				case "4":
					print("Exiting SmartMeter");
					operating = false;
					break;
						
				default:
					print("Unrecognized input - please try again.\n");
					break;
			}
		} 
		//in.close();
	}
	
	/**
	 * Registers a new power company
	 */
	private void registerPowerCompany() {
		print("Register with a power company\n");
			
		// Checks if they are registered already
		try {
			if( hasPowerCompany() ) {
				print("Already registered with power company:" + pcName + "\n");
				return;
			}
		} catch (RemoteException e) {
			// Shouldn't have a connection error here as we're in the object 
		} 
			
		// If not, get a list of companies
		try {
			RemoteListInterface<String> companyListRTI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/companyList");
			List<String> companyList = (ArrayList<String>) companyListRTI.accessList();
			
			print("Choose your new power company");
			
			int i = 1;
			for( String company : companyList ) {
				print(i + ": " + company);
				i++;
			}
			
			print(i + ": Cancel");
			
			Scanner s = new Scanner(System.in);
			print("Select the number of the power company you'd like to register with:");
							
			boolean validInput = false;
			
			while(!validInput) {
				// Check input
				String in = s.nextLine();
				try {
					int selection = Integer.parseInt(in);
				
					if( selection > 0 && selection <= companyList.size()) {
						// Get the associated power company object
						pcName = companyList.get(selection-1);

						print("Registering with " + pcName);
						powerCompany = (PowerCompanyInterface) Naming.lookup("rmi://localhost/" + pcName);
					
						if( !powerCompany.addCustomer(id, this, getLatestReading()) ) {
							print("Oops error. Please try again later.");
							powerCompany = null;
							pcName = null;
						}
						
						// Set up scheduled (every 2 minute) sending of readings and random alerts
						timer.schedule(new SendReadings(), 60000, 60000);
						Random r = new Random();
						timer.schedule(new SendAlert() , r.nextInt(300000)+60000);

						validInput = true;
					} else if( selection == companyList.size()+1 ) {
						print("Cancelling registration with a power company.");
						validInput = true;
					} else {
						print("Bad input - please select a number from the list above.");
					}
					print("End registration with a power company\n");							
				} catch (NumberFormatException e) {
					print("Bad input - please select a number from the list above.");
				} catch (RemoteException e) {
					
					if( connectionLost(e) ) {
						// Remove any changes we may have made
						powerCompany = null;
						pcName = null;
						
						// Error message and try again
						print("Can't connect to the selected power company. Please try again later or register with another company.");
						registerPowerCompany();
						validInput = true;
					} else {
						e.printStackTrace();
					}
				}
				
			}	
			//s.close();
		} catch (MalformedURLException | RemoteException | NotBoundException e1) {
			if( connectionLost(e1) ) {
				// Bad server connection which we need to fix to access power company list
				reconnectServer();
				registerPowerCompany();
			} else {
				e1.printStackTrace();
			}
		}

		dp.saveMeter(this);
	}

	/**
	 * Unregisters the meter with it's power company
	 */
	private void unregisterPowerCompany() {
		print("Unregister with your power company\n");

		// Shouldn't error as we are calling internally 
		try {
			if( !hasPowerCompany() ) {
				print("You're not currently registered with any power company.");
				return;
			}
		} catch (RemoteException e) {}
		
		print("Are you sure you want to leave " + pcName + " (yes/no)?");
		
		Scanner s2 = new Scanner(System.in);
		
		boolean validInput = false;
		while(!validInput) {

			// Get String
			String in = s2.nextLine();
			
			try {
				if( in.equalsIgnoreCase("yes") ) {
					if( powerCompany.removeCustomer(id) ) {
						print("You've left " + pcName + "!\n");
						
						this.powerCompany = null;
						pcName = null;
					} else {
						System.err.println("Sorry an error has occured");
					}
						
					validInput = true;
				} else if( in.equalsIgnoreCase("no") ) {
					print("Cancelled operation\n");
					validInput = true;
				} else {
					print("Please answer yes or no");
				}
			} catch (RemoteException e) {
				if( connectionLost(e) ) {
					// Reconnect to the power company and try again
					reconnectPowerCompany();
					unregisterPowerCompany();
				} else {
					e.printStackTrace();
				}
			}
		}
		//s2.close();
		dp.saveMeter(this);
	}

	/**
	 * Requests the services of a broker
	 */
	private void requestBroker() {
		try {
			if( hasBroker() ) {
				// Only have one broker at a time
				print("You're already in contact with " + brokerName + " - please wait for a response.\n");
				return;
			}
			if( hasPowerCompany() ) {
				print("You already have a power company. Are you sure you want to contact a broker? (yes/no)");
				Scanner s2 = new Scanner(System.in);
					
				boolean validInput = false;
				
				while(!validInput) {
					String input = s2.nextLine();
					if( input.equalsIgnoreCase("no") ) {
						print("Cancelling contacting a broker");
						//s2.close();
						return;
					} else if( input.equalsIgnoreCase("yes") ) {
						validInput = true;
					} else {
						print("Invalid input - please enter 'yes' or 'no'.\n");
					}
				}
				
				//s2.close();
			} 
		} catch (RemoteException e1) {
			// Shouldn't error as local
		}
		
		try {
			// Get a list of brokers
			RemoteListInterface<String> brokerListRLI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/brokerList");
			ArrayList<String> brokerList = (ArrayList<String>) brokerListRLI.accessList();
			
			print("Select a broker");
				
			int i = 1;
			for( String broker : brokerList ) {
				print(i + ": " + broker);
				i++;
			}
				
			print(i + ": Cancel");
				
			Scanner s = new Scanner(System.in);
			print("Select the number of the broker you'd like to shop with:");
							
			boolean validInput = false;
			while(!validInput) {					
				String in = s.nextLine();
				try {
					int selection = Integer.parseInt(in);
						
					if( selection > 0 && selection <= brokerList.size()) {
						
						// Get the associated broker object
						brokerName = brokerList.get(selection-1);
						broker = (BrokerInterface) Naming.lookup("rmi://localhost/" + brokerName);
							
						print("Connecting with " + brokerName);
							
						// Contacting broker
						broker.receiveRequest(id, this);
						
						print("Broker contacted - they will get back to you soon!\n");
													
						validInput = true;
					} else if( selection == brokerList.size()+1 ) {
						print("Cancelling requesting a broker.");
						validInput = true;
					} else {
						print("Bad input - please select a number from the list above.");
					}
						
				} catch (NumberFormatException e) {
					print("Bad input - please select a number from the list above.");
				} catch (RemoteException e) {
					if( connectionLost(e) ) {
						broker = null;
						brokerName = null;
						print("Can't connect to the selected broker. Please try again later or try another broker.");
						requestBroker();
						validInput = true;
					} else {
						e.printStackTrace();
					}	
				}
				//s.close();
			}
		} catch (MalformedURLException | RemoteException | NotBoundException e1) {
			if( connectionLost(e1) ) {
				reconnectServer();
				requestBroker();
			} else {
				e1.printStackTrace();
			}
		}
		
		dp.saveMeter(this);
	}
	
	/**
	 * Checks if the meter has a PowerCompany
	 * 
	 * @Override
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean hasPowerCompany() throws RemoteException {
		return !(powerCompany == null);
	}
	
	/**
	 * Checks if the meter has a broker
	 * 
	 * @return boolean
	 */
	private boolean hasBroker() {
		return !(broker == null);
	}

	/**
	 * Gets the id of the meter
	 * 
	 * @Override
	 * @return String
	 * @throws RemoteException
	 */
	public String getId() throws RemoteException {
		return id;
	}
	
	/**
	 * Get's the reading history of the meter
	 * 
	 * @Override
	 * @return ArrayList<Integer>
	 * @throws RemoteException
	 */
	public ArrayList<Integer> getHistory(String brokerName) throws RemoteException {
		// Only the registered broker can send this request
		if( !this.brokerName.equals(brokerName) )
			return null;
		print("Broker " + brokerName + " has requested our readings history. Sending now\n");
		
		// Sleep for testing only
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return meterReadings;
	}

	/**
	 * Gets the latest reading - unused now
	 * 
	 * @Override
	 * @return int
	 * @throws RemoteException
	 */
	public int getLatestReading() throws RemoteException {
		return meterReadings.get(meterReadings.size()-1);
	}	
	
	/**
	 * Receives and processes a command from the power company
	 * 
	 * @Override
	 * @param command int
	 * @param source String
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean receiveCommand(int command, String source) throws RemoteException {
		// Can only accept from registered power company
		if( !pcName.equals(source) )
			return false;
		if( command == 5 ) {
			print("Bill hasn't been paid - turning meter off.");
			print("Bill paid - meter turned back on.\n");
		} else if (command == 0) {
			print("Warning - you may not alter or modify your smart meter\n");
		} else if (command == 1) {
			print("You are using an excessive amount of power\n");
		} else {
			print("A random command has appeared!\n");
		}
		return true;
	}
	
	/**
	 * Receives an offer from the Broker
	 * 
	 * @Override
	 * @param name String : Name of Power Company
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean receiveOffer(String name, String tariff) throws RemoteException  {
		
		print("The broker has found the best deal for you!");
			
		print("Power company: " + name + ". Tariff details are: " + tariff);
			
		print("Accept offer? (yes/no) [due to scanner issues it may take 3 or 4 attempts to anwser - keep going until get a valid response]");
				
		Scanner s = new Scanner(System.in);
		
		broker = null;
		brokerName = null;	
		
		boolean validInput = false;
		while(!validInput) {
			String answer = s.nextLine();
			if( answer.equalsIgnoreCase("yes") ) {
				//s.close();
				return true;
			} else if( answer.equalsIgnoreCase("no") ) {
				//s.close();
				return false;
			} else {
				print("Bad input - please reply yes or no to the broker's deal");
			}
		}	

		
		// Pointless as we never leave the while loop but Java disagrees
		//s.close();
		return false;
	}
	
	/**
	 * Prints output to the command line, but prepends with the date and meter name
	 * 
	 * @param s String
	 */
	private void print(String s) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[Meter " + id + "][" + timeStamp + "] " + s);
	}
	
	/**
	 * Updates the Meter's PowerCompany (for Broker use)
	 * 
	 * @Override
	 * @param pc PowerCompanyInterface
	 * @return boolean
	 */
	public boolean setPowerCompany(PowerCompanyInterface pc) throws RemoteException {
		this.powerCompany = pc;
		pcName = pc.getName();
		
		dp.saveMeter(this);
		
		print("You are now registered with " + pcName + "\n");
		
		// Set up scheduled (every 2 minute) sending of readings and random alerts
		timer.schedule(new SendReadings(), 60000, 60000);
		Random r = new Random();
		timer.schedule(new SendAlert() , r.nextInt(300000)+60000);
		
		return true;
	}

	/**
	 * Getter for pcName
	 * 
	 * @Override
	 * @returns String
	 * @throws RemoteException
	 */
	public String getPowerCompanyName() throws RemoteException {
		return pcName;
	}

	/**
	 * Attempts to reconnect to a power company
	 */
	private void reconnectPowerCompany() {
		print("Connection lost to power company " + pcName + " - trying to re-establish contact.");
		
		boolean connected = false;
		
		// Keep trying till no more errors!
		while( !connected ) { 
			try {
				PowerCompanyInterface pc = (PowerCompanyInterface) Naming.lookup(pcName);
				pc.ping();
				powerCompany = pc;
				
				print("Connection re-established to power company " + pcName + " - restarting current process.");
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
	 * Attempts to reconnect to a broker
	 */
	private void reconnectBroker() {
		print("Connection lost to broker " + brokerName + " - trying to re-establish contact.");
		
		boolean connected = false;
		
		// Keep trying till no more errors!
		while( !connected ) { 
			try {
				BrokerInterface b = (BrokerInterface) Naming.lookup(brokerName);
				b.ping();
				broker = b;
				
				print("Connection re-established to broker " + brokerName + " - restarting current process.");
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
	
	/**
	 * Internal class representing the task of sending readings to the power company
	 */
	class SendReadings extends TimerTask {
		private Integer newReading;
		
		/**
		 *  Constructor - Used when not recursively called
		 */
		public SendReadings() {}
		
		/**
		 * Constructor - Used when already generated the new reading
		 * @param reading
		 */
		public SendReadings(int reading) {
			this.newReading = reading;
		}
		
		/**
		 * The main method of the task
		 * 
		 * @Override
		 */
		public void run() {

			try {
				// If we don't have a power company, we can't send readings!
				// Important as may have unregistered since we set the timer
				if( !hasPowerCompany() ) {
					return;
				}
			} catch (RemoteException e1) {
				// Shouldn't error
				e1.printStackTrace();
			}
			
			// If we haven't got a reading, let's make one!
			if( newReading == null ) {		
				print("Sending meter readings to your power company");
				
				// Make a new reading
				Random r = new Random();
				int increaseBy = r.nextInt(500);
				newReading = meterReadings.get(meterReadings.size()-1) + increaseBy;
				
				meterReadings.add(newReading);
			}
			
			// Send new reading
			try {
				// Return pretty useless tbh
				int diff = powerCompany.receiveReading(id, newReading);
				if (diff == -1)
					print("An Error has occured!");
				else
					print("Used " + diff + " units of electrcity since last reading\n");		
				newReading = null;
			} catch (RemoteException e) {
				// If we lose connection, try and get it again from the server
				if( connectionLost(e) ) {
					reconnectPowerCompany();
					timer.schedule(new SendReadings(newReading), 0);
				} else {
					e.printStackTrace();
				}
			}
			dp.saveMeter(Meter.this);
		}
	}
	
	/**
	 * Internal class representing the task of sending alerts to the powere company
	 */
	class SendAlert extends TimerTask {
		private Integer alert;
		
		/**
		 *  Constructor - Used when not recursively called
		 */
		public SendAlert(){}
		
		/**
		 * Constructor - Used when already generated the new reading
		 * @param reading
		 */
		public SendAlert(int alert) {
			this.alert = alert;
		}
		
		/**
		 * The main method of the task
		 * 
		 * @Override
		 */
		public void run() {
			Random r = new Random();

			// If we don't have a power company we can't alert anyone!
			try {
				if( !hasPowerCompany() ) {
					return;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
			if( alert == null ) {
				alert = r.nextInt(3);
			
				if( alert == 0) {
					print("Warning - you may not alter or modify your smart meter");
				} else if (alert == 1) {
					print("You are using an excessive amount of power");
				} else {
					print("A random alert has appeared!");
				}
				print("Sending an alert to your power company");
			}
			
			try {
				powerCompany.receiveAlert(id, alert);
			} catch (RemoteException e) {
				if( connectionLost(e) ) {
					reconnectPowerCompany();
					// No delay on this - try again straight away
					timer.schedule(new SendAlert(alert), 0);
				} else {
					e.printStackTrace();
				}
			}
			
			// Randomly schedule a new alert
			timer.schedule(new SendAlert() , r.nextInt(300000)+60000);
		}
	}
}