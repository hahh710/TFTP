/**
 * Error Simulator for a TFTP
 *Adapted from the File provided by the Professor
 *
 *Iteration 1:
 *      1. Receive File name from client and send to Server.
 *      2. Wait to receive from the Server
 *      3. Receive acknowledgement from server and send to client
 *      4. Then wait to Receive
 *      
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ErrorSimulator {
   private helplib 		  help;
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;
   
   //Constructor for the Error Simulator class
   public ErrorSimulator()
   {
      try {
         help = new helplib("ErrorSim",true);
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

      byte[] data;
      
      int clientPort, len;

      //Loop Forever
      for(;;) { 
         // Construct a DatagramPacket for receiving packets up
         // to Packet.PACKETSIZE bytes long (the length of the byte array).
         
         data = new byte[Packet.PACKETSIZE];
         receivePacket = new DatagramPacket(data, data.length);

         help.print("Simulator: Waiting for packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(receivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         help.print("Simulator: Packet received:");
         help.print("From host: " + receivePacket.getAddress());
         
         //get the port it received from
         clientPort = receivePacket.getPort();
         help.print("Host port: " + clientPort);
         
         //Length of the packet received
         len = receivePacket.getLength();
         help.print("Length: " + len);
         help.print("Containing: " );
         help.print("Containing: "+Arrays.toString(data));
         // Form a String from the byte array, and print the string.
         String received = new String(data,0,len);
         help.print(received);
         
         
         //Create Datagram packet to send to server and wait to receive from the server
         sendPacket = new DatagramPacket(data, len,
                                        receivePacket.getAddress(), 69);
        
         help.print("Simulator: sending packet.");
         help.print("To host: " + sendPacket.getAddress());
         help.print("Destination host port: " + sendPacket.getPort());
         len = sendPacket.getLength();
         help.print("Length: " + len);
         //Printing out the byte that is being sent
         help.printd("Containing: "+Arrays.toString(data));

         // Send the datagram packet to the server via the send/receive socket.

         try {
            sendReceiveSocket.send(sendPacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }
         
         // Construct a DatagramPacket for receiving packets up
         // to Packet.PACKETSIZE bytes long (the length of the byte array).

         data = new byte[Packet.PACKETSIZE];
         receivePacket = new DatagramPacket(data, data.length);

         help.print("Simulator: Waiting for packet.");
         try {
            // Block until a datagram is received via sendReceiveSocket.
            sendReceiveSocket.receive(receivePacket);
         } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         help.print("Simulator: Packet received:");
         help.print("From host: " + receivePacket.getAddress());
         help.print("Host port: " + receivePacket.getPort());
         len = receivePacket.getLength();
         help.print("Length: " + len);
         help.printd("Containing: "+Arrays.toString(data));

         // Construct a datagram packet that is to be sent to a specified port
         sendPacket = new DatagramPacket(data, receivePacket.getLength(),
                               receivePacket.getAddress(), clientPort);

         help.print( "Simulator: Sending packet:");
         help.print("To host: " + sendPacket.getAddress());
         help.print("Destination host port: " + sendPacket.getPort());
         len = sendPacket.getLength();
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
         sendSocket.close();
      } // end of loop

   }
   

   public static void main( String args[] )
   {
	   ErrorSimulator s = new ErrorSimulator();
       s.passOnTFTP();
   }
}

