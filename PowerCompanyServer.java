import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

/**
 * Server to create and bind a power company object
 */
public class PowerCompanyServer {
	public static void main(String[] args) {
		DataPersistence dp = new DataPersistence();
		
		// If data exists
		if( dp.saveDataExists() ) {
			System.out.println("Load a power company (a) or create a new one (b)");
			Scanner s = new Scanner(System.in);

			boolean validInput = false;
			
			while(!validInput) {
				String input = s.nextLine();
				
				if( input.equalsIgnoreCase("a") ) {
					
					// Get a list of saved power companies
					List<String> powerCompanies = dp.getPowerCompanyNames();
					
					int i = 1;
					for( String str : powerCompanies ) {
						System.out.println(i + " : " + str);
						i++;
					}
					System.out.println(i + ": Cancel");
					
					System.out.println("Select the number of your power company");
					
					// Process selection
					boolean validInput2 = false;
					while(!validInput2) {							
						// Check input
						try {
							int selection = Integer.parseInt(s.nextLine());
							
							if( selection > 0 && selection <= powerCompanies.size()) {
								String name = powerCompanies.get(selection-1);
								
								// Reload the object
								PowerCompany pc = dp.getPowerCompany(name);
								
								// Rebind it and send to central server
								try {
									name = name.substring(0, name.length()-3);
									Naming.rebind(name, pc);
									addToList(name);
								} catch (Exception e) {
									// Connection error
									System.err.println("Sorry an error has occured - please check your connection and the server's status");
								} 
								System.out.println("Reloading power company " + name + " from saved file.");
								validInput2 = true;
							} else if( selection == powerCompanies.size()+1 ) {
								System.out.println("Cancelling reloading a power company.");
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
					createNewPowerCompany();
					validInput = true;
				} else {
					System.out.println("Please enter a or b.");
				}
				//s.close();
			}
		} else {
			dp.createFolders();
			createNewPowerCompany();			
		}
	}
	
	public static void createNewPowerCompany() {
		System.out.println("Enter the name of the power company");
		
		Scanner s = new Scanner(System.in);
		String name = s.nextLine().replace(" ", "_");
		//s.close();
		
		try {
			Naming.rebind( name, new PowerCompany(name));
			addToList(name);
		} catch (Exception e) {
			// Connection error
			System.err.println("Sorry an error has occured - please check your connection and the server's status");
		}
	}

	private static void addToList(String name) throws MalformedURLException, RemoteException, NotBoundException {
		System.out.println("Providing power company details to the central server");
		RemoteListInterface<String> powerCompanyListRLI = (RemoteListInterface<String>) Naming.lookup("rmi://localhost/companyList");
		powerCompanyListRLI.addObject(name);		
	}
}
