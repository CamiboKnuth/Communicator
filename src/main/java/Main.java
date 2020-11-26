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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.BindException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
TODO: Encryption?
*/


public class Main extends Application {
	
	//starting point for binding server socket
	final static int BASE_PORT = 9900;
	
	//actual value to which server socket will be bound
	static int bindPort;
	
	//socket for receiving calls
	static ServerSocket serverSocket;
	static DatagramSocket datagramSocket;
	
	//phone number for this instance of the program
	static String instanceNumber;
	
	
	//thread with socket for receiving calls
	static CallHandlerThread callHandler;
	static CallMakerThread callMaker;

	//for showing nodes on screen
	static Stage stage;
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
	static ImageView mirrorView;
	static ImageView otherView;
	static Label inCallWithIp;
	static Button endCallButton;
	static Button toggleCameraButton;
	static Image noCamImage;
	static Image noVideoImage;
	static Image cameraUnavailableImg;
	
	
	static boolean cameraOn = false;


	public static void main(String[] args) {	
		launch(args);
	}
	
	@Override
	public void start(Stage stage) {
		
		this.stage = stage;
		
		//set title of the window
		stage.setTitle("Communicator");
		
		//create base and grid for adding elements
		//StackPane base = new StackPane();
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
		mirrorView = new ImageView();
		mirrorView.setRotationAxis(Rotate.Y_AXIS);
		mirrorView.setRotate(180);
		mirrorView.setFitHeight(100);
		mirrorView.setFitWidth(100);
		mirrorView.setPreserveRatio(true);

		otherView = new ImageView();
		
		inCallWithIp = new Label("Currently in call with:");
		inCallWithIp.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-color: #555555;");
		
		endCallButton = new Button("End Call");
		endCallButton.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
			
		toggleCameraButton = new Button("Start Camera");
		toggleCameraButton.setStyle("-fx-font-size: 1.3em; -fx-font-weight: bold;"
			+ "-fx-color: #CCCCCC; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
			
		
		//set images for no video and no camera
		initImages();
		
		//set actions for the buttons
		initButtonActions();
		
		//add grid to base of scene
		//base.getChildren().add(gridPane);
		//create scene with addon, width, height
		Scene scene = new Scene(gridPane, 350, 550);
		
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
					datagramSocket.close();
					
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
				//try to bind serverSocket and datagram socket.
				try { 
					serverSocket = new ServerSocket(bindPort);
					serverSocket.setSoTimeout(100);
					datagramSocket = new DatagramSocket(bindPort + 16);
					datagramSocket.setSoTimeout(100);
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
	
	public static void initImages() {
		try {
			URL resource = Main.class.getClassLoader().getResource("nocamera.jpg");
			File imgFile = new File(resource.toURI());
			noCamImage = new Image(new FileInputStream(imgFile));
		} catch (IOException ioex) {
			ioex.printStackTrace();
		} catch (URISyntaxException urex) {
			urex.printStackTrace();
		}
		
		try {
			URL resource = Main.class.getClassLoader().getResource("novideo.jpg");
			File imgFile = new File(resource.toURI());
			noVideoImage = new Image(new FileInputStream(imgFile));
		} catch (IOException ioex) {
			ioex.printStackTrace();
		} catch (URISyntaxException urex) {
			urex.printStackTrace();
		}

		try {
			URL resource = Main.class.getClassLoader().getResource("cameraunavailable.jpg");
			File imgFile = new File(resource.toURI());
			cameraUnavailableImg = new Image(new FileInputStream(imgFile));
		} catch (IOException ioex) {
			ioex.printStackTrace();
		} catch (URISyntaxException urex) {
			urex.printStackTrace();
		}
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
						IpTools.numberToInetSocketAddress(recipient, 0);
					
					//if call recipient is not this instance
					if (!IpTools.isOwnAddress(recipientAddress)) {
						
						try {
							//close callHandler temporarily
							callHandler.closeThread();
							callHandler.join();
							callHandler = null;
							
							//start callMaker to contact recipient
							callMaker = new CallMakerThread(datagramSocket, recipientAddress, recipient);
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
		
		//set action for toggle camera button
		toggleCameraButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				if(cameraOn) {
					toggleCameraButton.setText("Start Camera");
					showNoCameraMirror();
					cameraOn = false;
					VideoSender.getInstance().stopCamera();
				} else {
					toggleCameraButton.setText("Stop Camera");
					cameraOn = true;
					VideoSender.getInstance().startCamera();
				}
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
		
		mirrorView = new ImageView();
		mirrorView.setRotationAxis(Rotate.Y_AXIS);
		mirrorView.setRotate(180);

		otherView = new ImageView();
		
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
		
		//prevent screen from being enlarged
		stage.setResizable(false);
		
		//determine what main screen banner will say
		messageBanner.setText(message);
		//determine what will display over number field
		ipCallMessage.setText("Enter number to call:");
		
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
		callHandler = new CallHandlerThread(serverSocket, datagramSocket);
		callHandler.start();		

		//reset images on call screen
		setMirrorImageSize(100,100);
		setReceivedImageSize(350, 235);	
		showNoCameraMirror();
		showNoVideoImage();

		toggleCameraButton.setText("Start Camera");
		cameraOn = false;


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
		

		//ensure that screen is reset
		stage.setWidth(350);
		stage.setHeight(550);
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
		
		//permit screen size to change
		stage.setResizable(true);
		stage.setMinHeight(550);
		stage.setMinWidth(350);
		
		//clear grid
		gridPane.getChildren().clear();
		
		inCallWithIp.setText("Currently in call with: \n" + otherNum);
		
		//add in call screen elements to center of screen
		gridPane.add(mirrorView, 0, 0);
		gridPane.setHalignment(mirrorView, HPos.CENTER);
		
		gridPane.add(otherView, 0, 1);
		gridPane.setHalignment(otherView, HPos.CENTER);
		
		gridPane.add(inCallWithIp, 0, 2);
		gridPane.setHalignment(inCallWithIp, HPos.CENTER);	

		HBox hbox = new HBox();
		hbox.setSpacing(30);
		hbox.setAlignment(Pos.CENTER);
		hbox.getChildren().addAll(endCallButton, toggleCameraButton);
		
		gridPane.add(hbox, 0, 3);
		gridPane.setHalignment(hbox, HPos.CENTER);
	}
	
	public static void setReceivedImageSize(int width, int height) {
		otherView.setFitHeight(height);
		otherView.setFitWidth(width);
		otherView.setPreserveRatio(true);		
	}
	
	public static void fitReceivedImageSize() {
		otherView.setFitHeight(stage.getHeight() - 100);
		otherView.setFitWidth(stage.getHeight() - 150);
		otherView.setPreserveRatio(true);		
	}
	
	public static void showReceivedImage(byte[] toShow) {
		Image image = new Image(new ByteArrayInputStream(toShow));
		otherView.setImage(image);
	}
	
	public static void showNoVideoImage() {
		otherView.setImage(noVideoImage);
	}

	public static void setMirrorImageSize(int width, int height) {
		mirrorView.setFitHeight(height);
		mirrorView.setFitWidth(width);
		mirrorView.setPreserveRatio(true);		
	}	
	
	public static void showMirrorImage(byte[] toShow) {
		Image image = new Image(new ByteArrayInputStream(toShow));
		mirrorView.setImage(image);
	}
	
	public static void showNoCameraMirror() {
		mirrorView.setImage(noCamImage);
	}
	
	public static void showUnavailableCam() {
		mirrorView.setImage(cameraUnavailableImg);	
	}
}