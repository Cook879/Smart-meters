import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for Power Company objects
 */
public interface PowerCompanyInterface extends Remote {
	public boolean addCustomer(String id, MeterInterface m, int latestReading) throws RemoteException;
	public boolean removeCustomer(String id) throws RemoteException;
	
	public int receiveReading(String id, int newReading) throws RemoteException;
	public boolean receiveAlert(String m, int id) throws RemoteException;
		
	public String getName() throws RemoteException;
	public Tariff getTariffDetails() throws RemoteException;
	
	public boolean ping() throws RemoteException;
}
