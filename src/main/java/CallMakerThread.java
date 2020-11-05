import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class CallMakerThread extends Thread {
	
	private int bindPort = 9990;
	
	private volatile int callFlag;

	//flags for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	private String ipAddress;

	
	private Socket sendSocket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private DataReceiver receiver;
	private DataSender sender;


	public CallMakerThread(String ip) {
		ipAddress = ip;
		
		sendSocket = null;
		receiver = null;
		sender = null;

		callFlag = DEFAULT_FLAG;
	}
	
	public void closeThread() throws IOException {
		callFlag = CLOSE_FLAG;
		
		if(inputStream != null) {
			inputStream.close();
		}
		
		if(outputStream != null) {
			outputStream.close();
		}
		
		if(receiver != null) {
			receiver.closeThread();
		}
		
		if(sendSocket != null) {
			sendSocket.close();
		}
	}
	
	public boolean isRunning() {
		return (callFlag != CLOSE_FLAG);
	}
	
	public void run() {
		
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait to connect to other user
				sendSocket = new Socket(ipAddress, bindPort);
				inputStream = new DataInputStream(sendSocket.getInputStream());
				outputStream = null;
				
				//wait for other user to accept or reject
				String in = inputStream.readUTF();
				
				System.out.println("received: " + in);
				
				//if other user accepted call
				if(in.equals("ACC")) {					
					//show in call screen on main view
					Platform.runLater(()->{
						Main.showInCallScreen(sendSocket.getRemoteSocketAddress().toString());
					});	

					outputStream = new DataOutputStream(sendSocket.getOutputStream());
					
					sendSocket.setSoTimeout(2 * 1000);
					
					//handle call
					//static sender
					sender = new DataSender(outputStream);
					//threaded receiver
					receiver = new DataReceiver(inputStream);
					
					//begin receiving in threaded loop
					receiver.start();
					
					//begin sending in non-threaded loop
					sender.send();

				//if other user rejected call
				} else if (in.equals("REJ")){
					
					//show that call was rejected
					Platform.runLater(()->{
						Main.showRejectCallMessage();
					});
				}
				
				closeThread();
			
			} catch (SocketException sx) {
				System.out.println("connection ended");
				
				try {
					closeThread();
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
				
				Platform.runLater(()->{
					Main.showDefaultScreen();
				});	
				
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}
}