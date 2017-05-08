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
			printd("Sent the packet:\n"+p+"\nWith the following bytes:\n"+Arrays.toString(bReq));
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); }
	}
	
	//Helper shortens the receive command;
	public byte[] recievePacket(DatagramSocket soc){
		try{
			byte[] bRec = new byte[Packet.PACKETSIZE];
			DatagramPacket rpkt = new DatagramPacket(bRec,bRec.length);
			soc.receive(rpkt);
			printd("Got the following bytes:\n"+Arrays.toString(bRec));
			return bRec;
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); }
		return null;
	}
	
	//Shortens the action of sending and receiving data;
	public Packet sendReceive(Packet p, DatagramSocket soc, InetAddress addr, int port){
		sendPacket(p, soc, addr, port);
		printd("Awaiting response...");
		byte[] bRec = recievePacket(soc);
		Packet rec = new Packet();
		rec.byteParseFill(bRec);
		printd("Bytes parsed into following Packet:\n"+rec);
		return rec;
	}
}
