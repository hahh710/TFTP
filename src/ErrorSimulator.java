/**
 * Error Simulator for a TFTP
 *Adapted from the File provided by the Professor
 *
 *Iteration 2:
 *      1. Receive File name from client and send to Server.
 *      2. Wait to receive from the Server
 *      3. Receive acknowledgement from server and send to client
 *      4. Then wait to Receive
 *      
 *      ERROR:   Acknowledgement packet to Client is sending to the wrong port
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ErrorSimulator {
	
   private helplib 		  help;
   private Packet 		  Packet;
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket ServerReceivePacket, clientReceivePacket, errorPacket;
   private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket, sendSocketw;
   private int userInput;
   private Scanner sc;
   private InetAddress address;
   private int clientPort, index;
   
   //Constructor for the Error Simulator class
   public ErrorSimulator()
   {
	   
	   try {
		address = InetAddress.getLocalHost();
	    } catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	    }
	   
      try {
    	  boolean verbose;
    	  sc = new Scanner(System.in);
    	  while(true){
    		  
    		    // Ask the User the Mode to Operate in   *This is not really required so take it out
				System.out.println("Would you like to run it in verbose mode (Y/N)?");
				
				String input = sc.nextLine();
				if(input.toUpperCase().equals("Y")){ verbose=true; break;}
				if(input.toUpperCase().equals("N")){ verbose=false; break;}
				System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
		 }
    	  
    	 //sc.close();
         help = new helplib("ErrorSim",verbose);
         
    	 //Receive socket bind to port 23
         receiveSocket = new DatagramSocket(23);
         
         // socket for send and receive bind to an available port
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
   }

   public void passOnTFTP()
   {
      //Loop Forever
     // for(;;) { 
         // Construct a DatagramPacket for receiving packets up
         // to Packet.PACKETSIZE bytes long (the length of the byte array).
         
         byte[] data = new byte[Packet.PACKETSIZE];
         clientReceivePacket = new DatagramPacket(data, data.length);

         help.print("Simulator: Waiting for packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(clientReceivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         //Extracting the Packet Received from the Client
         help.print("Port the data comes from"+ clientReceivePacket.getPort());
         
         // Process the received datagram.
         help.print("Simulator: Packet received:");
         help.print("From host: " + clientReceivePacket.getAddress());
         
         //get the port it received from
         clientPort = clientReceivePacket.getPort();
         help.print("Host port: " + clientPort);
         
         //Length of the packet received
         int len = clientReceivePacket.getLength();
         help.print("Length: " + len);
         help.print("Containing: " );
         help.print("Containing: "+Arrays.toString(data));
         
         // Form a String from the byte array, and print the string.
         String received = new String(data,0,len);
         help.print(received);
         
         //Call the method to Add Error to the packet
         putError(clientReceivePacket, userInput, sendReceiveSocket, data, 69);
      //}
   }
      
      public void receiveAckFromServer(){
         
         //Wait to receive Acknowledgement from the Receive
         help.print("Waiting to Receive Acknowledgement from Server");
         // Construct a DatagramPacket for receiving packets up
         // to Packet.PACKETSIZE bytes long (the length of the byte array).
        for(;;){
        	
         byte[] data = new byte[Packet.PACKETSIZE];
         ServerReceivePacket = new DatagramPacket(data, data.length);

         help.print("Simulator: Waiting for packet From Server.");
         try {
            // Block until a datagram is received via sendReceiveSocket.
            sendReceiveSocket.receive(ServerReceivePacket);
         } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         help.print("Simulator: Packet received:");
         
         address = ServerReceivePacket.getAddress();
         help.print("From host: " + address);
         help.print("Host port: " + ServerReceivePacket.getPort());
         int len = ServerReceivePacket.getLength();
         help.print("Length: " + len);
         help.printd("Containing: "+Arrays.toString(data));
         
         //Sending Acknowledgement to Client
         sendAckToClient(ServerReceivePacket, clientPort, address, data);
        }// end of For loop
      }
      
      
     public void sendAckToClient(DatagramPacket sendPacket, int port, InetAddress addr, byte[] data){
         // Construct a datagram packet that is to be sent to a specified port
       
    	// errorPacket = new DatagramPacket(sendPacket, address, port);
    	 
    	
   	     String msg = new String(sendPacket.getData());
	  
   	     sendPacket = new DatagramPacket(msg.getBytes(), msg.length(), addr, port);

         help.print( "Simulator: Sending packet:");
         help.print("To host: " + addr);
         help.print("Destination host port: " + port);
         int  len = sendPacket.getLength();
         help.print("Length: " + len);
         help.printd("Containing: "+Arrays.toString(data));

         // Send the datagram packet to the client via a new socket.

         
         try {
            // Construct a new datagram socket and bind it to any port
            // on the local host machine. This socket will be used to
            // send UDP Datagram packets.
            sendSocket = new DatagramSocket();
         } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
         }

         try {
            sendSocket.send(sendPacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         help.print("Simulator: packet sent using port " + sendSocket.getLocalPort());
         help.print("");

         // We're finished with this socket, so close it.
         //sendSocket.close();
    
     } 
   
   public void putError(DatagramPacket newPacket, int userInput, DatagramSocket soc, byte[] data, int port){   
	   
	   //Putting the packet data into a String
	   String msg = new String(newPacket.getData());
	   
	   // Converting the string to array
	 //  String[] msgArray;
	 //  System.arraycopy(msg, 0, msgArray, 0, msg.length());
	   
	   help.print(msg);
	   
	   //Converting the packet to byte
	   byte[] message = new byte[512];
	   
	   help.print(""+ msg.length());
	   help.print(""+ message.length);
	   //Copying the message received into message which of type byte
	  System.arraycopy(msg.getBytes(), 0, message, 0, msg.length());

	   
	   // Change the packet to array Characters
	 //  char[] msgArray = msg.toCharArray();
	   
	   help.print("Received Packet: " + msg);
	   
	   switch(userInput){
	   case 0: 
		     //Do something
		     help.print("No changes to Packet");	
		   
			 help.print("Calling sendPacket Method Now");
			 sendPacket(soc, newPacket, address, data, 69);
		   break;
	   case 1: 
		   
		   help.print("Changed TFTP opcode");
		   
		   //Change RRQ to WRQ or WRQ to RRQ 
		   if(message[1]  == '1'){
			   message[1] = '2';
			   
			   help.print("Whats being sent: " + message);
			   
			   help.print("Case 1: 1st if Sending New Packet");
			   newPacket = new DatagramPacket(message, message.length, address, 69 );
			   sendPacket(soc, newPacket, address, data, 69);
			   break;
		    }else if(message[1]  == '2'){
		    	message[1] = '1';
		    	help.print("Case 1: 2nd if: Sending New Packet");
		    	newPacket = new DatagramPacket(message, message.length, address, 69 );
				sendPacket(soc, newPacket, address, data, 69);
		       break;   
		    }else{		
		    	help.print("Case 1: else: Sending New Packet");
		    	newPacket = new DatagramPacket(message, message.length, address, 69 );
			    sendPacket(soc, newPacket, address, data, 69);
			    break;    
		    }
		   
	   case 2:		   
		   //Replace the Filename   
		   StringBuilder msgb = new StringBuilder("kkkkkk");
		   
		   for(int i = 1; i < msg.length() ; i++ ){
			  if(Character.isAlphabetic(msg.charAt(i))){
				  help.print("inside if Statement");
				  
				  msgb.replace(0, i, "GF");
				 // msgb.append("*");
			  } 
			  help.print("Case 2: "+ msgb);
			  newPacket = new DatagramPacket(msg.toString().getBytes(), msg.length(), address, 69 );
			  sendPacket(soc, newPacket, address, data, 69);
			  break;
		   }
		   
	   case 3:
		   //Replace the Mode
		  // StringBuilder msgb1 = new StringBuilder(msg);
		   byte[] mode = "octet".getBytes();
		   byte[] mode2 = "netascii".getBytes();
		   
		   //Find the index of 0 before mode 01Filename0Mode0
		   for(int i = 1; i < message.length ; i++ ){
			  if(message[i] == mode[i]){
				  message[i] = mode2[i];
				  help.print(""+ message[i]);
				  help.print(""+ mode2[i]);
			  }else{
				  message[i] = mode[i];
				  help.print(""+ message[i]);
				  help.print(""+ mode[i]);
			  }
			  break;
		   }
		   
		   help.print("case 3: Sending New Packet" + message);
			  newPacket = new DatagramPacket(message, message.length, address, 69 );
			  sendPacket(soc, newPacket, address, data, 69);
	   case 4:
		   //Change the Delimiter btw Filename and Mode to 1
		  for(int i = 0; i<message.length; i++){
			  if(message[i] == '0'){
				  message[i] = '1';
			  }
			  break;
		  }
		  
		   help.print("Sending New Packet");
			  newPacket = new DatagramPacket(message, message.length, address, 69 );
			  sendPacket(soc, newPacket, address, data, 69);
			  
	   case 5:
		   //Change the Delimiter to 1
         
		      message[message.length-1] = '1';
		      help.print(""+ message[message.length-1]);
		      help.print("Sending New Packet");
			  newPacket = new DatagramPacket(message, message.length, address, 69 );
			  sendPacket(soc, newPacket, address, data, 69);
	   case 6: 
		   //Change the Socket
		   try {
			DatagramSocket socketb = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}		  
		   help.print("Sending New Packet");
			  newPacket = new DatagramPacket(message,message.length, address, 69 );
		try {
			sendSocketw.send(newPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		   
		   
		   
		 help.print("Calling sendPacket Method Now");
		 sendPacket(soc, newPacket, address, data, 69); 

	   }
   }
    
  // For sending Packet
  public void sendPacket(DatagramSocket soc, DatagramPacket packet, InetAddress addr, byte[] data, int port){
	  
	  help.print("Simulator: Inside sendPacket method: attempting to send");
	  
	  port = 69;
	  String msg = new String(packet.getData());
	  
	  packet = new DatagramPacket(msg.getBytes(), msg.length(), addr, port);
	  
	  
	  
	  help.print("Packet being sent:" + packet.getData());
	  
      help.print("To host: " + addr);
      help.print("Destination host port: " + packet.getPort());
      int  len = packet.getLength();
      help.print("Length: " + len);
      
      //Printing out the byte that is being sent
      help.printd("Containing: "+Arrays.toString(data));

      // Send the datagram packet to the server via the send/receive socket.

      try {
    	  help.print("Packet Sent to Server");
         soc.send(packet);
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      
      // After Sending to Server. Go back to waiting to receive
      receiveAckFromServer();
  }
  
     public void askInput(){
	    try{
		  //Prompt the User to Select what they would like to do the Packet			
			while(true){
				
				help.print("Choose Option");
				
				help.print("0:   Do Nothing");
				help.print("1:   Invalid TFTP Opcode");
				help.print("2:   Invalid Filename");
				help.print("3:   Invalid Mode ");
				help.print("4:   Invalid Delimeter btw Filename and Mode");
				help.print("5:   Invalid Delimeter after Mode");
				help.print("6:   Port Error");
				
				//Get the user input and convert to integer
				userInput = Integer.parseInt(sc.nextLine());
				
				System.out.println("Input Selected: "+ userInput);
				
				if(userInput >= 0 && userInput <= 8){ break;}
				      help.print("Invalid Selection! Please Try Again");
				
	    }
	   }catch(Exception e){ e.printStackTrace(); }
	    
	    sc.close();
	    
	   //After asking for input. Call the method to wait to receive
	    passOnTFTP();
    }


   public static void main( String args[] )
   {
	   ErrorSimulator s = new ErrorSimulator();
       
	   //call the askInput method first to ask the user for what to do
       s.askInput();
       
       //s.passOnTFTP();
       
   }
}
