import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.swing.*;

public class Server{
	@SuppressWarnings("resource")
	public static void main(String[] args){
		ServerMaster SM = new ServerMaster(true);
		SM.start();
		JOptionPane.showMessageDialog(null, "Press 'OK' at any point to quit");
		SM.Stop();
	}
}

class ServerMaster extends Thread{
	private DatagramPacket  		rpkt;
	private DatagramSocket 			soc;
	private helplib 				help;
	private boolean					verbose;
	private boolean					running;
	private ArrayList<WorkerHandler> workers;
	
	public ServerMaster(boolean Verbose){
		//Creates a DatagramSocket
		help = new helplib("Server", true);
		workers = new ArrayList<WorkerHandler>();
		try{ soc = new DatagramSocket(69); } 
		catch(SocketException se){ help.print("Failed to create socket!"); System.exit(1); }
		help.print("Initialized");
		running = true;
		verbose = Verbose;
	}
	public void Stop(){
		running = false;
	}
	public void run(){
		//Server will listen and timeout and loop back until it gets a request;
		while(running){
			byte[] rec = new byte[Packet.PACKETSIZE];
			rpkt = new DatagramPacket(rec,rec.length);
			try { 
				soc.setSoTimeout(500);
				soc.receive(rpkt);
				handlePacket(new Packet(rec),rpkt.getPort(),rpkt.getAddress());
			} catch (IOException e) 
			{ 
				soc.close();
				try{ soc = new DatagramSocket(69); } 
				catch(SocketException se){ help.print("Failed to create socket!"); System.exit(1); }
			}
			
		}
		soc.close();
		help.print("Closing server, waiting for workers to complete");
		for(int i=0;i<workers.size();i++){
			if(!workers.get(i).isDone()) workers.get(i).Wait();
		}
		help.print("All workers have completed, exiting.");
		System.exit(1);
	}
	
	//Sends the packet to a worker thats active and has the same address, else create a new one;
	private void handlePacket(Packet request, int port, InetAddress address){
		for(int i=0;i<workers.size();i++){
			if(!workers.get(i).isDone()){
				if(workers.get(i).address.equals(address) && workers.get(i).port == port){
					workers.get(i).passReq(request);
					return;
				}
			}
		}
		workers.add(new WorkerHandler(port,address,request,verbose));
		
	}
}

class WorkerHandler{
	public int port;
	public InetAddress address;
	private ServerWorker worker;
	private BlockingQueue<Packet> bQueue;
	public WorkerHandler(int Port, InetAddress clientAddress, Packet request, boolean verbose){
		bQueue 	= new ArrayBlockingQueue<Packet>(10);
		port	= Port;
		address = clientAddress;
		worker 	= new ServerWorker(Port,clientAddress,request,verbose,bQueue);
		worker.start();
	}
	
	public void Wait(){
		try { worker.join(); }
		catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	public boolean isDone(){
		if(worker.getState()==Thread.State.TERMINATED) return true;
		return false;
	}
	
	//Passes the request to the thread
	public void passReq(Packet request){
        try { bQueue.put(request);
        } catch (InterruptedException e) { e.printStackTrace(); }
	}
}

class ServerWorker extends Thread{
	int port;
	InetAddress address;
	Packet mainReq;
	DatagramSocket soc;
	helplib help;
	private BlockingQueue<Packet> bQueue;
	public ServerWorker(int Port, InetAddress clientAddress, Packet request, boolean verbose, BlockingQueue<Packet> Queue){
		port 	= Port;
		address = clientAddress;
		mainReq = request;
		bQueue 	= Queue;
		help	= new helplib("ServerWorker@"+Port, verbose);
	}
	
	public void run(){
		//READ/WRITE GOES HERE;
	}
	
}

//Object wraps around the worker thread so that we can access the port and address;
