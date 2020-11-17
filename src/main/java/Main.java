import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
TODO: Use UDP instead of TCP? Use socket timeout to end?
TODO: Encryption?
TODO: Video, change scene size?
*/


public class Main extends Application {
	
	//starting point for binding server socket
	final static int BASE_PORT = 9900;
	
	//actual value to which server socket will be bound
	static int bindPort;
	
	//socket for receiving calls
	static ServerSocket serverSocket;
	
	//phone number for this instance of the program
	static String instanceNumber;
	
	
	//thread with socket for receiving calls
	static CallHandlerThread callHandler;
	static CallMakerThread callMaker;

	//pane for showing nodes on screen
	static GridPane gridPane;
	

	//default screen nodes
	static Label messageBanner;
	static Label yourIp;
	static Button callButton;
	static Label ipCallMessage;
	static TextField ipCallRecipient;
	
	//calling screen node
	static Label callingIpLabel;
	static Button cancelCallButton;
	
	//receiving call screen
	static Label receivingCallFromIP;
	static Button acceptCallButton;
	static Button rejectCallButton;
	
	//in call node
	static Label inCallWithIp;
	static Button endCallButton;


	public static void main(String[] args) {	
		launch(args);
	}
	
	@Override
	public void start(Stage stage) {
		//set title of the window
		stage.setTitle("Communicator");
		
		//create base and grid for adding elements
		StackPane base = new StackPane();
        gridPane = new GridPane();

		//set alignment of grid
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		gridPane.setAlignment(Pos.CENTER);		
				
		//default screen nodes init
		messageBanner = new Label("");
		messageBanner.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		yourIp = new Label("");
		yourIp.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");

	
		
		//make call screen nodes init
		ipCallMessage = new Label("Enter number to call:");
		ipCallMessage.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		ipCallRecipient = new TextField();
		ipCallRecipient.setPrefWidth(180);
		ipCallRecipient.setMaxWidth(180);
		ipCallRecipient.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold; -fx-color: #555555;");
		
		callButton = new Button("Call");
		callButton.setMinSize(120, 60);
		callButton.setMaxSize(120, 60);
		callButton.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

		
		//calling screen node init
		callingIpLabel = new Label("Calling:");
		callingIpLabel.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		cancelCallButton = new Button("Cancel");
		cancelCallButton.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

		
		//receiving call screen init
		receivingCallFromIP = new Label("Receiving call from:");
		receivingCallFromIP.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		acceptCallButton = new Button("Accept Call");
		acceptCallButton.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
			
		rejectCallButton = new Button("Reject Call");
		rejectCallButton.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
		
		
		//in call screen node init
		inCallWithIp = new Label("Currently in call with:");
		inCallWithIp.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		endCallButton = new Button("End Call");
		endCallButton.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
		
		
		//set actions for the buttons
		initButtonActions();
		
		//add grid to base of scene
		base.getChildren().add(gridPane);
		//create scene with addon, width, height
		Scene scene = new Scene(base, 350, 550);
		
		//set and show the scene
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
		
		//determine what happens on window close
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent we) {
				try {
					//close callHandlerThread
					if(callHandler != null) {
						callHandler.closeThread();
						callHandler = null;
					}
					
					//close callMakerThread
					if(callMaker != null) {
						callMaker.closeThread();
						callMaker = null;
					}
					
					serverSocket.close();
					
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
				
				//exit system
				Platform.exit();
				System.exit(0);
			}
		});
		
		
		//section for binding server socket to a port
		//socket stays bound for the duration of execution
		try {
			boolean bound = false;
			//start binding attempts at base port
			bindPort = BASE_PORT;

			//loop until bound or if attempts surpass 16
			while(!bound && bindPort < BASE_PORT + 16) {
				//try to bind serverSocket.
				try { 
					serverSocket = new ServerSocket(bindPort);
					serverSocket.setSoTimeout(100);
					bound = true;

				//if bind fails, bind to next port
				} catch (BindException bex) {
					bindPort ++;
				}
			}
			
			
			if(!bound) {
				System.out.println("SERVER BIND FAILED, CLOSE OTHER INSTANCES.");
			}
			
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		//callHandler and Maker start null
		callHandler = null;
		callMaker = null;
		
		//show users number on screen
		setDisplayIp();
		
		//start off at default screen
		showDefaultScreen("");
	}
	
	public static void initButtonActions() {
				
		//set action for "call" button
		callButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				
				//get recipient number from user text field
				String recipient = ipCallRecipient.getText();
				
				if(IpTools.isValidNumber(recipient)) {

					//convert number to address
					InetSocketAddress recipientAddress =
						IpTools.numberToInetSocketAddress(recipient);
					
					//if call recipient is not this instance
					if (!IpTools.isOwnAddress(recipientAddress)) {
						
						try {
							//close callHandler temporarily
							callHandler.closeThread();
							callHandler.join();
							callHandler = null;
							
							//start callMaker to contact recipient
							callMaker = new CallMakerThread(recipientAddress, recipient);
							callMaker.start();

							//show call in process screen
							showCallingScreen();

						} catch (IOException ioex) {
							ioex.printStackTrace();
							showDefaultScreen("Call Failed.");
						} catch (InterruptedException intex) {
							intex.printStackTrace();
							showDefaultScreen("");
						}
						
					} else {
						ipCallMessage.setText("Cannot call yourself, try again:");
					}
				} else {
					ipCallMessage.setText("Not a valid number, try again:");
				}
			}
		});
		
		//set action for "cancel" button
		cancelCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				//shut down callMaker thread
				try {
					callMaker.closeThread();
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
				callMaker = null;
				
				//return to default screen
				showDefaultScreen("Call Cancelled");
			}
		});
		
		
		//set action for "accept call" button
		acceptCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				if(callHandler != null) {
					//accept call and go to in call screen
					callHandler.acceptCall();
				} else {
					showDefaultScreen("Accept failed.");
				}
			}
		});
		
		//set action for "reject call" button
		rejectCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				if(callHandler != null) {
					//reject call and return to default screen
					callHandler.rejectCall();
				} else {
					showDefaultScreen("");
				}
			}
		});
		
		//set action for "end call" button
		endCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				//carried out in separate method so other threads can call
				endCallAction();
			}
		});
	}
	
	public static void endCallAction() {
		//shut down callMaker thread
		if(callMaker != null) {
			try {
				callMaker.closeThread();
				callMaker.join();
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}  catch (InterruptedException intex) {
				intex.printStackTrace();
			}
		}
		
		callMaker = null;
		
		//shut down callHandler thread
		if(callHandler != null) {
			try {
				callHandler.closeThread();
				callHandler.join();
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}  catch (InterruptedException intex) {
				intex.printStackTrace();
			}
		}
		
		callHandler = null;
		
		//return to default screen
		showDefaultScreen("Call Ended");		
	}
	
	public static void setDisplayIp() {
		
		//local system ip to be indicated to user
		String address = "127.0.0.1";
		try {
			address = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException uhex) {
			uhex.printStackTrace();
		}

		//get number from ip and port
		instanceNumber = IpTools.addressAndPortToNumber(address, bindPort);
		
		//Indicate ip to user
		yourIp.setText("Your number is:\n" + instanceNumber);
	}
	
	public static void showDefaultScreen(String message) {
		
		//close any open callMakerThread
		if(callMaker != null) {
			try {
				callMaker.closeThread();
				callMaker.join();
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}  catch (InterruptedException intex) {
				intex.printStackTrace();
			}
			callMaker = null;
		}

		//close any open callHandlerThread
		if(callHandler != null) {
			try {
				callHandler.closeThread();
				callHandler.join();
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}  catch (InterruptedException intex) {
				intex.printStackTrace();
			}
			callHandler = null;
		}		

		
		//create and start socket thread for reception of calls
		callHandler = new CallHandlerThread(serverSocket);
		callHandler.start();

		
		//determine what main screen banner will say
		messageBanner.setText(message);
		//determine what will display over number field
		ipCallMessage.setText("Enter number to call:");
		

		//clear grid
		gridPane.getChildren().clear();		
		

		//add default screen elements to center of screen
		gridPane.add(yourIp, 0, 0);	
		gridPane.setHalignment(yourIp, HPos.CENTER);

		gridPane.add(ipCallMessage, 0, 2);
		gridPane.setHalignment(ipCallMessage, HPos.CENTER);
		
		ipCallRecipient.clear();
		gridPane.add(ipCallRecipient, 0, 3);
		gridPane.setHalignment(ipCallRecipient, HPos.CENTER);
		
		gridPane.add(callButton, 0, 4);
		gridPane.setHalignment(callButton, HPos.CENTER);
		
		gridPane.add(messageBanner, 0, 6);
		gridPane.setHalignment(messageBanner, HPos.CENTER);
	}
		
	public static void showCallingScreen() {
		//clear grid
		gridPane.getChildren().clear();
		
		callingIpLabel.setText("Calling: " + ipCallRecipient.getText());
		
		//add calling screen elements to center of screen
		gridPane.add(callingIpLabel, 0, 0);
		gridPane.setHalignment(callingIpLabel, HPos.CENTER);
		
		gridPane.add(cancelCallButton, 0, 4);
		gridPane.setHalignment(cancelCallButton, HPos.CENTER);
	}
	
	public static void showReceivingScreen(String otherNum) {
		//clear grid
		gridPane.getChildren().clear();
		
		receivingCallFromIP.setText("Receiving call from: " + otherNum);
		
		//add receiving call screen elements to center of screen
		gridPane.add(receivingCallFromIP, 0, 0);
		gridPane.setHalignment(receivingCallFromIP, HPos.CENTER);
		
		gridPane.add(acceptCallButton, 0, 1);
		gridPane.setHalignment(acceptCallButton, HPos.CENTER);
		
		gridPane.add(rejectCallButton, 0, 2);
		gridPane.setHalignment(rejectCallButton, HPos.CENTER);		
	}
	
	public static void showInCallScreen(String otherNum) {
		//clear grid
		gridPane.getChildren().clear();
		
		inCallWithIp.setText("Currently in call with: \n" + otherNum);
		
		//add in call screen elements to center of screen
		gridPane.add(inCallWithIp, 0, 0);
		gridPane.setHalignment(inCallWithIp, HPos.CENTER);	
		gridPane.add(endCallButton, 0, 4);
		gridPane.setHalignment(endCallButton, HPos.CENTER);	
	}
}