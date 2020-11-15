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

import java.net.Inet4Address;
import java.net.UnknownHostException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
TODO: Determine if first or second instance?
TODO: User regular number rather than IP?
TODO: Combine default and make call screens?
TODO: Use UDP instead of TCP? Use socket timeout to end?
TODO: Encryption?
TODO: Video, change scene size?
*/


public class Main extends Application {
	
	static int bindPort = 9990;
	
	//thread with socket for receiving calls
	static CallHandlerThread callHandler;
	static CallMakerThread callMaker;

	//pane for showing nodes on screen
	static GridPane gridPane;
	
	//universal back button to return to default screen
	static Button backButton;


	//default screen nodes
	static Label messageBanner;
	static Label yourIp;
	static Button makeCallButton;
	
	//make call screen nodes
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
		

		//create back button
		backButton = new Button("Back");
		backButton.setMinWidth(120);
		backButton.setMaxWidth(120);
		backButton.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

		
		//default screen nodes init
		messageBanner = new Label("");
		messageBanner.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		yourIp = new Label("");
		yourIp.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		makeCallButton = new Button("Make Call");
		makeCallButton.setMinSize(120, 60);
		makeCallButton.setMaxSize(120, 60);
		makeCallButton.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: black; -fx-faint-focus-color: transparent;");
	
		
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
		Scene scene = new Scene(base, 400, 600);
		
		//set and show the scene
		stage.setScene(scene);
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
					
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
				
				//exit system
				Platform.exit();
				System.exit(0);
			}
		});
		
		//callHandler and Maker start null
		callHandler = null;
		callMaker = null;
		
		//start off at default screen
		showDefaultScreen("");
	}
	
	public static void initButtonActions() {
		//set action for "back" button
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				showDefaultScreen("");
			}
		});	
		
		//set action for "make call" button
		makeCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				try {
					//close callHandler temporarily
					callHandler.closeThread();
					callHandler = null;
					
					//show options for making call
					showMakeCallScreen();
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		});
		
		//set action for "call" button
		callButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				
				if(isValidIp(ipCallRecipient.getText())) {
					if (!isOwnAddress(ipCallRecipient.getText())) {
						//start callMaker to contact recipient
						callMaker = new CallMakerThread(ipCallRecipient.getText());
						callMaker.start();

						//show call in process screen
						showCallingScreen();
					} else {
						ipCallMessage.setText("Cannot call yourself, try again:");
					}
				} else {
					ipCallMessage.setText("Not a valid number, try again:");
				}
			}
		});
		
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
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
		
		//shut down callHandler thread
		if(callHandler != null) {
			try {
				callHandler.closeThread();
				callHandler = null;
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
		
		//return to default screen
		showDefaultScreen("Call Ended");		
	}
	
	public static void setDisplayIp(String address) {
		//Indicate ip to user
		yourIp.setText("Your number is:\n" + address + "\n\nAwaiting call...");
	}
	
	public static void showDefaultScreen(String message) {
		//open callHandlerThread if null
		if(callHandler == null) {
			//create and start socket thread for reception of calls
			callHandler = new CallHandlerThread();
			callHandler.start();
		}
		
		//close any open callMakerThread
		if(callMaker != null) {
			try {
				callMaker.closeThread();
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
			callMaker = null;
		}
		
		//determine what main screen banner will say
		messageBanner.setText(message);
		

		//clear grid
		gridPane.getChildren().clear();		
		
		//add default screen elements to center of screen
		gridPane.add(messageBanner, 0, 0);
		gridPane.setHalignment(messageBanner, HPos.CENTER);
		
		gridPane.add(yourIp, 0, 3);	
		gridPane.setHalignment(yourIp, HPos.CENTER);
		
		gridPane.add(makeCallButton, 0, 10);
		gridPane.setHalignment(makeCallButton, HPos.CENTER);
	}
	
	public static void showMakeCallScreen() {
		
		//clear grid
		gridPane.getChildren().clear();
		
		ipCallMessage.setText("Enter number to call:");
		
		//add call making screen elements to center of screen
		gridPane.add(ipCallMessage, 0, 0);
		gridPane.setHalignment(ipCallMessage, HPos.CENTER);
		
		ipCallRecipient.clear();
		gridPane.add(ipCallRecipient, 0, 1);
		gridPane.setHalignment(ipCallRecipient, HPos.CENTER);
		
		gridPane.add(callButton, 0, 3);
		gridPane.setHalignment(callButton, HPos.CENTER);
		
		gridPane.add(backButton, 0, 10);
		gridPane.setHalignment(backButton, HPos.CENTER);
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
	
	public static void showReceivingScreen(String otherIp) {		
		//clear grid
		gridPane.getChildren().clear();
		
		//separate IP from initial slash character and port number
		otherIp = otherIp.substring(1,otherIp.indexOf(':'));
		
		receivingCallFromIP.setText("Receiving call from: " + otherIp);
		
		//add receiving call screen elements to center of screen
		gridPane.add(receivingCallFromIP, 0, 0);
		gridPane.setHalignment(receivingCallFromIP, HPos.CENTER);
		
		gridPane.add(acceptCallButton, 0, 1);
		gridPane.setHalignment(acceptCallButton, HPos.CENTER);
		
		gridPane.add(rejectCallButton, 0, 2);
		gridPane.setHalignment(rejectCallButton, HPos.CENTER);		
	}
	
	public static void showInCallScreen(String otherIp) {
		//clear grid
		gridPane.getChildren().clear();
		
		//separate IP from initial slash character and port number
		otherIp = otherIp.substring(1,otherIp.indexOf(':'));
		
		inCallWithIp.setText("Currently in call with: \n" + otherIp);
		
		//add in call screen elements to center of screen
		gridPane.add(inCallWithIp, 0, 0);
		gridPane.setHalignment(inCallWithIp, HPos.CENTER);	
		gridPane.add(endCallButton, 0, 4);
		gridPane.setHalignment(endCallButton, HPos.CENTER);	
	}
	
	private static boolean isValidIp(String ipToTest) {
		//regex for ip address from Regular Expressions Cookbook by Oreilly
		//along with regex for colon and port number
		String ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
			+ "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):(?:6553[0-5]|655"
			+ "[0-2][0-9]|65[0-4][0-9][0-9]|6[0-4][0-9][0-9][0-9]|[0-5][0-9]"
			+ "[0-9][0-9][0-9]|\\d{0,4})$";
			
		//search for regex pattern in ip argument
		Pattern regex = Pattern.compile(ipPattern);
		Matcher matcher = regex.matcher(ipToTest);
		
		return matcher.find();
	}
	
	private static boolean isOwnAddress(String ipToTest) {
		
		boolean isOwn = false;
		
		//split ip and port into two parts
		String[] items = ipToTest.split(":");
		
		String thisHost = "";
		
		//try to set thisHost to the ip address of the local system
		try {
			thisHost = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException uhex) {
			System.out.println("LOCALHOST ERROR: " + uhex.getMessage());
		}
	
		//If ip is this host's IP
		if (items[0].equals(thisHost) || items[0].equals("127.0.0.1")
		|| items[0].equals("localhost")) {

			//if port is the port to which this instance is bound:
			if (items[1].equals(Integer.toString(bindPort))) {
				isOwn = true;
			}
		}
		
		return isOwn;
	}
}