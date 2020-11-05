import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class CallHandlerThread extends Thread {
	
	//private int instanceNumber = 0;
	private int bindPort = 9990;
	
	private volatile int callFlag;

	//flags for thread state
	private final int DEFAULT_FLAG = 0;
	private final int ACCEPT_FLAG = 1;
	private final int REJECT_FLAG = 2;
	private final int CLOSE_FLAG = 3;
	
	private ServerSocket serverSocket;
	private Socket receiverSocket;
	
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private DataReceiver receiver;
	private DataSender sender;


	public CallHandlerThread() {
		
		receiver = null;
		sender = null;
		
		try {
			
			//try to bind serverSocket.
			//try { 
				serverSocket = new ServerSocket(bindPort);
				
			//if bind fails, bind to next port
			//} catch (BindException bex) {
			//	instanceNumber = 1;
			//	bindPort  = 9991;
			//	serverSocket = new ServerSocket(bindPort);
			//}
		
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
		
		if(inputStream != null) {
			inputStream.close();
		}
		
		if(sender != null) {
			sender.closeThread();
		}
		
		if(serverSocket != null) {
			serverSocket.close();
		}
		
		if(receiverSocket != null) {
			receiverSocket.close();
		}
	}
	
	public boolean isRunning() {
		return (callFlag != CLOSE_FLAG);
	}
	
	public void run() {
		
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait for connection to be accepted
				receiverSocket = serverSocket.accept();
				outputStream = new DataOutputStream(receiverSocket.getOutputStream());
				inputStream = null;
				
				//show options to user
				Platform.runLater(()->{
					Main.showReceivingScreen(receiverSocket.getRemoteSocketAddress().toString());
				});
				
				
				//wait for user to make a decision about connection
				while(callFlag == DEFAULT_FLAG);
				
				//if this user accepts the call
				if(callFlag == ACCEPT_FLAG) {
					outputStream.writeUTF("ACC");
					
					//show in call screen on main view
					Platform.runLater(()->{
						Main.showInCallScreen(receiverSocket.getRemoteSocketAddress().toString());
					});
					
					inputStream = new DataInputStream(receiverSocket.getInputStream());
					
					receiverSocket.setSoTimeout(2 * 1000);
	
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
						Main.showDefaultScreen();
					});
					
					callFlag = DEFAULT_FLAG;
				}
				
				outputStream.close();
				receiverSocket.close();
				
				if(inputStream != null) {
					inputStream.close();
				}

			} catch (SocketException sx) {
				try {
					outputStream.close();
					receiverSocket.close();
					
					if(sender != null) {
						sender.closeThread();
					}
					
					if(inputStream != null) {
						inputStream.close();
					}
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
				System.out.println("connection ended");
				
				Platform.runLater(()->{
					Main.showDefaultScreen();
				});	
				
				
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}
}