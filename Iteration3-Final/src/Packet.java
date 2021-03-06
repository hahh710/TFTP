
/**
 * Packet Object to avoid manually working with packets.
 *
 *Iteration Exclusive:
 * Built to be compatible with future iterations with minimal changes.
 *      
 */


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

//Packet Object class to avoid manually working with packets.
public class Packet {
	//Public packet MetaData Variables to allow for the max packets to easily be edited.
	public static int PACKETSIZE       = 1024;
	public static int BLOCKNUMBYTESIZE = 2;
	public static int DATASIZE         = 512;
	public static int MAXPACKETS       = (int) Math.pow(2, 8*BLOCKNUMBYTESIZE);
	
	//Private packet variables.
	private int    		Req     = 0;
	private int 		Size    = 0;
	private byte[] 		bData   = new byte[DATASIZE];
	private int    		pNum    = 0;
	private int    		ErrCode = 0;
	private String 		ErrMSG  = "";
	private String 		File    = "";
	private String 		Mode    = "";
	private boolean 	valid	= true;
	private int			Port    = 0;
	private InetAddress	Address = null;
	//Getters;
	public int     		GetRequest() { return Req;     }
	public byte[]  		GetData   () { return bData;   }
	public int     		GetPacketN() { return pNum;    }
	public int     		GetErrCode() { return ErrCode; }
	public String  		GetErrMSG () { return ErrMSG;  }
	public String  		GetFile   () { return File;    }
	public String  		GetMode   () { return Mode;    }
	public boolean 		GetValid  () { return valid;   }
	public int	   		GetPort   () { return Port;	   }
	public InetAddress 	GetAddress() { return Address; }
	
	public void	SetPort 	(int Port) 			  { this.Port = Port;		}
	public void	SetAddress 	(InetAddress Address) { this.Address = Address; }
	
	//Constructors-----------------------------------------------------------------------//
	//Read and Write requests;
	public Packet(int r, String f, String m){
		Req  = r;
		File = f;
		Mode = m;
		valid = true;
	}
	
	//Data Packet
	public Packet(int n, byte[] d){
		Req   = 3;
		bData = d;
		pNum  = n;
		valid = true;
	}
	
	//Acknowledgement Packet;
	public Packet(int n){
		Req  = 4;
		pNum = n;
		valid = true;
	}
	
	//Error Packet;
	public Packet(int e, String m){
		Req     = 5;
		ErrCode = e;
		ErrMSG  = m;
		valid = true;
	}
	
	public Packet(byte[] data){
		byteParseFill(data);
	}
	
	public Packet(){ }
	
	//toString overload
	public String toString() {
		String r = Req2Str() + " - ";
		if(Req==1||Req==2)
			r +="FILE: '" + File + "' MODE: '" + Mode+"'";
		else if(Req==3)
			r += "BLOCK: " + pNum;
		else if(Req==4)
			r += "BLOCK: " + pNum;
		else if(Req==5)
			r += ErrCode + " : " + ErrMSG;
		return r;
	}
	
	
	//Byte[] to Packet converter, returns false if the packet cannot be parsed;
	public boolean byteParseFill(byte[] input){
		bData = input;
		int i = 0;
		if(input[i++]!=0) return false;
		Req = input[i++];
		if(Req<1||Req>5)  return false;
		//Handling READ/WRITE requests;
		if(Req==1||Req==2){
			File = strExtract(input, getNextVal(0,i,input)-i, i);
			i = getNextVal(0,i,input);
			if(input[i++]!=0) return false;
			Mode = strExtract(input, getNextVal(0,i,input)-i, i);
			i = getNextVal(0,i,input);
			valid = true;
		}
		else if(Req == 3){
			pNum = b2i(byteExtract(input,BLOCKNUMBYTESIZE,i));
			i+=BLOCKNUMBYTESIZE;
			bData = byteExtract(input,input.length-4,i);
			valid = true;
		}
		else if(Req == 4){
			pNum = b2i(byteExtract(input,BLOCKNUMBYTESIZE,i));
			valid = true;
		}
		else if(Req == 5){
			ErrCode = b2i(byteExtract(input,BLOCKNUMBYTESIZE,i));
			i+=2;
			ErrMSG = strExtract(input, getNextVal(0,i,input)-i, i);
			valid = true;
		}
		else{
			valid = false;
		}
		return true;
		
	}
	
	//Converts packet to byte array;
	public byte[] toBytes(){
		byte[] out = new byte[1024];
		out[0]=0; out[1]=(byte)Req;
		int i=2;
		if(Req<1||Req>5)  return null;
		if(Req==1||Req==2){
			i = offsetByteCopy(out, File.getBytes(),i);
			out[i++] = 0;
			i = offsetByteCopy(out, Mode.getBytes(),i);
			out[i++] = 0;
			byte[] res = new byte[i];
			System.arraycopy(out, 0, res, 0, i);
			out=res;
		}
		if(Req==3){
			i = offsetByteCopy(out,i2b(pNum,BLOCKNUMBYTESIZE),i);
			System.arraycopy(bData, 0, out, i, bData.length);
			i+=bData.length;
			byte[] res = new byte[i];
			System.arraycopy(out, 0, res, 0, i);
			out=res;
		}
		if(Req==4){
			i = offsetByteCopy(out,i2b(pNum,BLOCKNUMBYTESIZE),i);
			byte[] res = new byte[i];
			System.arraycopy(out, 0, res, 0, i);
			out=res;
		}
		if(Req==5){
			i = offsetByteCopy(out,i2b(ErrCode,2),i);
			i = offsetByteCopy(out, ErrMSG.getBytes(),i);
			out[i++] = 0;
			byte[] res = new byte[i];
			System.arraycopy(out, 0, res, 0, i);
			out=res;
		}
		return out;
	}
	

	//Helper functions-------------------------------------------------------------------//
	
	//The following are mainly from Vishahan's (100994856), Assignment 1;
	//Locates the position of the next zero in a byte array;
	private int getNextVal(int compareTo, int start, byte[] input){
		for(int i = start; i < input.length; i++){
			if(input[i] == compareTo) return i; }
		return -1; }
	//Extracts an array of bytes from a packet;
	private byte[] byteExtract(byte[] input, int size, int start){
		if(size <= 0) return null;
		byte[] output = new byte[size];
		for(int i = 0; i < size; i++)  output[i] = input[start + i];
		return output; }
	//Inserts a byte array into another array at an offset and returns the end pointer;
	private int offsetByteCopy(byte[] result, byte[] input, int offset){
		if((input.length+offset)<result.length){
			for(int i = 0; i < input.length; i++)  result[i+offset] = input[i];
			return offset + input.length; }
		return -1;
	}
	private String strExtract(byte[] input, int size, int start){
		if(size <= 0) return "";
		byte[] output = new byte[size];
		for(int i = 0; i < size; i++) 
			output[i] = input[start + i];
		return new String(output);
	}
	
	//Converts bytes to integer;
	private int b2i(byte[] b){ 
		byte[] bytes = new byte[4];
		for(int i = 0; (i<4 && i<b.length);i++){
			bytes[3-i] = b[b.length-1-i];
		}
		ByteBuffer wrapped = ByteBuffer.wrap(bytes);
		return wrapped.getInt();
	}
	//Converts integer to bytes;
	private byte[] i2b(int integer,int size)   { 
		ByteBuffer dbuf = ByteBuffer.allocate(4);
		dbuf.putInt(integer);
		byte[] bytes = dbuf.array();
		byte[] result = new byte[size];
		for(int i = 0; (i<4 && i<size);i++){
			result[size-i-1] = bytes[3-i];
		}
		return result;
	}
	private String Req2Str(){
		switch (Req){
		case 1 : return "RRQ"  ;  case 2 : return "WRQ";
		case 3 : return "DATA" ;  case 4 : return "ACK";
		case 5 : return "ERROR";} return "UNDEFINED";
	}
}
