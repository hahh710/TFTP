Read Me
=====================================================================
File Transfer Protocol
Sending File format from the Client to the intermediate and finally 
to the Server which in turns send back an acknowledgement

Client:
  * Ensure that all files are on the same machine
    If not, change the Inet Address to match the IP address of the intermediate

===================================================================
Getting IP Address
* Run command Prompt on the machine
* On window Machine (Enter Ip config) or ipconfig on Linux Machine
* if the machine are using wifi
      * Find Ipv4 (thats the IP address)
      * change the InetAddress to this the new Address
* If machine is using ethernet
      * Find eth0 (thats the IP Address)
      * Change the InetAddress to this new Address
===================================================================

Intermediate:
  * Ensure the client and intermediate are on the same machine and 
    if not, change the IP Address as described above
  * Ensure the receiving Port is the same as the client port
  * Ensure the Sending port is the same as the server receiveing port
  * Run the intermediate first before running the client


Server:
  * Ensure the server and intermediate are on the same machine and 
    if not, change the IP Address as described above
  * Ensure the receiving Port is the same as the sending port of intermediate
  * Ensure the Sending port is the same as the server receiveing port
  * Run the intermediate first then the server before running the client

=========================================================================
Running the Program
Step 1:  Start the Sever 
Step 2: Start the Error Simulator
Step 3: Start the Client

The client prompts the user for for the whether it is a RRQ or WRQ then 
ask the client for the mode of Operation(Verbose or Normal).
The client initiate the communication with the server through handshaking and then 
commerce connection with the Server.

The Error Simulator: acts as a middle man between the client and the server.
   For RRQ: Pass on the packet from the client to the server and then waits for acknowledgement 
   from the server and then pass the acknowledgement back to the client.
   


================================================================================
Helper Function 
* Helplib
* Packet

Instruction:
* Ensure that helplib and packet are in the file project as the client, simulator and server

Helplib
An assortment of reusable functins accorss the classes. Helplib has a method for:
* Sending Packet
* receiving Packet
* Print with Verbose communication
* handing File input and output

packet
packet hide away manually dealing with creating byte to be sent in Datagram Packet.

Anything other than Above will give an Invalid Packet and the 
Server will throw an exception

