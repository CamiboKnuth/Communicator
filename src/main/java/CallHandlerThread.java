import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.InterruptedException;


import java.net.ServerSocket;
import java.net.Socket;


public class CallHandlerThread extends Thread {
	
	//flag for determining state of thread
	private volatile int callFlag;

	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int ACCEPT_FLAG = 1;
	private final int REJECT_FLAG = 2;
	private final int CLOSE_FLAG = 3;
	private final int TIMEOUT_FLAG = 4;
	
	//number of seconds to ring for before cancelling call
	private final int RING_TIME_SECONDS = 15;
	
	private ServerSocket serverSocket;
	private volatile Socket receiverSocket;
	
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private DataReceiver receiver;
	private DataSender sender;


	public CallHandlerThread(ServerSocket socket) {
		
		this.serverSocket = socket;
		
		receiver = null;
		sender = null;
		receiverSocket = null;
		
		//show address to user
		Platform.runLater(()->{
			Main.setDisplayIp("127.0.0.1"
				+ ":" + Integer.toString(serverSocket.getLocalPort()));
		});
		
		callFlag = DEFAULT_FLAG;
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
		
		if(inputStream != null) {
			inputStream.close();
		}
		
		if(sender != null) {
			sender.closeThread();
		}
		
		if(receiverSocket != null) {
			receiverSocket.close();
		}
	}
	
	public boolean isClosed() {
		return (callFlag == CLOSE_FLAG);
	}
	
	public void run() {
		
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait for connection to be accepted
				System.out.println("waiting for call");
				receiverSocket = serverSocket.accept();
				outputStream = new DataOutputStream(receiverSocket.getOutputStream());
				inputStream = null;
				
				//show options to user
				Platform.runLater(()->{
					Main.showReceivingScreen(receiverSocket.getRemoteSocketAddress().toString());
				});
				
				
				//number of times "WAIT" has been sent
				int times = 0;
				//wait for this user to make a decision about connection
				while(callFlag == DEFAULT_FLAG) {
					try {
						//repeatedly send "WAIT" to caller
						outputStream.writeUTF("WAIT");
						//hold for 250 milliseconds
						Thread.sleep(500);
						times ++;
						
						//If too much time passes, timeout
						if (times > RING_TIME_SECONDS * 2) {
							callFlag = TIMEOUT_FLAG;
							//send stop message to other user
							outputStream.writeUTF("STOP");
						}
					//if an exception occurs, timeout
					} catch (IOException ioex) {
						callFlag = TIMEOUT_FLAG;
					} catch (InterruptedException intex) {
						callFlag = TIMEOUT_FLAG;
					}
				}
				
				//if this user accepts the call
				if(callFlag == ACCEPT_FLAG) {
					outputStream.writeUTF("ACC");
					
					//show in call screen on main view
					Platform.runLater(()->{
						Main.showInCallScreen(receiverSocket.getRemoteSocketAddress().toString());
					});
					
					inputStream = new DataInputStream(receiverSocket.getInputStream());
					
					//static receiver
					receiver = new DataReceiver(inputStream);
					//threaded sender
					sender = new DataSender(outputStream);
					
					//begin sending in threaded loop
					sender.start();
					
					//begin receiving in non-threaded loop
					receiver.receive();					
				//if this user rejects the call
				} else if (callFlag == REJECT_FLAG){
					outputStream.writeUTF("REJ");
					
					//show default screen on main view
					Platform.runLater(()->{
						Main.showDefaultScreen("");
					});
					
					callFlag = DEFAULT_FLAG;
				} else if (callFlag == TIMEOUT_FLAG) {
					//show default screen on main view
					Platform.runLater(()->{
						Main.showDefaultScreen("Call timed out");
					});
					
					callFlag = DEFAULT_FLAG;					
				}
				
				//close streams and receiver socket
				outputStream.close();
				
				if (!receiverSocket.isClosed()) {
					receiverSocket.close();
				}
				
				if(inputStream != null) {
					inputStream.close();
				}

			} catch (IOException ioex) {
				System.out.println("CAllHANDLER EXCEPTION OCCURRED:\n" + ioex.getMessage());
			}
		}
	}
}