import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {
	
	
}

class ServerWorker extends Thread{
	int port;
	InetAddress address;
	Packet mainReq;
	private BlockingQueue<Packet> bQueue;
	public ServerWorker(int Port, InetAddress clientAddress, Packet request, BlockingQueue<Packet> Queue){
		port = Port;
		address = clientAddress;
		mainReq = request;
		bQueue = Queue;
	}
}

//Object wraps around the worker thread so that we can access the port and address;
class WorkerHandler{
	public int port;
	public InetAddress address;
	private ServerWorker worker;
	private BlockingQueue<Packet> bQueue;
	public WorkerHandler(int Port, InetAddress clientAddress, Packet request){
		bQueue = new ArrayBlockingQueue<Packet>(10);
		worker = new ServerWorker(Port, clientAddress, request, bQueue);
		port = Port;
		address = clientAddress;
	}
	
	public void start(){
		worker.start();
	}
	
	//Passes the request to the thread
	public void passReq(Packet request){
        try { bQueue.put(request);
        } catch (InterruptedException e) { e.printStackTrace(); }
	}
}