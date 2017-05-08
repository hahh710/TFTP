import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
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
		workingDir		= dir;
		Port 			= port;
		serverAddress 	= addr;
		help.print("Initialized");
	}
	
	@SuppressWarnings("resource")
	public void begin(){
		int request;
		//Get the request from the user.
		while(true){
			help.print("What type of request would you like to make(RRQ/WRQ)?");
			
			String input = new Scanner(System.in).nextLine();
			if(input.toUpperCase().equals("RRQ")){ request=1; break;}
			if(input.toUpperCase().equals("WRQ")){ request=2; break;}
			help.print("Invalid Request! Select either 'RRQ'(Read) or 'WRQ' (Write)");
		}
		if(request==1){
			//Read Request;
			
			//Get the file to read from the server.
			help.print("Please enter the name of the file you wish to access:");
			String sendFile = new Scanner(System.in).nextLine();
			
			//Get the file to save data to.
			FileOutputStream FOut;
			while(true){
				help.print("Please enter the name of the file you want to save the data to:");
				String saveFile = new Scanner(System.in).nextLine();
				FOut = help.OpenOFile(workingDir + saveFile, true);
				if(FOut==null) help.print("File already exists! Please use another!");
				else		   break;
			}
			
			int numBlock = 0; //Total number of blocks;
			int curBlock = -1; //The current block the receive is on;
			
			//Attempt to handshake with the server to get the size of the file from the server;
			//Send the request.
			help.print("Attempting request...");
			
			Packet req = new Packet(1, sendFile, "netascii");
			Packet rec = help.sendReceive(req, soc, serverAddress, Port);
			if(rec.GetRequest()==4){ numBlock=rec.GetPacketN(); }
			else{System.exit(1);}
			
			help.print("Request Success, receiving "+numBlock+" blocks.");
			try{
				//Once Handshake is established, initiate file transfer.
				help.print("Initiating file transfer.");
				Packet ack = new Packet(numBlock);
				//Loop that transfers the file.
				while(curBlock < numBlock){
					rec = help.sendReceive(ack, soc, serverAddress, Port);
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
				help.sendPacket(ack, soc, serverAddress, Port);
			}
			catch (Exception e) { e.printStackTrace(); System.exit(1); }
			help.print("File transfer complete!");
			try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		else{
			//Write Request;
			help.print("Please enter the name of the file you want to write to on the server:");
			String saveFile = new Scanner(System.in).nextLine();
			String sendFile;
			//Get the file to save data to.
			FileInputStream FIn;
			while(true){
				help.print("Please enter the name of the file you wish to read from:");
				sendFile = new Scanner(System.in).nextLine();
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
			Packet req = new Packet(1, sendFile, "netascii");
			Packet rec = help.sendReceive(req, soc, serverAddress, Port);
			if(rec.GetRequest()==4){ }
			else{System.exit(1);}
			
			//Ready for file transfer;
			help.print("Request Success, sending "+numBlock+" blocks.");
			while(curBlock < numBlock){
				byte[] bData = help.ReadData(FIn, curBlock, Packet.DATASIZE);
				Packet ack = new Packet(curBlock,bData);
				rec = help.sendReceive(ack, soc, serverAddress, Port);
				if(rec.GetRequest()==4){
					curBlock++;
				} else System.exit(1);
			}
			help.print("File transfer complete!");
		}
	}
	public static void main(String[] args){
		try {
			Client c = new Client(69, InetAddress.getLocalHost(),new java.io.File( "." ).getCanonicalPath() + "\\",true);
			c.begin();
		} catch (Exception e) { e.printStackTrace(); }
		
	}
}
