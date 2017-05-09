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
To get a Valid Packet:
Enter:   01filename.txt0octet0     : Read Request
Enter:   02filename.txt0octet0     : write Request
Enter:   03filename.txt0octet0     : Invalid Request
Enter:   01filename.txt0netascii0  : Read Request 
Enter:   02filename.txt0netascii0  : write Request
Enter:   03filename.txt0netascii0  : Invalid Request

Anything other than Above will give an Invalid Packet and the 
Server will throw an exception

