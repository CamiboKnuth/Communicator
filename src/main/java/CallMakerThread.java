import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;

public class CallMakerThread extends Thread {
	
	private volatile int callFlag;

	//flags for thread state
	final int DEFAULT_FLAG = 0;
	final int CLOSE_FLAG = 1;
	
	private String ipAddress;

	
	private Socket sendSocket;
	private DataInputStream inputStream;
	//private DataOutputStream outputStream;


	public CallMakerThread(String ip) {
		ipAddress = ip;
		
		sendSocket = null;

		callFlag = DEFAULT_FLAG;
	}
	
	public void closeThread() throws IOException {
		callFlag = CLOSE_FLAG;
		
		if(inputStream != null) {
			inputStream.close();
		}
		
		if(sendSocket != null) {
			sendSocket.close();
		}
	}
	
	public void run() {
		
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait for connection to be accepted
				sendSocket = new Socket(ipAddress, 9991);
				inputStream = new DataInputStream(sendSocket.getInputStream());
				
				String in = inputStream.readUTF(); 
				
				
				System.out.println("received: " + in);
				
				inputStream.close();
				sendSocket.close();
				
				callFlag = CLOSE_FLAG;
			/*	
				//accept call
				if(callFlag == ACCEPT_FLAG) {
					//TODO: initiate call
					
					callFlag = DEFAULT_FLAG;
				//reject call
				} else if (callFlag == REJECT_FLAG){
					//TODO: send rejection message to other user
					closeThread();
				}
			*/
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}
}