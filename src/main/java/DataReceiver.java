import java.io.DataInputStream;
import java.io.IOException;


public class DataReceiver extends Thread {
	
	private volatile int callFlag;
	
	private DataInputStream inputStream;
	
	//flags for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
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

	public void receive() {
		
		//handle call
		while(callFlag != CLOSE_FLAG) {
			try {
				String in = inputStream.readUTF();
				
				System.out.println("received: " + in);
				
			} catch (IOException ioex) {
				ioex.printStackTrace();
				callFlag = CLOSE_FLAG;
			}	
		}
	}
	
	public void run() {
		this.receive();
	}
}