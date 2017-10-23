import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

/**
 * Server to create and bind a broker object
 */
public class BrokerServer {
	public static void main(String[] args) {
		DataPersistence dp = new DataPersistence();
		
		// If data exists
		if( dp.saveDataExists() ) {
			System.out.println("Load a broker (a) or create a new one (b)");
			Scanner s = new Scanner(System.in);
			
			boolean validInput = false;
			
			while(!validInput) {
				String input = s.nextLine();
				
				if( input.equalsIgnoreCase("a") ) {
					
					// Get a list of saved brokers
					List<String> brokers = dp.getBrokerNames();
					
					int i = 1;
					for( String str : brokers ) {
						System.out.println(i + " : " + str);
						i++;
					}
					System.out.println(i + ": Cancel");
				
					System.out.println("Select the number of your broker");
					
					// Process selection
					boolean validInput2 = false;
					while(!validInput2) {							
						// Check input
						try {
							int selection = Integer.parseInt(s.nextLine());
							
							if( selection > 0 && selection <= brokers.size()) {
								String name = brokers.get(selection-1);
								
								// Reload the object
								Broker b = dp.getBroker(name);
								
								// Rebind it and send to central server
								try {
									name = name.substring(0, name.length()-4);
									Naming.rebind(name, b);
									addToList(name);
								} catch (Exception e) {
									// Connection error
									System.err.println("Sorry an error has occured - please check your connection and the server's status");
								} 
								
								System.out.println("Reloading broker " + name + " from saved file.");
								validInput2 = true;
							} else if( selection == brokers.size()+1 ) {
								System.out.println("Cancelling reloading a broker.");
								main(null);
								validInput2 = true;
							} else {
								System.out.println("Bad input - please select a number from the list above.");
							}
								
						} catch (NumberFormatException e) {
							System.out.println("Bad input - please select a number from the list above.");
						} catch (Exception e) {
							// Connection error
							System.err.println("Sorry an error has occured - please check your connection and the server's status");
						}
					}
				} else if (input.equalsIgnoreCase("b") ) {
					createNewBroker();
					validInput = true;
				} else {
					System.out.println("Please enter a or b.");
				}
				//s.close();
			}
		} else {
			dp.createFolders();
			createNewBroker();			
		}	
	}
	
	public static void createNewBroker() {
		System.out.println("Enter the name of the broker");
		
		Scanner s = new Scanner(System.in);
		String name = s.nextLine().replace(" ", "_");
		//s.close();
		
		try {
			Naming.rebind(name, new Broker(name));
			addToList(name);
		} catch (Exception e) {
			// Connection error
			System.err.println("Sorry an error has occured - please check your connection and the server's status");
		}
	}

	private static void addToList(String name) throws MalformedURLException, RemoteException, NotBoundException {
		System.out.println("Providing broker details to the central server");
		RemoteListInterface<String> brokerListRLI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/brokerList");
		brokerListRLI.addObject(name);		
	}
}
