import javafx.application.Platform;

import java.io.DataOutputStream;
import java.io.IOException;

import java.io.EOFException;
import java.net.SocketException;


public class DataSender extends Thread {
	
	//flag for determining state of thread
	private volatile int callFlag;
	
	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	private DataOutputStream outputStream;
	
	public DataSender(DataOutputStream stream) {
		outputStream = stream;
		callFlag = DEFAULT_FLAG;
	}

	public void closeThread() {
		callFlag = CLOSE_FLAG;
		
		try {
			outputStream.close();
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}	
	}

	//run sending loop in original thread
	public void send() {
		
		//handle call
		while(callFlag != CLOSE_FLAG) {
			try {
				outputStream.writeUTF("SENDDATA");
				
				System.out.println("sending: " + "SENDDATA");
				
			} catch (IOException ioex) {
				System.out.println("SEND EXCEPTION OCCURRED:\n" + ioex.getMessage());
				callFlag = CLOSE_FLAG;
			}	
		}
	}
	
	//run sending loop in separate thread
	public void run() {
		this.send();
	}
}