import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;

public class CallHandlerThread extends Thread {
	
	private volatile int callFlag;

	//flags for thread state
	final int DEFAULT_FLAG = 0;
	final int ACCEPT_FLAG = 1;
	final int REJECT_FLAG = 2;
	final int CLOSE_FLAG = 3;
	
	private ServerSocket serverSocket;
	private Socket receiverSocket;
	
	//private DataInputStream inputStream;
	private DataOutputStream outputStream;


	public CallHandlerThread() {
		try {
			serverSocket = new ServerSocket(9991);
			receiverSocket = null;
			callFlag = DEFAULT_FLAG;
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
	
	public void acceptCall() {
		callFlag = ACCEPT_FLAG;
	}
	
	public void rejectCall() {
		callFlag = REJECT_FLAG;
	}
	
	public void closeThread() throws IOException {
		callFlag = CLOSE_FLAG;
		
		if(outputStream != null) {
			outputStream.close();
		}
		
		if(serverSocket != null) {
			serverSocket.close();
		}
		
		if(receiverSocket != null) {
			receiverSocket.close();
		}
	}
	
	public void run() {
		
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait for connection to be accepted
				receiverSocket = serverSocket.accept();
				//inputStream = new DataInputStream(receiverSocket.getInputStream());
				outputStream = new DataOutputStream(receiverSocket.getOutputStream());
				
				//show options to user
				Platform.runLater(()->{
					Main.showReceivingScreen(receiverSocket.getRemoteSocketAddress().toString());
				});
				
				
				//wait for user to make a decision about connection
				while(callFlag == DEFAULT_FLAG);
				
				//accept call
				if(callFlag == ACCEPT_FLAG) {
					outputStream.writeUTF("ACC");
					
					outputStream.close();
					receiverSocket.close();
					
					//TODO: handle call
					
					callFlag = DEFAULT_FLAG;
				//reject call
				} else if (callFlag == REJECT_FLAG){
					outputStream.writeUTF("REJ");

					outputStream.close();
					receiverSocket.close();
					
					callFlag = DEFAULT_FLAG;
				}

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}
}