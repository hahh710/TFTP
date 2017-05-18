import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
	public static void main(String args[]){
		Scanner in = new Scanner(System.in);
		
		System.out.print("System is located locally at:");
		try { System.out.println(InetAddress.getLocalHost().getHostAddress()); }
		catch (UnknownHostException e) { }
		
		boolean verbose;
		int     mode;
		
		while(true){
			System.out.println("Would you like to run it in verbose mode (Y/N)?");
			String input = in.nextLine();
			if(input.toUpperCase().equals("Y")){ verbose=true; break;}
			if(input.toUpperCase().equals("N")){ verbose=false; break;}
			System.out.println("Invalid Mode! Select either 'Y'(Yes), 'N'(No)");
		}
		while(true){
			System.out.println("Please select which parts of the system you wish to run:");
			System.out.println("    0 - Client");
			System.out.println("    1 - Server");
			System.out.println("    2 - Client and Server");
			System.out.println("    3 - Client with Error Simulation");
			System.out.println("    4 - Client and Server with Error Simulation");
			int input = in.nextInt();
			if(input >= 0 && input <= 4){ mode = input; break; }
			System.out.println("Invalid option! Please try again.\n");
		}
	}
}
