/**
 * Client for a TFTP
 *
 *Iteration 1:
 * Client asks user to make R/W request.
 * Request sent to server at port 23 and receives response.
 * Performs file operation.
 * Little to no error checking done for now.
 *      
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client{
	private DatagramSocket 	soc;
	private helplib 		help;
	private String 			workingDir;
	private InetAddress		serverAddress;
	private int				Port;
	
	public Client(int port, InetAddress addr, String dir, boolean verbose){
		help = new helplib("Client", verbose);
		try{ soc = new DatagramSocket(); } 
		catch(SocketException se){ help.print("Failed to create Socket."); System.exit(1); }
		workingDir	= dir;
		Port 		= port;
		serverAddress 	= addr;
		help.print("Initialized");
	}
	
	public void begin(){
		int request;
		//Get the request from the user.
		Scanner sc2 = new Scanner(System.in);
		while(true){
			help.print("What type of request would you like to make(RRQ/WRQ)?");
			
			String input = sc2.nextLine();
			if(input.toUpperCase().equals("RRQ")){ request=1; break;}
			if(input.toUpperCase().equals("WRQ")){ request=2; break;}
			help.print("Invalid Request! Select either 'RRQ'(Read) or 'WRQ' (Write)");
		}
		
		if(request==1){
			//Read Request;
			
			//Get the file to read from the server.
			help.print("Please enter the name of the file you wish to access:");
			String sendFile = sc2.nextLine();
			
			//Get the file to save data to.
			FileOutputStream FOut;
			while(true){
				help.print("Please enter the name of the file you want to save the data to:");
				String saveFile = sc2.nextLine();
				FOut = help.OpenOFile(workingDir + saveFile, true);
				if(FOut==null) help.print("File already exists! Please use another!");
				else		   break;
			}
			
			int numBlock = 0; //Total number of blocks;
			int curBlock = -1; //The current block the receive is on;
			
			//Attempt to handshake with the server to get the size of the file from the server;
			//Send the request.
			System.out.println();
			help.print("Attempting request...");
			
			Packet req = new Packet(1, sendFile, "netascii");
			Packet rec = help.sendReceive(req, soc, serverAddress, Port);
			serverAddress = rec.GetAddress();
			Port = rec.GetPort();
			
			if(!help.isOkay(rec, 4)){ return; }
			numBlock=rec.GetPacketN();
			
			System.out.println();
			help.print("Request Success, receiving "+numBlock+" blocks.");
			try{
				//Once Handshake is established, initiate file transfer.
				help.print("Initiating file transfer.");
				Packet ack = new Packet(numBlock);
				//Loop that transfers the file.
				while(curBlock < numBlock){
					rec = help.sendReceive(ack, soc, serverAddress, Port);
					if(!help.isOkay(rec, 3)){ return; }
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
				}
				ack = new Packet(numBlock);
				ack = help.sendReceive(ack, soc, serverAddress, Port);
			}
			catch (Exception e) { e.printStackTrace(); System.exit(1); }
			System.out.println();
			help.print("File transfer complete!");
			try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		else{
			//Write Request;
			help.print("Please enter the save location on the server:");
			String saveFile = sc2.nextLine();
			String sendFile;
			//Get the file to save data to.
			FileInputStream FIn;
			while(true){
				help.print("Please enter the name of the file you wish to read from:");
				sendFile = sc2.nextLine();
				FIn = help.OpenIFile(workingDir + sendFile);
				if(FIn==null) 	help.print("File doesnt exist! Please use another!");
				else		   	break;
			}
			help.print("Attempting request...");
			//Getting the size of the file;
			int numBlock = 0;
			int curBlock = 0;
			try { numBlock = (int)(FIn.getChannel().size()/Packet.DATASIZE);
			} catch (IOException e) { e.printStackTrace(); }
			
			//Send the request;
			Packet req = new Packet(2, saveFile, "netascii");
			Packet rec = help.sendReceive(req, soc, serverAddress, Port);
			serverAddress = rec.GetAddress();
			Port = rec.GetPort();
			if(!help.isOkay(rec, 4)){ return; }

			help.print("Request Success, sending "+numBlock+" blocks.");
			//Ready for file transfer;
			req = new Packet(numBlock);
			rec = help.sendReceive(req, soc, serverAddress, Port);
			if(!help.isOkay(rec, 4)){ return; }
			
			while(curBlock <= numBlock){
				byte[] bData = help.ReadData(FIn, curBlock, Packet.DATASIZE);
				Packet ack = new Packet(curBlock,bData);
				rec = help.sendReceive(ack, soc, serverAddress, Port);
				if(!help.isOkay(rec, 4)){ return; }
				curBlock++;
			}
			
			help.print("File transfer complete!");
		}
		sc2.close();
	}
	
	public static void main(String[] args){
		try {
			boolean verbose;
			Scanner sc = new Scanner(System.in);
			while(true){
				System.out.println("Would you like to run it in verbose mode (Y/N)?");
				
				String input = sc.nextLine();
				if(input.toUpperCase().equals("Y")){ verbose=true; break;}
				if(input.toUpperCase().equals("N")){ verbose=false; break;}
				System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
			}
			Client c = new Client(23, InetAddress.getLocalHost(),new java.io.File( "." ).getCanonicalPath() + "\\",verbose);
			c.begin();
			sc.close();
		} catch (Exception e) { e.printStackTrace(); }
		
	}
}
