import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Exposes an ArrayList remotely 
 */
public class RemoteList<T> extends UnicastRemoteObject implements RemoteListInterface<T>{
	
	private static final long serialVersionUID = -1674806174633078218L;
	private ArrayList<T> list;
	private int type;
	/**
	 * Constructor 
	 * 
	 * @param list String[]
	 * @throws RemoteException
	 */
	protected RemoteList(ArrayList<T> list, int type) throws RemoteException {
		super();
		this.list = list;
		this.type = type;
		DataPersistence dp = new DataPersistence();
		dp.saveRemoteList(this, type);
	}
	
	/**
	 * Constructor 
	 * 
	 * @throws RemoteException
	 */
	protected RemoteList(int type) throws RemoteException {
		super();
		this.list = new ArrayList<T>();
		this.type = type;
		DataPersistence dp = new DataPersistence();
		dp.saveRemoteList(this, type);
	}
	
	/**
	 * Access the list
	 * 
	 * @Override
	 * @return Object
	 * @throws RemoteException
	 */
	public ArrayList<T> accessList() throws RemoteException {
		return list;
	}

	/**
	 * Allows us to add an object to the remote list
	 * 
	 * @Override
	 * @param T obj
	 * @boolean 
	 */
	public boolean addObject(T obj) throws RemoteException {
		if( !list.contains(obj) )
			list.add(obj);		
	
		DataPersistence dp = new DataPersistence();
		dp.saveRemoteList(this, type);
		
		return true;
	}
}
