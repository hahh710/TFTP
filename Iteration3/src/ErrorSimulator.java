

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
	   private int 					clientPort, serverPort, userInput, Req, packetNumber;
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
			   help.print("Packet Received From Port: "+ clientPort);
		       help.print("Packet Received From Address: " + Packet.GetAddress() + "\n");
		       
		       // Filename and Mode
		       help.print("Filename : " + Packet.GetFile() + "  |   Mode : " + Packet.GetMode());
		       
		       //Length of the packet received
		       data = Packet.GetData();
		       int len = data.length;
		       help.print("Length of Received Packet: " + len + "\n");
		       help.print("Packet Received in Bytes: " + Arrays.toString(data));
		   
		       // Form a String from the byte array, and print the string.
		       String received = new String(data,0,len);
		       help.print("Packet Received in String: "+ received  + "\n");
		       
		       // server sees them as being sent by the client
		       if(!isTransferringFile() && userInput < 7 && userInput > -1){
		    	   putError(Packet, userInput, sendReceiveSocket, 69);
		       }else if(Packet.GetRequest() == 3 && userInput == 7){ // DATA packet
		    	   putError(Packet, userInput, sendReceiveSocket, serverPort);
		       }else if(isTransferringFile() && userInput == 8){
		    	   Packet p = Packet;
		    	   putError(Packet, userInput, sendReceiveSocket, serverPort);
		    	   help.sendPacket(p, sendReceiveSocket, address, serverPort);
		       }else if (!isTransferringFile()){
		    	   help.sendPacket(Packet, sendReceiveSocket, address, 69);
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
		       help.print("Packet Received in Bytes: " + Arrays.toString(data));
		       
		       // Form a String from the byte array, and print the string.
		       received = new String(data,0,data.length);
		       help.print("Packet Received in String: "+ received  + "\n");
		       
		       // Send Packet received to client
		       help.print("\nSend Packet received from Server to the Client");
		       
		       // client sees them as being sent by the server
		       if(Packet.GetRequest() == 3 && userInput == 7) { // DATA packet
		    	   putError(Packet, userInput, sendSocket, clientPort);
		       }else if(isTransferringFile() && userInput == 8){
		    	   Packet p = Packet;
		    	   putError(Packet, userInput, sendSocket, clientPort);
		    	   userInput = -1;
		    	   help.sendPacket(p, sendSocket, address, clientPort);
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
		   
		   // Data received
		   byte[] data = newPacket.GetData();
		   byte[] mode = newPacket.GetMode().getBytes();
		   byte[] fileName = newPacket.GetFile().getBytes();
		   
		   // Putting the packet data into a String
		   String msg = new String(data);
		   
		   help.print("\nReceived Packet Length: "+ msg.length() + "\n");
		   
		   //used for cases 2 and 3
		   byte[] newData;
		   
		   switch(userInput){
		   case 0: 
			   
			   help.print("\nSending unchanged Request to Server to establish a connection");
			   help.print("Packet Received in Bytes: " + msg);
			   
			   //forward the packet received
			   help.sendPacket(newPacket, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;	
			   
		   case 1: // Change opcode
			   
			   help.print("\nChanging TFTP opcode\n");
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
			   help.print("Modified Packet: " + Arrays.toString(data) + "\n");
			   
			   // Send Packet to Server
			   Packet = new Packet(data);
			   help.sendPacket(Packet, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;
			   
		   case 2: // change from RRQ/WRQ to an invalid request
			   
			   help.print("\nChanging request opcode to an invalid opcode");
			   help.print("Original Packet: " + msg);
			   
			   //change opcode to an invalid opcode
			   data[1] = 9;
			   help.print("Modified Packet: " + Arrays.toString(data) + "\n");
			   
			   //send new data to server
			   Packet = new Packet(data);
			   help.sendPacket(Packet, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;
			   
		   case 3:	//Replace the Filename
			   
			   help.print("\nRemoving filename");
			   help.print("Original Packet: " + msg);
			   
			   //remove filename
			   newData = new byte[3 + mode.length];
			   newData[0] = data[0];
			   newData[1] = data[1];
			   newData[2] = 0;
			   System.arraycopy(mode, 0, newData, 3, mode.length);
			   newData[2 + mode.length] = 0;
			   
			   // New Packet
			   help.print("Modified Packet: " + Arrays.toString(newData) + "\n");
				
			   // Send Packet to Server
			   Packet = new Packet(newData);
			   help.sendPacket(Packet, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;
			   
		   case 4: //Replace the Mode
			   
			   help.print("\nChanging the Original Mode to Something Else");
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
			   help.print("Modified Packet: " + Arrays.toString(data));
			      
			   // Send Packet to Server
	           Packet = new Packet(newData);
			   help.sendPacket(Packet, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;
			   
		   case 5: // Change the Delimiter between Filename and Mode to 1
			   
			   help.print("\nChanging Delimiter between Filename and Mode");
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
			   help.print("Modified Packet" + Arrays.toString(data));
			   
		       // Send Packet to Server
	           Packet = new Packet(data);
			   help.sendPacket(Packet, soc, address, port);
				
			   this.userInput = -1;
			   
			   break;
			   
		   case 6:	// Change the Delimiter to 1
			   
			   help.print("\nChanging the Delimiter after Mode to 1");
			   help.print("Original Packet" + msg);
			   
			   // Modify Packet
			   count = 0;
			   for(i = 0; i < data.length - 1; i++){
				   if(data[i] == 0){ count++; }
				   if(count == 3){break;}
			   }
			   
			   data[i] = 1;
			     
			   // New Packet  
			   help.print("Modified Packet" + Arrays.toString(data) + "\n");
			   
			   // Send Packet to Server
	           Packet = new Packet(data);
			   help.sendPacket(Packet, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;
			   
		   case 7: // change DATA size to be greater than 512b
			   
			   help.print("\nChanging data size to be greater than 512b");
			   
			   //change DATA size
			   newData = new byte[514];
			   System.arraycopy(data, 0, newData, 0, data.length);
			   newData[newData.length - 1] = 0;
			   newData[newData.length - 2] = 1;
			   
			   // New Packet  
			   Packet = new Packet(newPacket.GetPacketN(), newData);
			   help.print("Modified Packet" + Arrays.toString(Packet.GetData()) + "\n");
			   
			   //send Packet to server
			   help.sendPacket(Packet, soc, address, port);
			   
			   this.userInput = -1;
			   
			   break;
		   
		   case 8: // Change the Socket Invalid TID and send to the server.
			   
			   help.print("\nChanging the Port");
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
		       
		       help.print("Packet Received in Bytes: " + Arrays.toString(Packet.GetData()));
		       help.print("Error Code : " + Packet.GetErrCode());
		       help.print("Error Message: " + Packet.GetErrMSG());
		       
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
					
				   //Get the user input and convert to integer
				   userInput = Integer.parseInt(sc.nextLine());
					
				   System.out.println("Input Selected: "+ userInput);
					
				   if(userInput >= 0 && userInput <= 8){ 
					   break;
				   }
				   
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
