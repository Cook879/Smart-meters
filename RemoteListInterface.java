import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Interface for remote ArrayLists
 */
public interface RemoteListInterface<T> extends Remote {
	public ArrayList<T> accessList() throws RemoteException;
	public boolean addObject(T obj) throws RemoteException;
}
