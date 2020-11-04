import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main extends Application {
	
	//thread with socket for receiving calls
	static CallHandlerThread callHandler;
	static CallMakerThread callMaker;

	//pane for showing nodes on screen
	static GridPane gridPane;
	
	//universal back button to return to default screen
	static Button backButton;


	//default screen nodes
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
		
		//default screen nodes init
		yourIp = new Label("Your ip is: "
			+ "127.0.0.1" + "\nAwaiting Call...");
		makeCallButton = new Button("Make Call");
		
		//make call screen nodes init
		callButton = new Button("Call");
		ipCallMessage = new Label("Enter ip address to call:");
		ipCallRecipient = new TextField();
		
		//calling screen node init
		callingIpLabel = new Label("Calling:");
		cancelCallButton = new Button("Cancel Call");
		
		//receiving call screen init
		receivingCallFromIP = new Label("Receiving call from:");
		acceptCallButton = new Button("Accept Call");
		rejectCallButton = new Button("Reject Call");
		
		//in call node init
		inCallWithIp = new Label("Currently in call with:");
		endCallButton = new Button("End Call");	
		
		
		//set actions for the buttons
		initButtonActions();
		
		//add grid to base of scene
		base.getChildren().add(gridPane);
		//create scene with addon, width, height
		Scene scene = new Scene(base, 500, 500);
		
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
		
		
		
		//create and start socket thread for reception of calls
		callHandler = new CallHandlerThread();
		callHandler.start();
		
		//callMaker starts null
		callMaker = null;
		
		//start off at default screen
		showDefaultScreen();
	}
	
	public static void initButtonActions() {
		//set action for "back" button
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				showDefaultScreen();
			}
		});	
		
		//set action for "make call" button
		makeCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				try {
					callHandler.closeThread();
					callHandler = null;
					
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
					callMaker = new CallMakerThread(ipCallRecipient.getText());
					callMaker.start();

					showCallingScreen();
				} else {
					ipCallMessage.setText("Not a valid address, try again:");
				}
			}
		});
		
		//set action for "accept call" button
		acceptCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				callHandler.acceptCall();
			}
		});
		
		//set action for "reject call" button
		rejectCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				callHandler.rejectCall();
			}
		});
	}
	
	public static void showDefaultScreen() {
		//clear grid
		gridPane.getChildren().clear();		
		
		gridPane.add(yourIp, 0, 0);	
		gridPane.add(makeCallButton, 0, 5);		
	}
	
	public static void showMakeCallScreen() {
		//clear grid
		gridPane.getChildren().clear();
		
		//add buttons and field for making call to grid
		gridPane.add(ipCallMessage, 0, 0);
		gridPane.add(ipCallRecipient, 0, 1);
		gridPane.add(callButton, 1, 1);
		gridPane.add(backButton, 0, 4);
	}
	
	public static void showCallingScreen() {
		//clear grid
		gridPane.getChildren().clear();
		
		callingIpLabel.setText("Calling: " + ipCallRecipient.getText());
		
		//TODO: initiate call connection with timeout limit
		
		//add buttons and field for making call to grid
		gridPane.add(callingIpLabel, 0, 0);
		gridPane.add(cancelCallButton, 0, 4);		
	}
	
	public static void showReceivingScreen(String otherIp) {		
		//clear grid
		gridPane.getChildren().clear();
		
		receivingCallFromIP.setText("Receiving call from: " + otherIp);
		
		//add buttons for accepting and rejecting calls
		gridPane.add(receivingCallFromIP, 0, 0);
		gridPane.add(acceptCallButton, 1, 0);
		gridPane.add(rejectCallButton, 1, 1);			
	}
	
	public static void showInCallScreen(String otherIp) {
		//clear grid
		gridPane.getChildren().clear();
		

		//TODO: show interactive call screen with other person
		
		
		gridPane.add(backButton, 0, 4);
	}
	
	private static boolean isValidIp(String ipToTest) {
		//regex for ip address from Regular Expressions Cookbook by Oreilly
		String ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
			+ "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
			
		//search for regex pattern in ip argument
		Pattern regex = Pattern.compile(ipPattern);
		Matcher matcher = regex.matcher(ipToTest);
		
		return matcher.find();
	}
}