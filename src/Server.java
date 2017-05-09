/**
 * Client for a TFTP
 *
 *Iteration 1:
 * Server class starts a ServerMaster thread and waits for user
 *   to press OK, on a displayed popup, to end ServerMaster.
 * ServerMaster continually awaits a connection at port 69.
 * ServerMaster receives connection and passes it to a
 *   WorkerHandler thats working on any request with the Client's
 *   port and address, or creates a WorkerHandler if none exist.
 * Newly created WorkerHandler starts up a ServerWorker thread,
 *   works on handling the client's request.
 * Upon ServerMaster ending, waits for all workers to complete,
 *   before closing and refuses any further connections.
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.swing.*;

//When program is run, server is called to create a Server Master;
public class Server{
	public static void main(String[] args){
		ServerMaster SM = new ServerMaster(true);
		SM.start();
		JOptionPane.showMessageDialog(null, "Press 'OK' at any point to quit");
		SM.Stop();
	}
}

//ServerMaster, awaits for connection and passes on to WorkerHandler;
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
	//Tells the ServerMaster to quit;
	public void Stop(){
		running = false;
		help.print("Closing server, waiting for workers to complete");
	}
	//Main ServerMaster logic;
	public void run(){
		//Server will listen and timeout and loop back until it gets a request;
		while(running || !allDone()){
			byte[] rec = new byte[Packet.PACKETSIZE];
			rpkt = new DatagramPacket(rec,rec.length);
			try { 
				soc.setSoTimeout(500);
				soc.receive(rpkt);
				//If server times out, the following part is skipped;
				help.print("Got a connection, deligating to worker.");
				help.printd("The bytes recieved are:\n"+ Arrays.toString(rec));
				handlePacket(new Packet(rec),rpkt.getPort(),rpkt.getAddress());
			} catch (IOException e) 
			{ 
				soc.close();
				try{ soc = new DatagramSocket(69); } 
				catch(SocketException se){ help.print("Failed to create socket!"); System.exit(1); }
			}
			
		}
		soc.close();
		help.print("All workers have completed, exiting.");
		System.exit(1);
	}
	
	private boolean allDone(){
		for(int i=0;i<workers.size();i++)
			if(!workers.get(i).isDone()) return false;
		return true;
	}
	
	//Sends the packet to a worker thats active and has the same address, else create a new one;
	private void handlePacket(Packet request, int port, InetAddress address){
		//Locates appropriate Worker to send packet to;
		for(int i=0;i<workers.size();i++){
			if(!workers.get(i).isDone()){
				if(workers.get(i).address.equals(address) && workers.get(i).port == port){
					workers.get(i).passReq(request);
					return;
				}
			}
		}
		//If the program is still running, accepts a new request;
		if(running)
			workers.add(new WorkerHandler(port,address,request,verbose));
		
	}
}

//This class wraps around the ServerWorker class so that MetaData can be retrieved
class WorkerHandler{
	public int port;
	public InetAddress address;
	private ServerWorker worker;
	private BlockingQueue<Packet> bQueue;
	
	//Constructor for worker;
	public WorkerHandler(int Port, InetAddress clientAddress, Packet request, boolean verbose){
		bQueue 	= new ArrayBlockingQueue<Packet>(10);
		port	= Port;
		address = clientAddress;
		worker 	= new ServerWorker(Port,clientAddress,request,verbose,bQueue);
		worker.start();
	}
	
	//Wait for ServerWorker Thread to complete;
	public void Wait(){
		try { worker.join(); }
		catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	//Checks if ServerWorker Thread to complete;
	public boolean isDone(){
		if(worker.getState()==Thread.State.TERMINATED) return true;
		return false;
	}
	
	//Passes the request to the thread;
	public void passReq(Packet request){
		try { bQueue.put(request);
		} catch (InterruptedException e) { e.printStackTrace(); }
	}
}

//Worker thread that handles a client request;
class ServerWorker extends Thread{
	int port;
	InetAddress address;
	Packet mainReq;
	DatagramSocket soc;
	helplib help;
	private BlockingQueue<Packet> bQueue;
	
	//Constructor;
	public ServerWorker(int Port, InetAddress clientAddress, Packet request, boolean verbose, BlockingQueue<Packet> Queue){
		port 	= Port;
		address = clientAddress;
		mainReq = request;
		bQueue 	= Queue;
		help	= new helplib("ServerWorker@"+Port, verbose);
		help.print("Worker created to handle the request:\n"+request);
		try{ soc = new DatagramSocket(); } 
		catch(SocketException se){ help.print("Failed to create Socket."); System.exit(1); }
	}
	
	//Main ServerWorker logic;
	public void run(){
		if(mainReq.GetRequest()==1){
			//Read request;
			FileInputStream FIn = help.OpenIFile(mainReq.GetFile());
			if(FIn==null){ System.exit(1); }
			int numBlock = 0;
			int curBlock = 0;
			try { numBlock = (int)(FIn.getChannel().size()/Packet.DATASIZE);
			} catch (IOException e) { e.printStackTrace(); }
			help.print("File located, Initiating transfer of "+ numBlock + " blocks.");
			Packet ack = new Packet(numBlock);
			help.sendPacket(ack, soc, address, port);
			Packet rec = receivePacket();
			if(rec.GetRequest()!=4) System.exit(1);
			//File transfer loop;
			while(curBlock <= numBlock){
				byte[] bData = help.ReadData(FIn, curBlock, Packet.DATASIZE);
				ack = new Packet(curBlock,bData);
				help.sendPacket(ack, soc, address, port);
				rec = receivePacket();
				if(rec.GetRequest()==4){
					curBlock++;
				} else System.exit(1);
			}
			try { FIn.close(); } catch (IOException e) { e.printStackTrace(); }
			help.print("File transfer complete!");
		}
		else{
			//Write Request
			FileOutputStream FOut = help.OpenOFile(mainReq.GetFile(), true);
			Packet ack = new Packet(0);
			help.sendPacket(ack, soc, address, port);
			Packet rec = receivePacket();
			int numBlock = rec.GetPacketN();
			int curBlock = -1;
			ack = new Packet(0);
			help.sendPacket(ack, soc, address, port);
			while(curBlock < numBlock){
				rec = receivePacket();
				
				//Makes sure the packet is valid and then writes it to file.
				if(curBlock+1==rec.GetPacketN()){ 
					curBlock++; 
					help.WriteData(FOut, rec.GetData());
				}
				else{
					help.print("Invalid Packet Recieved! Closing.");
					System.exit(1);
				}
				//Create response with the current block received;
				ack = new Packet(rec.GetPacketN());
				help.sendPacket(ack, soc, address, port);
			}
			try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
			help.print("File transfer complete!");
		}
	}
	
	//Gets a packet from the parent WorkerHandler.
	private Packet receivePacket(){
		try {
			Packet p = bQueue.take();
			help.printd("Got the following packet:\n"+p);
			return p;
		} catch (InterruptedException e) { e.printStackTrace(); }
		return null;
	}
}


