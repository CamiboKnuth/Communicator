import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class CallMakerThread extends Thread {

	//flag for determining state of thread
	private volatile int callFlag;

	//flag values for thread state
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	private String recipientAddress;
	private String recipientNumber;
	private int recipientPort;

	
	private Socket sendSocket;
	private DatagramSocket datagramSocket;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private DataReceiver receiver;
	private DataSender sender;


	public CallMakerThread(
	DatagramSocket datagramSocket,
	InetSocketAddress address,
	String recipientNumber) {

		recipientAddress = address.getAddress().getHostAddress();
		recipientPort = address.getPort();
		this.recipientNumber = recipientNumber;
		
		this.datagramSocket = datagramSocket;
		sendSocket = null;
		receiver = null;
		sender = null;

		callFlag = DEFAULT_FLAG;
	}
	
	//set thread flag to closed, then close streams and sockets
	public void closeThread() throws IOException {
		callFlag = CLOSE_FLAG;
		
		if(sendSocket != null) {
			try {
				sendSocket.close();
				sendSocket = null;
			} catch (IOException ioex) {
				System.out.println("ERROR: " + ioex.getMessage());
			}
		}
		
		if(inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
		
		if(outputStream != null) {
			outputStream.close();
			outputStream = null;
		}
		
		if(receiver != null) {
			receiver.closeThread();
			receiver = null;
		}
		
		if(sender != null) {
			sender.closeThread();
			sender = null;
		}
	}
	
	public void run() {
		
		//loop until closed
		while(callFlag != CLOSE_FLAG) {
			try {
		
				//wait to connect to other user
				sendSocket = new Socket(recipientAddress, recipientPort);
				
				//set socket timeout to 2 seconds
				sendSocket.setSoTimeout(2*1000);
				
				inputStream = new DataInputStream(sendSocket.getInputStream());
				outputStream = new DataOutputStream(sendSocket.getOutputStream());

				//send instance number so the other user can display
				outputStream.writeUTF(Main.instanceNumber);
				
				//wait for other user to accept or reject
				String in = inputStream.readUTF();
				
				try {
					while(in.equals("WAIT")) {
						in = inputStream.readUTF();
					}
				
				} catch (SocketTimeoutException stex) {
					System.out.println("SOCKET TIMEOUT");
				} catch (ConnectException connex) {
					System.out.println("CONNECT EXCEPTION");
				} catch (SocketException sx) {
					System.out.println("SOCKET EXCEPTION");
				} catch (IOException ioex) {
					System.out.println("IOEXCEPTION");
				}
				
				
				System.out.println("received: " + in);
				
				//if other user accepted call
				if(in.equals("ACC")) {
					
					//show in call screen on main view
					Platform.runLater(()->{
						Main.showInCallScreen(this.recipientNumber);
					});	
					
					//non-threaded sender
					sender = new DataSender(datagramSocket, this.recipientNumber);
					//threaded receiver
					receiver = new DataReceiver(datagramSocket);
					
					//begin receiving in threaded loop
					receiver.start();
					
					//begin sending in non-threaded loop
					sender.send();

				//if other user rejected call
				} else if (in.equals("REJ")){
					
					//show that call was rejected
					Platform.runLater(()->{
						Main.showDefaultScreen("Call was rejected.");
					});
				//if other user didn't respond
				} else if (in.equals("STOP")) {
					//show that call timed out
					Platform.runLater(()->{
						Main.showDefaultScreen("Call timed out.");
					});	
				//if this user cancelled the call
				} else if (callFlag == CLOSE_FLAG){
					
				} else {
					//show that call failed
					Platform.runLater(()->{
						Main.showDefaultScreen("Call failed due to disconnection.");
					});					
				}
			
			} catch (IOException ioex) {
				if(callFlag != CLOSE_FLAG) {
					System.out.println("CALLMAKER EXCEPTION OCCURRED:\n" + ioex.getMessage());
					ioex.printStackTrace();
					
					Platform.runLater(()->{
						Main.showDefaultScreen("Call failed: connection not made.");
					});
					
				}
			}

			//close thread
			try {
				this.closeThread();
			} catch (IOException ioex) {
				System.out.println("ERROR: CALLMAKER CLOSE FAILED: " + ioex.getMessage());
			}
		}
	}
}