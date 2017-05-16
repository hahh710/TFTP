/**
 * Server for a TFTP
 *
 *Iteration 2:
 * Server outputs cleaned up and now readable.
 * Server now checks for errors and sends response.
 * Server updated to use Steady File Transfer protocol.
 * Removed use of a message box to close the system,
 *   replaced with scanner instead.
 * Output locations now in separate folders.
 * 
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

//When program is run, server is called to create a Server Master;
public class Server{
	public static void main(String[] args){
		boolean verbose;
	  	Scanner sc = new Scanner(System.in);
	  	while(true){
			System.out.println("Would you like to run it in verbose mode (Y/N)?");
			
			String input = sc.nextLine();
			if(input.toUpperCase().equals("Y")){ verbose=true; break;}
			if(input.toUpperCase().equals("N")){ verbose=false; break;}
			System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
		 }
	  	
		ServerMaster SM = null;
		try { SM = new ServerMaster(verbose,new java.io.File( "." ).getCanonicalPath() + "\\server\\");
		} catch (IOException e) { e.printStackTrace(); }
		SM.start();
		
		while(true){
			System.out.println("\nEnter 'Q' followed by 'Enter' to quit at any time.\n");
			String input = sc.nextLine();
			if(input.toUpperCase().equals("Q")){ break;}
		}
		//JOptionPane.showMessageDialog(null, "Press 'OK' at any point to quit");
		SM.Stop();
		sc.close();
	}
}

//ServerMaster, awaits for connection and passes on to WorkerHandler;
class ServerMaster extends Thread{
	private DatagramPacket  			rpkt;
	private DatagramSocket 				soc;
	private helplib 					help;
	private boolean						verbose;
	private boolean						running;
	private ArrayList<WorkerHandler>	workers;
	private String 						workingDir;
	
	public ServerMaster(boolean Verbose, String dir){
		//Creates a DatagramSocket
		help = new helplib("Server", Verbose);
		workers = new ArrayList<WorkerHandler>();
		try{ soc = new DatagramSocket(69); } 
		catch(SocketException se){ help.print("Failed to create socket!"); System.exit(1); }
		System.out.println();
		help.print("Initialized");
		running 	= true;
		verbose 	= Verbose;
		workingDir 	= dir;
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
				System.out.println();
				help.printd("The bytes recieved are:\n"+ help.byteToString(rec));
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
			workers.add(new WorkerHandler(port,address,request,workingDir,verbose));
		
	}
}

//This class wraps around the ServerWorker class so that MetaData can be retrieved
class WorkerHandler{
	public int port;
	public InetAddress address;
	private ServerWorker worker;
	private BlockingQueue<Packet> bQueue;
	private String workingDir;
	
	//Constructor for worker;
	public WorkerHandler(int Port, InetAddress clientAddress, Packet request, String dir, boolean verbose){
		bQueue 		= new ArrayBlockingQueue<Packet>(10);
		port		= Port;
		address 	= clientAddress;
		workingDir 	= dir;
		worker 		= new ServerWorker(Port,clientAddress,request,verbose,workingDir,bQueue);
		
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
	private int port;
	private InetAddress address;
	private Packet mainReq;
	private DatagramSocket soc;
	private helplib help;
	private String workingDir;
	private BlockingQueue<Packet> bQueue;
	
	//Constructor;
	public ServerWorker(int Port, InetAddress clientAddress, Packet request, boolean verbose, String dir, BlockingQueue<Packet> Queue){
		port 		= Port;
		address 	= clientAddress;
		mainReq 	= request;
		bQueue 		= Queue;
		workingDir 	= dir;
		help		= new helplib("ServerWorker@"+Port, verbose);
		help.print("\n--------------------------------------------------------------------------------\nWorker created to handle the request:\n"+request+"\n");
		help.print("\nFrom the location:\nPort:    "+port+"\nAddress: "+ address + "\n");
		try{ soc = new DatagramSocket(); } 
		catch(SocketException se){ help.print("Failed to create Socket."); System.exit(1); }
	}
	
	//Main ServerWorker logic;
	public void run(){
		System.out.println();
		if(!mainReq.GetMode().equals("netascii")){
			Packet ack = new Packet(4,"Invalid file mode");
			help.sendPacket(ack, soc, address, port);
			soc.close();
			return;
		}
		if(mainReq.GetRequest()==1){
			//Read request;
			FileInputStream FIn = help.OpenIFile(workingDir+mainReq.GetFile());
			if(FIn==null){ System.exit(1); }
			int numBlock = 0;
			int curBlock = 0;
			try { numBlock = (int)(FIn.getChannel().size()/Packet.DATASIZE);
			} catch (IOException e) { e.printStackTrace(); }
			help.print("File located, Initiating transfer of "+ numBlock + " blocks.");
			Packet ack = new Packet(numBlock);
			help.sendPacket(ack, soc, address, port);
			Packet rec = recurreceive(soc);
			if(!help.isOkay(rec, 4)){ 
				Packet ERR = new Packet(4,"Invalid packet received.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				return; 
			}

			if(rec.GetRequest()!=4) System.exit(1);
			//File transfer loop;
			while(curBlock <= numBlock){
				byte[] bData = help.ReadData(FIn, curBlock, Packet.DATASIZE);
				ack = new Packet(curBlock,bData);
				help.sendPacket(ack, soc, address, port);
				rec = recurreceive(soc);
				if(!help.isOkay(rec, 4)){ 
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, address, port);
					soc.close();
					return; 
				}
				
				if(rec.GetRequest()==4){
					curBlock++;
				} else System.exit(1);
			}
			ack = new Packet(curBlock);
			help.sendPacket(ack, soc, address, port);
			try { FIn.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		else{
			//Write Request
			FileOutputStream FOut = help.OpenOFile(workingDir+mainReq.GetFile(), true);
			Packet ack = new Packet(0);
			help.sendPacket(ack, soc, address, port);
			Packet rec = recurreceive(soc);
			if(!help.isOkay(rec, 4)){ 
				Packet ERR = new Packet(4,"Invalid packet received.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				return; 
			}
			
			int numBlock = rec.GetPacketN();
			int curBlock = -1;
			ack = new Packet(0);
			help.sendPacket(ack, soc, address, port);
			while(curBlock < numBlock){
				rec = recurreceive(soc);
				if(!help.isOkay(rec, 3)){ 
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, address, port);
					soc.close();
					return; 
				}
				
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
		}
		help.print("File transfer complete! Worker thread closing.\n--------------------------------------------------------------------------------\n\n");
	}
	
	/*//Gets a packet from the parent WorkerHandler.
	private Packet receivePacket(){
		try {
			Packet p = bQueue.take();
			help.printd("Got the following packet:\n"+p);
			return p;
		} catch (InterruptedException e) { e.printStackTrace(); }
		return null;
	}*/
	
	private Packet recurreceive(DatagramSocket soc){
		Packet rec = help.recievePacket(soc);
		if(!checkAddress(rec)){
			Packet ERR = new Packet(5,"Packet received from unknown sender.");
			help.sendPacket(ERR, soc,rec.GetAddress(),rec.GetPort());
			help.print("Listenning for the connection again...");
			return recurreceive(soc);
		}
		else return rec;
	}
	
	private boolean checkAddress(Packet P){
		if(P.GetPort()==port && P.GetAddress().equals(address)) return true;
		return false;
	}
}