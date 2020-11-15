import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpTools {
	
	private static long powerRaise(int base, int exponent) {
		//default result will be zero
		long total = 1;
		
		//multiply total by base and add to total
		//a number of times determined by exponent
		for (int i = 0; i < exponent; i++) {
			total = total * base;
		}
		
		return total;
	}
			
	private static long binToDec(String binString) {
		long total = 0;

		//reverse the input string
		binString = new StringBuilder(binString).reverse().toString();
		
		//for each number in the reversed binary string
		for (int i = 0; i < binString.length(); i++) {
			//add to the total that value times 2 to the power of the index it is in
			total += Long.valueOf(binString.substring(i, i+1)) * powerRaise(2, i);
		}
		
		return total;
	}

	private static String decToBin(long decNum) {
		String binString = "";
		
		if (decNum == 0) {
			binString = "0";
		}
		
		//repeatedly divide by two and take remainder, building binary
		//string backwards
		while (decNum > 0) {
			binString += Long.toString(decNum % 2);
			decNum = decNum / 2;
		}
		
		//return reverse of what was found
		return new StringBuilder(binString).reverse().toString();
	}

	public static String addressAndPortToNumber(String ip, int port) {
		//split ip address into each of the four numbers
		String[] listOfNums = ip.split("\\.");
		StringBuilder binString = new StringBuilder("");
		
		//convert each number in the address to binary
		for(int i = 0; i < listOfNums.length; i++) {
			StringBuilder currentBinary =
				new StringBuilder(decToBin(Long.valueOf(listOfNums[i])));
				
			//flip string builder to append to end rather than beginning
			currentBinary.reverse();
			
			//add zeros to beginning of binary until 8 bits
			while(currentBinary.length() < 8) {
				currentBinary.append("0");
			}
			
			//append the 8 bit binary strings together
			binString.append(currentBinary.reverse());
		}
		
		//convert port to a binary string representing instance
		//number of this program. Since each computer can only
		//have 16 instances at max, portString goes from 0000 to 1111
		String portString = decToBin(port - Main.BASE_PORT);
		
		//if port is not 4 bits, add 0s to beginning until it is
		while(portString.length() < 4) {
			portString = "0" + portString;
		}
		
		//append portString to ip string
		binString.append(portString);
		
		//for adding dashes to number, convert to decimal representation, then string
		StringBuilder dashAdder =
			new StringBuilder(Long.toString(binToDec(binString.toString())));
			

		//add zeros to beginning of number until 11 digits
		dashAdder.reverse();
		while(dashAdder.length() < 11) {
			dashAdder.append("0");
		}		
		dashAdder.reverse();
		
		//add dashes to number
		dashAdder.insert(1,"-");
		dashAdder.insert(5,"-");
		dashAdder.insert(9,"-");
		
		return dashAdder.toString();		
	}
	
	public static InetSocketAddress numberToInetSocketAddress(String num) {
		//remove 3 dashes from number
		StringBuilder dashRemover = new StringBuilder(num);
		dashRemover.deleteCharAt(9);
		dashRemover.deleteCharAt(5);
		dashRemover.deleteCharAt(1);	
		
		//convert number without dashes to binary string
		StringBuilder binString =
			new StringBuilder(decToBin(Long.valueOf(dashRemover.toString())));
		
		//flip string builder to append to end rather than beginning
		binString.reverse();
		
		//append zeros to beginning of binary string til length is 36
		while (binString.length() < 36) {
			binString.append("0");
		}
		//undo earlier flip
		binString.reverse();
		

		//4 bits at end represent instance number, which in turen represents port
		String instanceString = binString.substring(binString.length() - 4, binString.length());
		//take the first 32 bits, which are ip address
		binString.delete(binString.length() - 4, binString.length());
		
		
		StringBuilder finalIp = new StringBuilder("");
		//separate 32 bit address into 4 chunks of 8 bits, convert each to numbers
		//with decimals between
		for (int i = 0; i < 4; i++) {
			finalIp.append(Long.toString(binToDec(binString.substring(8*i,8*(i+1)))) + ".");
		}
		
		//remove final decimal point from ip
		finalIp.deleteCharAt(finalIp.length() - 1);
		//get port by adding instance to base port
		int port = ((int) binToDec(instanceString)) + Main.BASE_PORT;
		

		//create and return address based on ip and port
		InetSocketAddress toReturn = new InetSocketAddress(finalIp.toString(),port);
		
		return toReturn;
	}
	
	public static boolean isOwnAddress(InetSocketAddress toTest) {
		//default to assuming address is not own
		boolean isOwn = false;
		
		String thisHost = "";
		
		//try to set thisHost to the ip address of the local system
		try {
			thisHost = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException uhex) {
			System.out.println("LOCALHOST ERROR: " + uhex.getMessage());
		}
		
		//separate address and port of argument
		String ipToTest = toTest.getAddress().getHostAddress();
		int portToTest = toTest.getPort();
	
		//If ip is this host's IP
		if (ipToTest.equals(thisHost) || ipToTest.equals("127.0.0.1")
		|| ipToTest.equals("localhost")) {

			//if port of argument is same is this system's bind port
			if (portToTest == Main.bindPort) {
				isOwn = true;
			}
		}
		
		return isOwn;
	}
	
	public static boolean isValidIp(String ipToTest) {
		//regex for ip address from Regular Expressions Cookbook by Oreilly
		String ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
			+ "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
			
		//search for regex pattern in ip argument
		Pattern regex = Pattern.compile(ipPattern);
		Matcher matcher = regex.matcher(ipToTest);
		
		return matcher.find();
	}
	
	public static boolean isValidNumber(String toTest) {
		//regex for numbers to call
		String numPattern = "^\\d-\\d{3}-\\d{3}-\\d{4}$";
			
		//search for regex pattern in argument
		Pattern regex = Pattern.compile(numPattern);
		Matcher matcher = regex.matcher(toTest);
		
		return matcher.find();		
	}
}