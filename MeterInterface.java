import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Remote interface for the Meter class
 */
public interface MeterInterface extends Remote {
	
	public boolean receiveCommand(int command, String source) throws RemoteException;
	public boolean receiveOffer(String name, String tariff) throws RemoteException;
	
	public String getId() throws RemoteException;	
	public ArrayList<Integer> getHistory(String brokerName) throws RemoteException;
	public int getLatestReading() throws RemoteException;	
	public String getPowerCompanyName() throws RemoteException;
	
	public boolean setPowerCompany(PowerCompanyInterface pc) throws RemoteException;

	public boolean hasPowerCompany() throws RemoteException;
	
	public boolean ping() throws RemoteException;
}
