import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.IOException;

import java.io.EOFException;
import java.net.SocketException;


public class DataReceiver extends Thread {
	
	//flag for determining state of thread
	private volatile int callFlag;
	
	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	private DataInputStream inputStream;
	
	public DataReceiver(DataInputStream stream) {
		inputStream = stream;
		callFlag = DEFAULT_FLAG;
	}


	public void closeThread() {
		callFlag = CLOSE_FLAG;
		
		try {
			inputStream.close();
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}	
	}

	//run reception loop in original thread
	public void receive() {
		
		//handle call
		while(callFlag != CLOSE_FLAG) {
			try {
				String in = inputStream.readUTF();
				
				System.out.println("received: " + in);
				
			} catch (IOException ioex) {
				System.out.println("RECEIVE EXCEPTION OCCURRED:\n" + ioex.getMessage());
				callFlag = CLOSE_FLAG;
				
				//end the call
				Platform.runLater(()->{
					Main.endCallAction();
				});
			}
		}
	}
	
	//run reception loop in separate thread
	public void run() {
		this.receive();
	}
}