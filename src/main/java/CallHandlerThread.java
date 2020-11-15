import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.InterruptedException;


import java.net.BindException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


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
	private volatile Socket receiverSocket;
	
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private DataReceiver receiver;
	private DataSender sender;


	public CallHandlerThread() {
		
		try {
			//try to bind serverSocket.
			boolean bound = false;
			//loop until bind succeeds
			while(!bound) {
				try { 
					serverSocket = new ServerSocket(Main.bindPort);
					bound = true;
					
				//if bind fails, bind to next port
				} catch (BindException bex) {
					Main.bindPort ++;
				}
			}
		} catch (IOException ioex) {
			System.out.println("ERROR: SERVER BIND FAILED");
			ioex.printStackTrace();
		}
		
		receiver = null;
		sender = null;
		receiverSocket = null;
		
		//local system ip to be indicated to user
		String ip = "127.0.0.1";
		try {
			ip = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException uhex) {
			uhex.printStackTrace();
		}
		
		//string must be final to be used in lambda expression below
		final String finalIp = ip;
		
		//show address to user
		Platform.runLater(()->{
			Main.setDisplayIp(finalIp
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
	
	//set thread flag to closed, then close streams and sockets
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
		
		if (serverSocket != null) {
			serverSocket.close();
		}
	}
	
	public void run() {
		
		//loop until closed
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait for connection to be accepted (call made by another user)
				System.out.println("waiting for call");
				receiverSocket = serverSocket.accept();
				
		
				//ensure flag is still default after accepting from serverSocket
				if (callFlag == DEFAULT_FLAG) {
					
					outputStream = new DataOutputStream(receiverSocket.getOutputStream());
					inputStream = null;
					
					//show options to user
					Platform.runLater(()->{
						Main.showReceivingScreen(receiverSocket.getRemoteSocketAddress().toString());
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
				if (!receiverSocket.isClosed()) {
					receiverSocket.close();
				}
				
				if(inputStream != null) {
					inputStream.close();
				}
				
				if(outputStream != null) {
					outputStream.close();
				}

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