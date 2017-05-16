package iteration2;
/**
 * A collection of functions that is to be used across
 *   the various classes so as to avoid duplicate functions.
 *
 *Iteration Exclusive:
 * Future Iterations may add more helper functions when needed.
 *      
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

//A collection of helper libraries to automate certain events.
public class helplib {
	private String name;
	private boolean verbose;
	public helplib(String Name, boolean Verbose){
		name    = Name;
		verbose = Verbose;
	}
	//Open File and make sure it exists.
	public FileInputStream OpenIFile(String path){
		try { 
			FileInputStream in = new FileInputStream(path); return in;
		} catch (FileNotFoundException e) {
			print("FileIO::ERROR::File not found. "+ path); return null; 
		}
	}
	public FileOutputStream OpenOFile(String path, boolean notExist){
		File dir = new File(path);
		if(!dir.exists() && notExist){
			try { dir.createNewFile(); } 
			catch (IOException e) {  }
		}
		else if(dir.exists() && notExist){ return null; }
		
		try { 
			FileOutputStream out = new FileOutputStream(dir,true); return out;
		} catch (FileNotFoundException e) {
			//The code should just quit if it somehow comes to this.
			e.printStackTrace(); System.exit(1);
		}
		return null;
	}
	
	//Read bytes from a File at position.
	public byte[] ReadData(FileInputStream fIn, int block, int size){
		try { 
			long fileSize = fIn.getChannel().size();
			byte[] out = new byte[size];
			fIn.read(out);
			return out;
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}
	
	//Write bytes to end of file.
	public void WriteData(FileOutputStream fOut, byte[] block){
		int i = 0;
		byte[] write = null;
		for(i = 0; i<block.length; i++){
			if(block[i]==0){ 
				write = new byte[i];
				for(int j = 0; j<i; j++){
					write[j] = block[j];
				}
				break;
			}
		}
		if(i>=block.length) write = block;
		try { fOut.write(write);
		} catch (IOException e) {e.printStackTrace(); }
	}
	
	//Custom Print statement that adds the class name with it;
	public void print(String Message){
		System.out.println(name+"::: "+Message);
	}
	
	//Custom Print that doesn't get printed unless stated otherwise;
	public void printd(String Message){
		if(verbose){
			System.out.println(name+":D: "+Message);
		}
		
	}
	
	//Helper shortens the send command;
	public void sendPacket(Packet p, DatagramSocket soc, InetAddress addr, int port){
		try{
			byte[] bReq = p.toBytes();
			DatagramPacket spkt = new DatagramPacket(bReq, bReq.length, addr, port);
			soc.send(spkt);
			printd("\nSent the packet: "+p+"\nWith the following bytes:\n"+byteToString(bReq)+"\n");
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); }
	}
	
	/*//Helper shortens the receive command;
	public byte[] recievePacket(DatagramSocket soc){
		try{
			byte[] bRec = new byte[Packet.PACKETSIZE];
			DatagramPacket rpkt = new DatagramPacket(bRec,bRec.length);
			soc.receive(rpkt);
			printd("Got the following bytes:\n"+byteToString(bRec));
			return bRec;
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); }
		return null;
	}*/
	
	//Helper shortens the receive command;
	public Packet recievePacket(DatagramSocket soc){
		Packet rec = new Packet();
		try{
			byte[] bRec = new byte[Packet.PACKETSIZE];
			DatagramPacket rpkt = new DatagramPacket(bRec,bRec.length);
			soc.receive(rpkt);
			printd("Packet received from:\nPort:    "+rpkt.getPort()+"\nAddress: "+ rpkt.getAddress());
			printd("Got the following bytes:\n"+byteToString(bRec)+"\n");
			rec.byteParseFill(bRec);
			rec.SetAddress(rpkt.getAddress());
			rec.SetPort(rpkt.getPort());
			return rec;
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); }
		return rec;
	}
	
	public Packet recievePacket(DatagramSocket soc, int timeout) throws IOException{
		Packet rec = new Packet();
		soc.setSoTimeout(timeout);
		byte[] bRec = new byte[Packet.PACKETSIZE];
		DatagramPacket rpkt = new DatagramPacket(bRec,bRec.length);
		soc.receive(rpkt);
		printd("Packet received from:\nPort:    "+rpkt.getPort()+"\nAddress: "+ rpkt.getAddress());
		printd("Got the following bytes:\n"+byteToString(bRec)+"\n");
		rec.byteParseFill(bRec);
		rec.SetAddress(rpkt.getAddress());
		rec.SetPort(rpkt.getPort());
		
		return rec;

		
		
	}

	
	//Shortens the action of sending and receiving data;
	public Packet sendReceive(Packet p, DatagramSocket soc, InetAddress addr, int port){
		sendPacket(p, soc, addr, port);
		printd("Awaiting response...");
		Packet rec = recievePacket(soc);
		printd("Bytes parsed into following Packet: "+rec+"\n");
		return rec;
	}

	
	//Allows the checking of a packet an whether or not its okay;
	public boolean isOkay(Packet P, int Request){
		if(P.GetRequest() == 5) {
			print("Error received.");
			return !handleError(P);
		}
		else if(P.GetRequest() != Request && Request!=0) {
			print("Unexpected request.");
			return false;
		}
		else if(!P.GetValid()) {
			print("Invalid packet received.");
			return false;
		}
		return true;
	}
	
	//Allows Handles any error received;
	public boolean handleError(Packet P){
		if(P.GetRequest() == 5) {
			print(P.toString()); 
			return true;
		}
		return false;
	}
	
	//For ease of sight, print data but avoid duplicating too many 0s without losing actual message
	public String byteToString(byte[] arr){
		String res = "";
		int i = 0;
		int j = 0;
		for(i = 0; i < 4; i++){
			if      (arr[i]<10 ) res+= "  ";
			else if (arr[i]<100) res+=  " ";
			res+=arr[i]+", ";
			j++;
			if(j==16){ j=0; res+="\n"; }
		}
		for(i = 4; i < arr.length; i++){
			if(arr[i]==arr[i-1]&&arr[i]==0){}
			else{
				if      (arr[i]<10 ) res+= "  ";
				else if (arr[i]<100) res+=  " ";
				res+=arr[i]+", ";
				j++;
			}
			if(j==16){ j=0; res+="\n"; }
		}
		return res;
	}
	
	public String byteToStringW0(byte[] arr){
		String res = "";
		int i = 0;
		int j = 0;
		for(i = 0; i < arr.length; i++){
			if      (arr[i]<10 ) res+= "  ";
			else if (arr[i]<100) res+=  " ";
			res+=arr[i]+", ";
			j++;
			if(j==16){ j=0; res+="\n"; }
		}
		return res;
	}
}
