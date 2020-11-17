import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.InterruptedException;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class CallHandlerThread extends Thread {
	
	//flag for determining state of thread
	private volatile int callFlag;

	//flag values for state of CallHandlerThread
	private final int DEFAULT_FLAG = 0;
	private final int ACCEPT_FLAG = 1;
	private final int REJECT_FLAG = 2;
	private final int CLOSE_FLAG = 3;
	private final int TIMEOUT_FLAG = 4;
	
	//number of seconds to ring for before cancelling call
	private final int RING_TIME_SECONDS = 15;
	
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private volatile Socket receiverSocket;
	
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private DataReceiver receiver;
	private DataSender sender;


	public CallHandlerThread(ServerSocket serverSocket, DatagramSocket datagramSocket) {
		
		this.serverSocket = serverSocket;
		this.datagramSocket = datagramSocket;
		
		receiver = null;
		sender = null;
		receiverSocket = null;

		callFlag = DEFAULT_FLAG;
	}
	
	public void acceptCall() {
		callFlag = ACCEPT_FLAG;
	}
	
	public void rejectCall() {
		callFlag = REJECT_FLAG;
	}
	
	//set thread flag to closed, then close streams and sockets
	public void closeThread() throws IOException {
		callFlag = CLOSE_FLAG;
		
		if(receiverSocket != null) {
			receiverSocket.close();
		}
		
		if(outputStream != null) {
			outputStream.close();
		}
		
		if(inputStream != null) {
			inputStream.close();
		}
		
		if(sender != null) {
			sender.closeThread();
		}
		
		if(receiver != null) {
			receiver.closeThread();
		}
	}
	
	public void run() {
		
		//loop until closed
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait for connection to be accepted (call made by another user)
				receiverSocket = serverSocket.accept();
				String inNum = "";
				
				System.out.println("receiving call...");
				
		
				//ensure flag is still default after accepting from serverSocket
				if (callFlag == DEFAULT_FLAG) {
					
					outputStream = new DataOutputStream(receiverSocket.getOutputStream());
					inputStream = new DataInputStream(receiverSocket.getInputStream());
					
					//receive other user's instance number
					inNum = inputStream.readUTF();
					
					//variables in lambda must be final
					final String number = inNum;
					
					//show options to user
					Platform.runLater(()->{
						Main.showReceivingScreen(number);
					});
				//if flag is not default after accepting from serverSocket, error has occured
				} else {
					closeThread();
				}
				
				
				//number of times "WAIT" has been sent to other user
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
							//send stop message to other user if timeout
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
					//send accept message to other user
					outputStream.writeUTF("ACC");
					
					//variables in lambda must be final
					final String number = inNum;
					
					//show in call screen on main view
					Platform.runLater(()->{
						Main.showInCallScreen(number);
					});
					
					//non-threaded receiver
					receiver = new DataReceiver(datagramSocket);
					//threaded sender
					sender = new DataSender(datagramSocket, number);
					
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
				if (!receiverSocket.isClosed()) {
					receiverSocket.close();
				}
				
				if(inputStream != null) {
					inputStream.close();
				}
				
				if(outputStream != null) {
					outputStream.close();
				}

			} catch (SocketTimeoutException stex) {

			} catch (IOException ioex) {
				System.out.println("CAllHANDLER EXCEPTION OCCURRED:\n"
					+ ioex.getMessage());
					
				//if call flag is not closed when error occurs, call has disconnected
				if (callFlag != CLOSE_FLAG) {
					
					callFlag = DEFAULT_FLAG;
					
					//show default screen on main view
					Platform.runLater(()->{
						Main.showDefaultScreen("Call disconnected.");
					});					
				}
			}
		}
	}
}