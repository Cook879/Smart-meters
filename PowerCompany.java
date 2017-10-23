import java.io.ObjectInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Power Company object
 */
public class PowerCompany extends UnicastRemoteObject implements PowerCompanyInterface {

	private String name;
	private Tariff tariff;

	// Maps a meter id to an interface and to the latest reading
	private Map<String, MeterInterface> customers;
	private Map<String, Integer> readings;
	
	private transient Timer timer;
	private transient DataPersistence dp;
	
	private static final long serialVersionUID = 8696220143569633358L;
	
	/**
	 * Constructor 
	 * 
	 * @Override
	 * @param name String
	 * @throws RemoteException
	 */
	protected PowerCompany(String name) throws RemoteException {
		super();
		
		this.name = name;
		
		// Calculate a random tariff
		Random r = new Random();
		if( r.nextInt(2) == 0) {
			// Same night and day
			int fee = r.nextInt(100)+1;
			// day cost, night cost, discount, threshold
			this.tariff = new Tariff(fee, fee, r.nextInt(30)/100, r.nextInt(500)+100);
		} else {
			// Cheaper at night
			int dayCost = r.nextInt(100)+1;
			this.tariff = new Tariff(dayCost, r.nextInt(dayCost)+1, r.nextInt(30)/100, r.nextInt(500)+100);
		}
		
		// Set up variables
		customers = new HashMap<String,MeterInterface>();
		readings = new HashMap<String, Integer>();
		
		timer = new Timer();		
		dp = new DataPersistence();
		dp.savePowerCompany(this);
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
	 * Adds a customer to the Power Company
	 * 
	 * @Override
	 * @param id String
	 * @param m MeterInterface
	 * @param int latestReading
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean addCustomer(String id, MeterInterface m, int latestReading) throws RemoteException {
		customers.put(id, m);
		readings.put(id, latestReading);
			
		print("New customer registered with meter id " + id + ".\n");	
			
		dp.savePowerCompany(this);
			
		return true;	
	}
	
	/**
	 * Removes a customer from the Power Company
	 * 
	 * @Override
	 * @param id String
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean removeCustomer(String id) throws RemoteException {
		customers.remove(id);
		readings.remove(id);
		
		print(name + " has lost customer " + id);
		dp.savePowerCompany(this);
	
		return true;
	}
	
	/**
	 * Receives a new meter reading
	 * 
	 * @Override
	 * @param id String
	 * @param newReading int
	 * @return int : Amount of electricity used between readings
	 * @throws RemoteException
	 */
	public int receiveReading(String id, int newReading) throws RemoteException {
		// Make sure the sender is a customer
		if( !customers.containsKey(id) )
			return -1;
		
		print("Recieving a new reading for meter " + id + ".\n");
		
		// Sleep for testing purposes
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Get previous reading, store new one
		int prevReading = readings.get(id);
		readings.put(id, newReading);
		
		print("Added a new reading for meter " + id + " of " + newReading);
		dp.savePowerCompany(this);
		
		Random r = new Random();
		int pay = r.nextInt(10);
		// One in 10 chance they don't pay (for simulation purposes)
		if( pay == 9 )
			timer.schedule(new SendCommand(id), 150000);
	
		// Kinda a pointless return as they can work this out on the meter but meh
		return newReading - prevReading;
	}

	/**
	 * Receives an alert from a meter
	 * 
	 * @Override
	 * @param m String
	 * @param alert int
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean receiveAlert(String m, int alert) throws RemoteException {
		if( !customers.containsKey(m) )
			return false;
		
		String alertStr;
		
		if( alert == 0) {
			alertStr = "Customer messing around with the meter";
		} else if (alert == 1) {
			alertStr = "Customer using excessive amounts of power";
		} else {
			alertStr = "Random alert is random";
		}
		
		print("Recieved alert from meter " + m + " " + alertStr);
		
		print("Sending appropriate response to the meter " + m);
		
		try{
			customers.get(m).receiveCommand(alert, name);
			return true;
		} catch (Exception e) {
			if( connectionLost(e) ) {
				// Second time lucky ;)
				reconnectMeter(m);
				receiveAlert(m, alert);
				return true;
			} else {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	/**
	 * Getter for the Name variable
	 * 
	 * @Override
	 * @return String
	 * @throws RemoteException
	 */
	public String getName() throws RemoteException {
		return name;
	}
	
	/**
	 * Getter for the tariff
	 * 
	 * @Override
	 * @return Tariff
	 * @throws RemoteException
	 */
	public Tariff getTariffDetails() throws RemoteException {
		return tariff;
	}
	
	/**
	 * Prints output to the command line, but prepends with the date and power company name
	 * @param s String
	 */
	private void print(String s) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[PowerCompany " + name + "][" + timeStamp + "] " + s);
	}
		
	/**
	 * Attempts to reconnect to a meter
	 * 
	 * @param id String
	 */
	private void reconnectMeter(String id) {
		System.out.println("Connection lost to meter " + id + " - trying to re-establish contact.");
		
		boolean connected = false;
		
		while( !connected ) { 
			try {
				MeterInterface m = (MeterInterface) Naming.lookup("rmi://localhost/" + id);
				m.ping();
				
				// Update our records
				customers.put(id, m);
				
				System.out.println("Connection re-established to meter " + id + " - restarting current process.");	
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
	 * A basic method to allow other entities to test their connection with the power company
	 * 
	 * @Override
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean ping() throws RemoteException {
		return true;
	}
	
	/**
	 * Internal class for sending commands to meters via tasks
	 * Only switch off errors sent through timer - rest sent when generated
	 */
	class SendCommand extends TimerTask {
		
		String id;
		
		/**
		 * Constructor 
		 *
		 * @param id String
		 */
		public SendCommand(String id) {
			this.id = id;
		}
		
		/**
		 * Runs the task 
		 * 
		 * @Override
		 */
		public void run() {
			try {
				// Check still a customer - may have unregistered between when timer started and now
				if( customers.containsKey(id)) {
					try {
						print("Meter " + id + " has failed to pay their bill. Switiching off power.");
						customers.get(id).receiveCommand(5, name);
					} catch (Exception e) {
						if( connectionLost(e) ) {
							reconnectMeter(id);
							
							// Time is 0 so send automatically this time
							timer.schedule(new SendCommand(id),0);
						} else {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}