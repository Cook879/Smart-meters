import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the Broker class
 */
public interface BrokerInterface extends Remote {
	public boolean receiveRequest(String id, MeterInterface m) throws RemoteException;
	
	public String getName() throws RemoteException;

	public boolean ping() throws RemoteException;
}
