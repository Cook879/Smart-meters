import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DataPersistence {
	// Folder directories
	public static final String ROOT_FOLDER = "save_data";
	public static final String POWER_COMPANY_FOLDER = ROOT_FOLDER + "/power_companies";
	public static final String BROKER_FOLDER = ROOT_FOLDER + "/brokers";
	public static final String METER_FOLDER = ROOT_FOLDER + "/meters";
	public static final String SERVER_FOLDER = ROOT_FOLDER + "/central";
	public static final String METER_LIST_FILE = SERVER_FOLDER + "/meters.list";
	public static final String POWER_COMPANY_LIST_FILE = SERVER_FOLDER + "/power_companies.list";
	public static final String BROKER_LIST_FILE = SERVER_FOLDER + "/brokers.list";
		
	// Objects for folders
	private File powerCompanies;
	private File brokers;
	private File meters;
	private File meterList;
	private File powerCompanyList;
	private File brokerList;
		
	/**
	 * Constructor - creates file objects
	 */
	public DataPersistence( ) {
		powerCompanies = new File( POWER_COMPANY_FOLDER );
		brokers = new File( BROKER_FOLDER );
		meters = new File( METER_FOLDER );
		meterList = new File( METER_LIST_FILE );
		powerCompanyList = new File( POWER_COMPANY_LIST_FILE );
		brokerList = new File( BROKER_LIST_FILE );
	}
	
	/**
	 * Checks if save data exists by checking the directories exist
	 * 
	 * @return boolean
	 */
	public boolean saveDataExists() {
		return (powerCompanies.exists() && brokers.exists() && meters.exists() 
				&& meterList.exists() && powerCompanyList.exists() && brokerList.exists() );
	}	
	
	public boolean meterSaveDataExists() {
		return meters.exists();
	}
	
	public boolean powerCompanySaveDataExists() {
		return powerCompanies.exists();
	}
	
	public boolean brokerSaveDataExists() {
		return brokers.exists();
	}
	
	/**
	 * Saves a PowerCompany object
	 * 
	 * @param pc PowerCompany
	 */
	public void savePowerCompany(PowerCompany pc) {
		try {
			File f = new File( POWER_COMPANY_FOLDER + "/" + pc.getName() + ".pc" );
			FileOutputStream fos = new FileOutputStream( f );
			ObjectOutputStream oos = new ObjectOutputStream( fos );
			
			oos.writeObject( pc );
			
			oos.flush();
			oos.close();
			fos.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves a Broker object
	 * 
	 * @param b Broker
	 */
	public void saveBroker( Broker b ) {
		try {
			File f = new File( BROKER_FOLDER + "/" + b.getName() + ".brk" );
			FileOutputStream fos = new FileOutputStream( f );
			ObjectOutputStream oos = new ObjectOutputStream( fos );
			
			oos.writeObject( b );
			
			oos.flush();
			oos.close();
			fos.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Saves a Meter object
	 * 
	 * @param m Meter
	 */
	public void saveMeter( Meter m ) {
		try {
			File f = new File( METER_FOLDER + "/" + m.getId() + ".met" );
			FileOutputStream fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeObject(m);
			
			oos.flush();
			oos.close();
			fos.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the save directories
	 */
	public void createFolders() {
		File root = new File(ROOT_FOLDER);
		root.mkdir();
		powerCompanies.mkdir();
		brokers.mkdir();
		meters.mkdir();
		File server = new File(SERVER_FOLDER);
		server.mkdir();
		/*try {
			meterList.createNewFile();
			powerCompanyList.createNewFile();
			brokerList.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	/**
	 * Gets list of saved Meter objects
	 * 
	 * @return List<String>
	 */
	public List<String> getMeterNames() {
		List<String> list = new ArrayList<String>();
		
		for( String s : meters.list() ) {
			if( s.toLowerCase().endsWith(".met") ) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Deserializes a specified Meter object
	 * 
	 * @param string String
	 * @return Meter
	 */
	public Meter getMeter(String string) {
		boolean opened = false;
		
		Meter m = null;
		while( !opened ) {
			File f = new File( METER_FOLDER + "/" + string );
			try {
				FileInputStream fis = new FileInputStream( f );
				ObjectInputStream ois = new ObjectInputStream( fis );
				m = (Meter) ois.readObject();
				
				fis.close();
				ois.close();
							
				opened = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m;
	}

	/**
	 * Gets list of saved PowerCompany objects
	 * 
	 * @return List<String>
	 */
	public List<String> getPowerCompanyNames() {
		List<String> list = new ArrayList<String>();
		
		for( String s : powerCompanies.list() ) {
			if( s.toLowerCase().endsWith(".pc") ) {
				list.add(s);
			}
		}
		return list;
	}

	/**
	 * Deserializes a specified PowerCompany object
	 * 
	 * @param string String
	 * @return PowerCompany
	 */
	public PowerCompany getPowerCompany(String name) {
		boolean opened = false;
		
		PowerCompany pc = null;
		while( !opened ) {
			File f = new File( POWER_COMPANY_FOLDER + "/" + name );
			try {
				FileInputStream fis = new FileInputStream( f );
				ObjectInputStream ois = new ObjectInputStream( fis );
				pc = (PowerCompany) ois.readObject();
				
				fis.close();
				ois.close();
							
				opened = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return pc;
	}

	/**
	 * Gets list of saved Broker objects
	 * 
	 * @return List<String>
	 */
	public List<String> getBrokerNames() {
		List<String> list = new ArrayList<String>();
		
		for( String s : brokers.list() ) {
			if( s.toLowerCase().endsWith(".brk") ) {
				list.add(s);
			}
		}
		return list;
	}
	
	/**
	 * Deserializes a specified Broker object
	 * 
	 * @param string String
	 * @return Broker
	 */
	public Broker getBroker(String name) {
		boolean opened = false;
		
		Broker b = null;
		while( !opened ) {
			File f = new File( BROKER_FOLDER + "/" + name );
			try {
				FileInputStream fis = new FileInputStream( f );
				ObjectInputStream ois = new ObjectInputStream( fis );
				b = (Broker) ois.readObject();
				
				fis.close();
				ois.close();
							
				opened = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return b;
	}
	
	/**
	 * Gets the specified RemoteList 
	 * 
	 * @return RemoteList<String>
	 */
	@SuppressWarnings("unchecked")
	public RemoteList<String> getRemoteList(int type) {
		boolean opened = false;
		
		RemoteList<String> list = null;
		while( !opened ) {
			try {
				FileInputStream fis;
				if( type == 0 )
					fis = new FileInputStream( meterList );
				else if (type == 1)
					fis = new FileInputStream( powerCompanyList );
				else
					fis = new FileInputStream( brokerList );
				ObjectInputStream ois = new ObjectInputStream( fis );
				list = (RemoteList<String>) ois.readObject();
				
				fis.close();
				ois.close();
							
				opened = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;			
	}
	public RemoteList<String> getMeterList() {
		return getRemoteList(0);
	}

	public RemoteList<String> getPowerCompanyList() {
		return getRemoteList(1);
	}
	
	public RemoteList<String> getBrokerList() {
		return getRemoteList(2);
	}
	
	/**
	 * Saves a RemoteList object
	 * 
	 * @param remoteList RemoteList
	 */
	@SuppressWarnings("rawtypes")
	public void saveRemoteList( RemoteList remoteList, int type ) {
		try {
			FileOutputStream fos;
			if( type == 0 )
				fos = new FileOutputStream( meterList );
			else if (type == 1)
				fos = new FileOutputStream( powerCompanyList );
			else
				fos = new FileOutputStream( brokerList );
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeObject(remoteList);
			
			oos.flush();
			oos.close();
			fos.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
}
