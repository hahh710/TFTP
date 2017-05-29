

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class ErrorSimulator {
	private helplib 		  		help;
	private Packet 		  		Packet;
	private DatagramSocket		receiveSocket, sendSocket, sendReceiveSocket, changeSocket;		
	private static Scanner 		sc;
	private InetAddress 			address;
	private int 					clientPort, serverPort, userInput, Req, packetNumber, clientReq = -1, pType;
	private boolean				transferringFile;

	public ErrorSimulator(boolean verbose){
		transferringFile = false;

		try{
			address = InetAddress.getLocalHost();
		} catch(UnknownHostException e){
			e.printStackTrace();
		}

		help = new helplib("ErrorSim",verbose);

		try {
			//Receive socket bind to port 23
			receiveSocket = new DatagramSocket(23);

			// socket for send and receive bind to an available port
			// used to communicate with the server
			sendReceiveSocket = new DatagramSocket();

			// socket to send bind to any available port
			// used to communicate with the client
			sendSocket = new DatagramSocket();

		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void passOnTFTP(){
		//Infinite for loop so it comes back here to wait for Client
		for(;;){
			byte[] data = new byte[Packet.PACKETSIZE];

			// Receive Packet from Client
			if(!isTransferringFile()){
				Packet = help.recievePacket(receiveSocket);
			}else{
				try{
					Packet = help.recievePacket(sendSocket,500);
				} catch (IOException e) {
					help.print("No response from client, assuming client completed.");
					transferringFile = false;
					sendSocket.close();
					receiveSocket.close();
					sendReceiveSocket.close();
					return;
				}
			}

			// Extracting the Packet Received from the Client
			clientPort = Packet.GetPort();
			if(clientReq == -1){
				clientReq = Packet.GetRequest();
			}
			help.print("Packet Received From Port: "+ clientPort);
			help.print("Packet Received From Address: " + Packet.GetAddress() + "\n");

			// Filename and Mode
			help.print("Filename : " + Packet.GetFile() + "  |   Mode : " + Packet.GetMode());

			//Length of the packet received
			data = Packet.GetData();
			int len = data.length;
			help.print("Length of Received Packet: " + len + "\n");
			help.print("Packet Received in Bytes: \n" + help.byteToString(data));

			// Form a String from the byte array, and print the string.
			String received = new String(data,0,len);
			help.print("Packet Received in String: "+ received  + "\n");

			// server sees them as being sent by the client
			if(!isTransferringFile() && userInput < 7 && userInput > -1){
				putError(Packet, userInput, sendReceiveSocket, 69);
			}else if(Packet.GetRequest() == 3 && userInput == 7){ // DATA packet
				putError(Packet, userInput, sendReceiveSocket, serverPort);
			}else if(isTransferringFile() && userInput == 8 && Packet.GetPacketN() == 1){
				Packet p = Packet;
				putError(Packet, userInput, sendReceiveSocket, serverPort);
				help.sendPacket(p, sendReceiveSocket, address, serverPort);
			}else if (!isTransferringFile()){
				help.sendPacket(Packet, sendReceiveSocket, address, 69);
			}else if(isTransferringFile() && userInput >= 9 && userInput <= 11){
				putError(Packet, userInput, sendReceiveSocket, serverPort);
			}else{
				help.sendPacket(Packet, sendReceiveSocket, address, serverPort);
			}

			// Block until a datagram packet is received from sendReceiveSocket
			Packet = help.recievePacket(sendReceiveSocket);
			serverPort = Packet.GetPort();

			// Extract information received from server
			help.print("Packet Received From Port: "+ serverPort);
			help.print("Packet Received From Address: " + Packet.GetAddress() + "\n");

			// Data received
			data = Packet.GetData();
			help.print("Packet Received in Bytes: " + help.byteToString(data));

			// Form a String from the byte array, and print the string.
			received = new String(data,0,data.length);
			help.print("Packet Received in String: "+ received  + "\n");

			// Send Packet received to client
			help.print("Send Packet received from Server to the Client");

			// client sees them as being sent by the server
			if(Packet.GetRequest() == 3 && userInput == 7) { // DATA packet
				putError(Packet, userInput, sendSocket, clientPort);
			}else if(isTransferringFile() && userInput == 8 && Packet.GetPacketN() == 1){
				Packet p = Packet;
				putError(Packet, userInput, sendSocket, clientPort);
				userInput = -1;
				help.sendPacket(p, sendSocket, address, clientPort);
			}else if(isTransferringFile() && userInput >= 9 && userInput <= 11){
				putError(Packet, userInput, sendSocket, clientPort);
			}else{
				help.sendPacket(Packet, sendSocket, address, clientPort);
			}

			if(!isTransferringFile()){
				transferringFile = true;
			}
		}
	}

	// Simulate the User error and send the new packet to the Server
	public void putError(Packet newPacket, int userInput, DatagramSocket soc, int port){   

		String sender = "";
		String packetName ="";
		Packet p1;

		// Data received
		byte[] data = newPacket.GetData();
		byte[] mode = newPacket.GetMode().getBytes();
		byte[] fileName = newPacket.GetFile().getBytes();
		int blockNumber = newPacket.GetPacketN();
		int packetType = newPacket.GetRequest();

		if(packetType == 3){
			packetName = "DATA";
		}else if(packetType == 4){
			packetName = "ACK";
		}

		// Putting the packet data into a String
		String msg = new String(data);

		help.print("Received Packet Length: "+ msg.length() + "\n");

		//used for cases 2 and 3
		byte[] newData;

		switch(userInput){
		case 0: 

			help.print("Sending unchanged Request to Server to establish a connection");
			help.print("Packet Received in Bytes: " + msg);

			//forward the packet received
			help.sendPacket(newPacket, soc, address, port);

			this.userInput = -1;

			break;	

		case 1: // Change opcode

			help.print("Changing TFTP opcode");
			help.print("Original Packet: " + msg);

			//Change RRQ to WRQ or WRQ to RRQ 
			if(data[1]  == 1){
				data[1] = 2;
				help.print("changed from RRQ to WRQ"); 
			}else if(data[1] == 2){
				data[1] = 1;
				help.print("changed from WRQ to RRQ");
			}

			// New Packet
			help.print("Modified Packet: " + help.byteToString(data) + "\n");

			// Send Packet to Server
			Packet = new Packet(data);
			help.sendPacket(Packet, soc, address, port);

			this.userInput = -1;

			break;

		case 2: // change from RRQ/WRQ to an invalid request

			help.print("Changing request opcode to an invalid opcode");
			help.print("Original Packet: " + msg);

			//change opcode to an invalid opcode
			data[1] = 9;
			help.print("Modified Packet: " + help.byteToString(data) + "\n");

			//send new data to server
			help.sendPacket(data, soc, address, port);

			this.userInput = -1;

			break;

		case 3:	//Replace the Filename

			help.print("Removing filename");
			help.print("Original Packet: " + msg);

			//remove filename
			newData = new byte[3 + mode.length];
			newData[0] = data[0];
			newData[1] = data[1];
			newData[2] = 0;
			System.arraycopy(mode, 0, newData, 3, mode.length);
			newData[2 + mode.length] = 0;

			// New Packet
			help.print("Modified Packet: " + help.byteToString(newData) + "\n");

			// Send Packet to Server
			Packet = new Packet(newData);
			help.sendPacket(Packet, soc, address, port);

			this.userInput = -1;

			break;

		case 4: //Replace the Mode

			help.print("Changing the Original Mode to Something Else");
			help.print("Original Packet: " + msg);

			byte[] mod =  "octettttt".getBytes();

			int counter = 0, k;
			//Find the index of 0 before mode 01Filename0Mode0
			for(k = 0; k < data.length ; k++ ){

				if(data[k] == 0) {counter++;}
				if (counter == 2)  {break;}
			}

			//Change the mode using arraycopy
			newData = new byte[data.length + mod.length];
			System.arraycopy(data, 0, newData, 0, 2 + fileName.length);
			System.arraycopy(mod, 0, newData, k + 1, mod.length);

			//Change the byte after the mode to 0
			newData[newData.length - 1] = 0;

			// New Packet
			help.print("Modified Packet: " + help.byteToString(data));

			// Send Packet to Server
			Packet = new Packet(newData);
			help.sendPacket(Packet, soc, address, port);

			this.userInput = -1;

			break;

		case 5: // Change the Delimiter between Filename and Mode to 1

			help.print("Changing Delimiter between Filename and Mode");
			help.print("Original Packet" + msg);

			// Modify Packet
			int count = 0;
			int i;
			for(i = 0; i< data.length-1; i++){
				if(data[i] == 0){count++;}
				if(count == 2){ break;}
			}

			data[i] = 1;

			// New Packet
			help.print("Modified Packet" + help.byteToString(data));

			// Send Packet to Server
			Packet = new Packet(data);
			help.sendPacket(Packet, soc, address, port);

			this.userInput = -1;

			break;

		case 6:	// Change the Delimiter to 1

			help.print("Changing the Delimiter after Mode to 1");
			help.print("Original Packet" + msg);

			// Modify Packet
			count = 0;
			for(i = 0; i < data.length - 1; i++){
				if(data[i] == 0){ count++; }
				if(count == 3){break;}
			}

			data[i] = 1;

			// New Packet  
			help.print("Modified Packet" + help.byteToString(data) + "\n");

			// Send Packet to Server
			Packet = new Packet(data);
			help.sendPacket(Packet, soc, address, port);

			this.userInput = -1;

			break;

		case 7: // change DATA size to be greater than 512b

			help.print("Changing data size to be greater than 512b");

			//change DATA size
			newData = new byte[514];
			System.arraycopy(data, 0, newData, 0, data.length);
			newData[newData.length - 1] = 0;
			newData[newData.length - 2] = 1;

			// New Packet  
			Packet = new Packet(newPacket.GetPacketN(), newData);
			help.print("Modified Packet" + help.byteToString(Packet.GetData()) + "\n");

			//send Packet to server
			help.sendPacket(Packet, soc, address, port);

			this.userInput = -1;

			break;

		case 8: // Change the Socket Invalid TID and send to the server.
			help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
			help.print("Changing the Port");
			try {
				changeSocket = new DatagramSocket();
			} catch (SocketException e) {
				e.printStackTrace();
			}		  

			help.print("Port Changed by Creating New Socket");

			// Send Packet through a new port to the server or client 
			if(port == clientPort){
				help.print("Send to client through an invalid TID");
			}else{
				help.print("Send to server through an invalid TID");
			}

			help.sendPacket(Packet, changeSocket, address, port);

			// Receive Packet from server 
			Packet = help.recievePacket(changeSocket);

			// Extract info
			help.print("Packet Received From Port: "+ Packet.GetPort());
			help.print("Packet Received From Address: " + Packet.GetAddress() + "\n");

			help.print("Packet Received in Bytes: " + help.byteToString(Packet.GetData()));
			help.print("Error Code : " + Packet.GetErrCode());
			help.print("Error Message: " + Packet.GetErrMSG());
			help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
			
			break;

		case 9: // Delaying a Packet

			help.print("Beginning delaying a packet error simulation.");

			if(Req != clientReq){
				help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
				help.print("Wrong request.");
				help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
				help.sendPacket(newPacket, soc, address, port);
				userInput = -1;
				break;
			}

			if(!(pType == packetType && packetNumber == blockNumber)){
				help.print("Wrong packet type or block number");
				help.sendPacket(newPacket, soc, address, port);
				break;
			}

			//we have the correct packet number and type and request.
			
			help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
			
			//receiver times out
			help.print("receiving Message from first time out");
			p1 = help.recievePacket(soc);

			if(port == clientPort){ //sending to the client
				soc = sendReceiveSocket;
				sender = "Server";
			}else{ //sending to the server
				soc = sendSocket;
				sender = "Client";
			}

			//sender times out
			help.print("receiving Message from second time out");
			Packet = help.recievePacket(soc);

			if(port == clientPort){
				help.sendPacket(Packet, sendSocket, address, clientPort);
				help.sendPacket(p1, sendReceiveSocket, address, serverPort);
			}else{
				help.sendPacket(Packet, sendReceiveSocket, address, serverPort);
				help.sendPacket(p1, sendSocket, address, clientPort);
			}

			help.print("Mission was a success.");
			help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
			userInput = -1;

			break;

		case 10: // Lose a packet

			help.print("Beginning losing a packet error simulation.");

			if(Req != clientReq){
				
				help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
				help.print("Wrong request.");
				help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
				help.sendPacket(newPacket, soc, address, port);
				userInput = -1;
				break;
			}

			if(!(pType == packetType && packetNumber == blockNumber)){
				help.print("Wrong packet type or block number");
				help.sendPacket(newPacket, soc, address, port);
				break;
			}

			//we have the correct packet number and type and request.
			help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
			
			//receiver times out
			help.print("receiving Message from first time out");
			p1 = help.recievePacket(soc);

			if(port == clientPort){ //sending to the client
				soc = sendReceiveSocket;
				sender = "Server";
			}else{ //sending to the server
				soc = sendSocket;
				sender = "Client";
			}

			//sender times out
			help.print("receiving Message from " + sender + " time out");
			Packet = help.recievePacket(soc);

			if(port == clientPort){
				help.print("Sending packet to client");
				help.sendPacket(Packet, sendSocket, address, clientPort);
			}else{
				help.print("Sending packet to server");
				help.sendPacket(Packet, sendReceiveSocket, address, serverPort);
			}

			help.print("Mission was a success.");
			help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
			userInput = -1;

			break;
			
		case 11: //Duplicating packets
			
			help.print("Beginning duplicating a packet error simulation.");

			if(Req != clientReq){
				help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
				help.print("Wrong request.");
				help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
				help.sendPacket(newPacket, soc, address, port);
				userInput = -1;
				break;
			}

			if(!(pType == packetType && packetNumber == blockNumber)){
				help.print("Wrong packet type or block number");
				help.sendPacket(newPacket, soc, address, port);
				break;
			}
			
			if(port == clientPort){ //sending to the client
				sender = "Client";
			}else{ //sending to the server
				sender = "Server";
			}
			
			help.print("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
			
			p1 = Packet;
			
			help.print("Forwarding packet to " + sender);
			help.sendPacket(p1, soc, address, port);
			
			help.print("Receiving Packet from " + sender);
			Packet = help.recievePacket(soc);
			
			help.print("Sending duplicate packet " + sender);
			help.sendPacket(p1, soc, address, port);
			
			if(port == clientPort){ //sending to the client
				help.print("Sending packet to server");
				help.sendPacket(Packet, sendReceiveSocket, address, serverPort);
			}else{ //sending to the server
				help.print("Sending packet to client");
				help.sendPacket(Packet, sendSocket, address, clientPort);
			}
			
			help.print("Mission was a success.");
			help.print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
			userInput = -1;

			break;
		}
	}// End of Method

	public boolean isTransferringFile(){
		return transferringFile;
	}

	public void askInput(Scanner sc){
		try{
			//Prompt the User to Select what they would like to do the Packet			
			while(true){
				help.print("Choose Error Code to Implement");

				help.print("0:   Do Nothing");
				help.print("1:   Error Code 4: Change RRQ to WRQ or WRQ to RRQ");
				help.print("2:   Error Code 4: Change RRQ/WRQ to an invalid request");
				help.print("3:   Error Code 4: Remove Filename");
				help.print("4:   Error Code 4: Change Mode");
				help.print("5:   Error Code 4: Change Delimiter between Filename and Mode");
				help.print("6:   Error Code 4: Change Delimiter after Mode");
				help.print("7:   Error Code 4: DATA packet size greater than 512b");
				help.print("8:   Error Code 5: Change Port Number");
				help.print("9:   Delayed Packets");
				help.print("10:  Lose A Packet");
				help.print("11:  Duplicate A Packet");

				//Get the user input and convert to integer
				userInput = Integer.parseInt(sc.nextLine());

				System.out.println("Input Selected: "+ userInput);

				if(userInput >= 0 && userInput <= 8){ 
					break;
				}else if(userInput >= 9 && userInput <= 11){

					help.print("Select the request type: \n\t\t1 for RRQ \n\t\t2 for WRQ");
					Req = Integer.parseInt(sc.nextLine());
					help.print("You chose: " + Req);

					if(Req == 1 || Req == 2){

						help.print("Select the packet type: \n\t\t3 for DATA \n\t\t4 for ACK");
						pType = Integer.parseInt(sc.nextLine());
						help.print("You chose: " + pType);

						if(pType == 3 || pType == 4){

							help.print("Enter the block number");
							packetNumber = Integer.parseInt(sc.nextLine());
							help.print("You chose: " + packetNumber);

							if(packetNumber > -1){
								break;
							}
						}
					}
				}

				System.out.println("");
				help.print("Invalid Selection! Please Try Again");

			}

		}catch(Exception e){ 
			e.printStackTrace(); 
		}


		//After asking for input. Call the method to wait to receive
		passOnTFTP();

	}

	public static void main( String args[] )
	{
		boolean verbose;
		sc = new Scanner(System.in);
		while(true){
			// Ask the User the Mode to Operate in
			System.out.println("Would you like to run it in verbose mode (Y/N)?");

			String input = sc.nextLine();
			if(input.toUpperCase().equals("Y")){ verbose=true; break;}
			if(input.toUpperCase().equals("N")){ verbose=false; break;}
			System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
		} 


		boolean running = true;
		//call the askInput method first to ask the user for what to do
		while(running){

			//Create an object of the error simulator
			ErrorSimulator s = new ErrorSimulator(verbose);
			s.askInput(sc);


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
	}

}
