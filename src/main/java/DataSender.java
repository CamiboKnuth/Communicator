import java.io.DataOutputStream;
import java.io.IOException;


public class DataSender extends Thread {
	
	private volatile int callFlag;
	
	private DataOutputStream outputStream;
	
	//flags for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
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

	public void send() {
		
		//handle call
		while(callFlag != CLOSE_FLAG) {
			try {
				outputStream.writeUTF("SENDDATA");
				
				System.out.println("sending: " + "SENDDATA");
				
			} catch (IOException ioex) {
				ioex.printStackTrace();
				callFlag = CLOSE_FLAG;
			}	
		}
	}
	
	public void run() {
		this.send();
	}
}