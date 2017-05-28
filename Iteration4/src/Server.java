import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

//When program is run, server is called to create a Server Master;
public class Server{
	public static void main(String[] args){
		boolean verbose;
		boolean changePath;
		String 	path = "";
		try { path = new java.io.File( "." ).getCanonicalPath() + "\\server\\";
		} catch (IOException e) { e.printStackTrace(); }
		
	  	Scanner sc = new Scanner(System.in);
	  	while(true){
			System.out.println("Would you like to run it in verbose mode (Y/N)?");
			
			String input = sc.nextLine();
			if(input.toUpperCase().equals("Y")){ verbose=true; break;}
			if(input.toUpperCase().equals("N")){ verbose=false; break;}
			System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
		}
	  	
	  	while(true){
			System.out.println("Do you wish to change working directory (Y/N)?\nCurrently set to the following path: \n"+path);
			
			String input = sc.nextLine();
			if(input.toUpperCase().equals("Y")){ changePath=true;  break;}
			if(input.toUpperCase().equals("N")){ changePath=false; break;}
			System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
		 }
	  	if(changePath){
	  		while(true){
				System.out.println("Please enter a new directory for the server:");
				
				String input = sc.nextLine();
				input = input.trim();
				File f = new File(input);
				if(f.exists() && f.isDirectory()){ 
					path=input;  
					if(path.substring(path.length() - 1).equals("\\")) {}
					else path+="\\";
					System.out.println("Directory set to:\n"+path);
					break;
				}
				System.out.println("Invalid directory! Please try again...");
	  		}
	  	}
	  	
		ServerMaster SM = null;
		SM = new ServerMaster(verbose,path);
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
		if(running)
			workers.add(new WorkerHandler(port,address,request,workingDir,verbose));
		
	}
}

//This class wraps around the ServerWorker class so that MetaData can be retrieved
class WorkerHandler{
	public int port;
	public InetAddress address;
	private ServerWorker worker;
	private String workingDir;
	
	//Constructor for worker;
	public WorkerHandler(int Port, InetAddress clientAddress, Packet request, String dir, boolean verbose){
		port		= Port;
		address 	= clientAddress;
		workingDir 	= dir;
		worker 		= new ServerWorker(Port,clientAddress,request,verbose,workingDir);
		
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
	
}

//Worker thread that handles a client request;
class ServerWorker extends Thread{
	private int port;
	private InetAddress address;
	private Packet mainReq;
	private DatagramSocket soc;
	private helplib help;
	private String workingDir;
	
	//Constructor;
	public ServerWorker(int Port, InetAddress clientAddress, Packet request, boolean verbose, String dir){
		port 		= Port;
		address 	= clientAddress;
		mainReq 	= request;
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
		if(mainReq.GetFile().equals("")){
			Packet ack = new Packet(4,"No Filename");
			help.sendPacket(ack, soc, address, port);
			soc.close();
			return;
		}
		if(!mainReq.GetMode().equals("netascii")){
			Packet ack = new Packet(4,"Invalid file mode");
			help.sendPacket(ack, soc, address, port);
			soc.close();
			return;
		}
		if(mainReq.GetRequest()==1){
			//Read request;
			
			//Open file for reading;
			File file = new File(workingDir+mainReq.GetFile());
			BufferedInputStream FIn = null;
			try { 
				FIn = new BufferedInputStream(new FileInputStream(file));
			} catch (SecurityException e){
				help.print("FileIO::ERROR::Access Violation.");
				Packet ERR = new Packet(2,"Access Violation.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				return; 
			} catch (FileNotFoundException e) {
				if(file.exists()){
					help.print("FileIO::ERROR::Access Violation.");
					Packet ERR = new Packet(2,"Access Violation.");
					help.sendPacket(ERR, soc, address, port);
					soc.close();
					return; 
				}
				help.print("FileIO::ERROR::File not found. "+ workingDir+mainReq.GetFile());
				Packet ERR = new Packet(1,"File not found.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				return; 
			}
			//Done opening file.
			
			int numBlock = 0;
			int curBlock = 0;
			numBlock = (int)(file.length()/Packet.DATASIZE);
			help.print("File located, Initiating transfer of "+ numBlock + " blocks.");
			Packet ack = new Packet(numBlock);
			help.sendPacket(ack, soc, address, port);
			Packet rec;
			try {
				rec = recurreceive(soc,help.timeout,help.retries,null);
			} catch (IOException e1) {
				help.print("Connection timed out, tread quitting.");
				soc.close();
				try { FIn.close(); } catch (IOException e) { e.printStackTrace(); }
				return;
			}
			if(!help.isOkay(rec, 4)){ 
				Packet ERR = new Packet(4,"Invalid packet received.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				try { FIn.close(); } catch (IOException e) { e.printStackTrace(); }
				return; 
			}

			if(rec.GetRequest()!=4) System.exit(1);
			//File transfer loop;
			boolean valid = true;
			while(curBlock <= numBlock){
				ack = null;
				if(valid){
					byte[] bData = help.ReadData(FIn, curBlock, Packet.DATASIZE);
					ack = new Packet(curBlock,bData);
					help.sendPacket(ack, soc, address, port);
				}
				
				try {
					rec = recurreceive(soc,help.timeout,help.retries,ack);
				} catch (IOException e) {
					help.print("Connection timed out, tread quitting.");
					soc.close();
					try { FIn.close(); } catch (IOException e1) { e1.printStackTrace(); }
					return;
				}
				if(!help.isOkay(rec, 4)){ 
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, address, port);
					soc.close();
					return; 
				}
				
				if(rec.GetPacketN()==curBlock){
					curBlock++;
					valid = true;
				} else {
					help.print("Invalid acknowledgment recieved! Ignoring.");
					valid = false;
				}
			}
			ack = new Packet(curBlock);
			help.sendPacket(ack, soc, address, port);
			try { FIn.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		else{
			//Write Request
			long usableSpace = new File(workingDir).getUsableSpace();
			
			BufferedOutputStream FOut = null;
			File dir = new File(workingDir+mainReq.GetFile());
			
			if(!dir.exists() ){
				try { dir.createNewFile(); } 
				catch (IOException e) {  }
			}
			else{
				help.print("FileIO::ERROR::File Already Exists.");
				Packet ERR = new Packet(6,"File Already Exists.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				return; 
			}
			try { 
				FOut = new BufferedOutputStream(new FileOutputStream(dir,true));
			} catch (FileNotFoundException e) {
				//The thread should just quit if it somehow comes to this.
				e.printStackTrace(); return;
			}
			
			Packet ack = new Packet(0);
			help.sendPacket(ack, soc, address, port);
			Packet rec;
			try {
				rec = recurreceive(soc,help.timeout,help.retries,null);
			} catch (IOException e1) {
				help.print("Connection timed out, tread quitting.");
				soc.close();
				try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
				return;
			}
			if(!help.isOkay(rec, 4)){ 
				Packet ERR = new Packet(4,"Invalid packet received.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
				return; 
			}
			
			int numBlock = rec.GetPacketN();
			int curBlock = -1;
			
			//Check if file is larger than the free space;
			if(numBlock*Packet.DATASIZE>usableSpace){
				help.print("FileIO::ERROR::Disk full or allocation exceeded.");
				Packet ERR = new Packet(3,"Disk full or allocation exceeded.");
				help.sendPacket(ERR, soc, address, port);
				soc.close();
				try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
				return; 
			}
			
			
			ack = new Packet(0);
			help.sendPacket(ack, soc, address, port);
			while(curBlock < numBlock){
				try {
					rec = recurreceive(soc,help.timeout,help.retries,null);
				} catch (IOException e1) {
					help.print("Connection timed out, tread quitting.");
					soc.close();
					try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
					return;
				}
				if(!help.isOkay(rec, 3)){ 
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, address, port);
					soc.close();
					try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
					return; 
				}
				
				//Makes sure the packet is valid and then writes it to file.
				if(curBlock+1==rec.GetPacketN()){ 
					curBlock++; 
					help.WriteData(FOut, rec.GetData());
					//Create response with the current block received;
					ack = new Packet(rec.GetPacketN());
					help.sendPacket(ack, soc, address, port);
				}
				else{
					help.print("Invalid datablock recieved! Ignoring.");
				}

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
	
	private Packet recurreceive(DatagramSocket soc, int timeout, int retries, Packet resend) throws IOException{
		Packet rec = null;
		try {
			rec = help.recievePacket(soc, timeout);
		} catch (IOException e) {
			help.printd("Socket timed out, retrying...");
			if(resend!=null){
				help.printd("Resending last packet...");
				help.sendPacket(resend, soc, address, port);
			}
			if(retries>0) return recurreceive(soc,timeout,retries-1,resend);
			throw e;
		}
		if(!checkAddress(rec)){
			Packet ERR = new Packet(5,"Packet received from unknown sender.");
			help.sendPacket(ERR, soc,rec.GetAddress(),rec.GetPort());
			help.print("Listenning for the connection again...");
			return recurreceive(soc,timeout,retries,resend);
		}
		else return rec;
	}
	
	private boolean checkAddress(Packet P){
		if(P.GetPort()==port && P.GetAddress().equals(address)) return true;
		return false;
	}
}
