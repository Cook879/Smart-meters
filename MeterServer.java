import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


/**
 * Server to create and bind a meter object
 */
public class MeterServer {
	
	/**
	 * Loads or creates a meter instance, binding it to the registry.
	 * 
	 * @param args String[]
	 */
	public static void main(String[] args) {
		DataPersistence dp = new DataPersistence();
		
		// Check for persistent data
		if( dp.meterSaveDataExists() ) {
			
			System.out.println("Load a meter (a) or create a new one (b):");
			Scanner s = new Scanner(System.in);
			
			boolean validInput = false;
			
			while( !validInput ) {
				String input = s.nextLine();
				
				if( input.equalsIgnoreCase("a") ) {
					
					// Get list of saved meters
					List<String> meters = dp.getMeterNames();
				
					int i = 1;
					for( String str : meters ) {
						System.out.println(i + " : " + str);
						i++;
					}
					
					System.out.println(i + " : " + "Cancel");
					
					System.out.println("Select the number of your meter:");

					boolean validInput2 = false;
					while( !validInput2 ) {
						try {
							int selection = s.nextInt();
							if( selection > 0 && selection <= meters.size() ) {
								String id = meters.get(selection-1);
								Meter m = dp.getMeter(id);
								id = id.substring(0, id.length()-4);
								
								// Rebind it and send to central server
								try {
									Naming.rebind(id, m);
									addToList(id);
								} catch (Exception e) {
									// Connection error
									System.err.println("Sorry an error has occured - please check your connection and the server's status");
								} 
								
								System.out.println("Reloading meter " + id + " from saved file.");
								
								validInput2 = true;
								validInput = true;
								
								m.runMeter();
							} else if( selection == meters.size() + 1) {
								System.out.println("Cancelling reloading a meter");
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
				} else if( input.equalsIgnoreCase("b") ) {
					createNewMeter();
					validInput = true;
				} else {
					System.out.println("Bad input - try again!");
				}
			}
			//s.close();
		} else {
			dp.createFolders();
			createNewMeter();
		}
	}
	
	/**
	 * Creates a new meter
	 */
	public static void createNewMeter() {
		Random r = new Random();
		
		// 'cos negative ids look ugly
		String id = Integer.toString(Math.abs(r.nextInt()));
		
		try {
			Meter m = new Meter(id);
			Naming.rebind(id, m);
			addToList(id);
			m.runMeter();
		} catch (Exception e) {
			// Connection error
			System.err.println("Sorry an error has occured - please check your connection and the server's status");
			e.printStackTrace();
		}
	}

	/**
	 * Adds a meter to the remote list
	 * 
	 * @param id String
	 * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	private static void addToList(String name) throws MalformedURLException, RemoteException, NotBoundException {
		System.out.println("Providing meter details to the central server");
		RemoteListInterface<String> meterListRLI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/meterList");
		meterListRLI.addObject(name);

	}
}
