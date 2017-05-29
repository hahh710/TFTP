import java.io.*;
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
		@SuppressWarnings("resource")
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
			BufferedOutputStream FOut;
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
			help.print("Attempting request...");

			Packet req = new Packet(1, sendFile, "netascii");
			Packet rec = help.sendReceive(req, soc, serverAddress, Port);
			serverAddress = rec.GetAddress();
			Port = rec.GetPort();

			if(!help.isOkay(rec, 4)){
				if(rec.GetRequest()!=5)
				{
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, serverAddress, Port);
				}
				return;  
			}
			numBlock=rec.GetPacketN();

			System.out.println();
			help.print("Request Success, receiving "+numBlock+" blocks.");
			try{
				//Once Handshake is established, initiate file transfer.
				help.print("Initiating file transfer.");
				Packet ack = new Packet(numBlock);
				boolean valid = true;
				//Loop that t the file.
				while(curBlock < numBlock){
					if(valid) help.sendPacket(ack, soc, serverAddress, Port);
					try { rec = recurreceive(soc, help.timeout, help.retries, ack);
					} catch (IOException e) { help.print("No response, ending session."); return; }

					if(!help.isOkay(rec, 3)){ 		
						if(rec.GetRequest()!=5)
						{
							Packet ERR = new Packet(4,"Invalid packet received.");
							help.sendPacket(ERR, soc, serverAddress, Port);
						}
						return;  
					}
					//Makes sure the packet is valid and then writes it to file.
					if(curBlock+1==rec.GetPacketN()){ 
						curBlock++; 
						help.WriteData(FOut, rec.GetData());
						ack = new Packet(rec.GetPacketN());
						valid = true;
					}
					else{
						help.print("Invalid datablock recieved! Ignoring.");
						valid = false;
					}
					//Create response with the current block received;

				}
				ack = new Packet(numBlock);
				ack = help.sendReceive(ack, soc, serverAddress, Port);
			}
			catch (Exception e) { e.printStackTrace(); System.exit(1); }
			try { FOut.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		else{
			//Write Request;
			help.print("Please enter the save location on the server:");
			String saveFile = sc2.nextLine();
			String sendFile = "";
			File file = null;
			//Get the file to save data to.
			BufferedInputStream FIn;
			
			while(true){
				help.print("Please enter the name of the file you wish to read from:");
				sendFile = sc2.nextLine();
				FIn = help.OpenIFile(workingDir + sendFile);
				file = new File(workingDir + sendFile);
				if     (FIn==null) 			  help.print("File doesnt exist! Please use another!");
				else if(file.length()>130000) help.print("File too big to transfer, Please use another!");
				else		   	break;
			}
			file = new File(workingDir + sendFile);
			help.print("Attempting request...");
			//Getting the size of the file;
			int numBlock = 0;
			int curBlock = 0;
			numBlock = (int)(file.length()/Packet.DATASIZE);

			//Send the request;
			Packet req = new Packet(2, saveFile, "netascii");
			Packet rec = help.sendReceive(req, soc, serverAddress, Port);
			serverAddress = rec.GetAddress();
			Port = rec.GetPort();
			if(!help.isOkay(rec, 4)){
				if(rec.GetRequest()!=5)
				{
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, serverAddress, Port);
				}
				return; 
			}

			help.print("Request Success, sending "+numBlock+" blocks.");
			//Ready for file transfer;
			req = new Packet(numBlock);
			help.sendPacket(req, soc, serverAddress, Port);
			try { rec = recurreceive(soc, help.timeout, help.retries, null);
			} catch (IOException e) { help.print("No response, ending session."); return; }

			if(!help.isOkay(rec, 4)){ 
				if(rec.GetRequest()!=5)
				{
					Packet ERR = new Packet(4,"Invalid packet received.");
					help.sendPacket(ERR, soc, serverAddress, Port);
				}
				return; 
			}
			boolean valid = true;
			while(curBlock <= numBlock){
				Packet ack = null; 
				if(valid){
					byte[] bData = help.ReadData(FIn, curBlock, Packet.DATASIZE);
					ack = new Packet(curBlock,bData);
					help.sendPacket(ack, soc, serverAddress, Port);
				}
				try { rec = recurreceive(soc, help.timeout, help.retries, ack);
				} catch (IOException e) { help.print("No response, ending session."); return; }

				if(!help.isOkay(rec, 4)){
					if(rec.GetRequest()!=5)
					{
						Packet ERR = new Packet(4,"Invalid packet received.");
						help.sendPacket(ERR, soc, serverAddress, Port);
					}
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

		}
		help.print("File transfer complete!\n--------------------------------------------------------------------------------\n\n");

	}

	private Packet recurreceive(DatagramSocket soc, int timeout, int retries, Packet resend) throws IOException{
		Packet rec = null;
		try {
			rec = help.recievePacket(soc, timeout);
		} catch (IOException e) {
			help.printd("Socket timed out, retrying...");
			if(resend!=null){
				help.printd("Resending last packet...");
				help.sendPacket(resend, soc, serverAddress, Port);
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
		if(P.GetPort()==Port && P.GetAddress().equals(serverAddress)) return true;
		return false;
	}

	public static void main(String[] args){
		try {
			
			System.out.println("\n\n\n-=:CLIENT:=-\n\n\n");
			
			boolean verbose;
			int     port;
			boolean running = true;
			Scanner sc = new Scanner(System.in);
			while(true){
				System.out.println("Would you like to run it in verbose mode (Y/N)?");

				String input = sc.nextLine();
				if(input.toUpperCase().equals("Y")){ verbose=true; break;}
				if(input.toUpperCase().equals("N")){ verbose=false; break;}
				System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
			}
			while(running){
				while(true){
					System.out.println("Will the client be run along side an error simulator (Y/N)?");
					System.out.println("'N' Will connect directly to the server at port 69.");
					String input = sc.nextLine();
					if(input.toUpperCase().equals("Y")){ port=23; break;}
					if(input.toUpperCase().equals("N")){ port=69; break;}
					System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
				}
				Client c = new Client(port, InetAddress.getLocalHost(),new java.io.File( "." ).getCanonicalPath() + "\\client\\",verbose);
				c.begin();
				while(true){
					System.out.println("Would you like to run again (Y/N)?");

					String input = sc.nextLine();
					if(input.toUpperCase().equals("Y")){ running=true; break;}
					if(input.toUpperCase().equals("N")){ running=false; break;}
					System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
				}
				System.out.println();
			}
			sc.close();
		} catch (Exception e) { e.printStackTrace(); }

	}
}
