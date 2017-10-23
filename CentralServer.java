import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

/**
 * Central server
 * 
 * @author Richard Cook
 */
public class CentralServer {

	/**
	 * Main method runs the central server and creates new remote lists for each entity
	 * 
	 * @param args String[]
	 */
	public static void main(String[] args) {
		DataPersistence dp = new DataPersistence();
		
		//If data exists
		if( dp.saveDataExists() ) {
			System.out.println("Load previous central server (a) or create a new one (b)");
			Scanner s = new Scanner(System.in);
			
			boolean validInput = false;
			
			while(!validInput) {
				String input = s.nextLine();
				
				if( input.equalsIgnoreCase("a") ) {
					print("Central server booting up.");

					try {
						Naming.rebind("meterList", dp.getMeterList());
						print("Meter list registered.");
						Naming.rebind("companyList", dp.getPowerCompanyList());
						print("Company list registered.");
						Naming.rebind("brokerList", dp.getBrokerList());
						print("Broker list registered.");
					} catch (Exception e) {
						System.err.println("Error - Java RMI registry is not currently running. Please turn it on and try again.");
					} 
					
				} else if (input.equalsIgnoreCase("b") ) {
					newLists();
					validInput = true;
				} else {
					System.out.println("Please enter a or b.");
				}
			}
			s.close();
		} else {
			dp.createFolders();
			newLists();			
		}	
	}
	
	public static void newLists() {
		print("Central server booting up.");

		try {
			Naming.rebind("meterList", new RemoteList<String>(0));
			print("Meter list registered.");
			Naming.rebind("companyList", new RemoteList<String>(1));
			print("Company list registered.");
			Naming.rebind("brokerList", new RemoteList<String>(2));
			print("Broker list registered.");
		} catch (Exception e) {
			System.err.println("Error - Java RMI registry is not currently running. Please turn it on and try again.");
		} 
	}


	private static void print(String s) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[Central server][" + timeStamp + "] " + s);
	}
}