import javafx.application.Application;
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


public class Main extends Application {
	//pane for showing nodes on screen
	GridPane gridPane;
	
	//universal back button to return to default screen
	Button backButton;


	//default screen nodes
	Label yourIp;
	Button makeCallButton;
	
	//make call screen nodes
	Button callButton;
	Label ipCallMessage;
	TextField ipCallRecipient;
	
	//calling screen node
	Label callingIpLabel;
	
	//receiving call screen
	Label receivingCallFromIP;
	Button acceptCallButton;
	Button rejectCallButton;
	
	//in call node
	Label inCallWithIp;
	Button endCallButton;


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
		Label yourIp = new Label("Your ip is: "
			+ "127.0.0.1" + "\nAwaiting Call...");
		makeCallButton = new Button("Make Call");
		
		//make call screen nodes init
		callButton = new Button("Call");
		ipCallMessage = new Label("Enter ip address to call:");
		ipCallRecipient = new TextField();
		
		//calling screen node init
		callingIpLabel = new Label("Calling:");
		
		//receiving call screen init
		receivingCallFromIP = new Label("Receiving call from:");
		acceptCallButton = new Button("Accept Call");
		rejectCallButton = new Button("Reject Call");
		
		//in call node init
		inCallWithIp = new Label("Currently in call with: " + "1.2.3.4");
		endCallButton = new Button("End Call");	
		
		

		//set actions for the buttons
		initButtonActions();

		//add button to grid
		gridPane.add(makeCallButton, 0, 0);
		
		//add grid to base of scene
		base.getChildren().add(gridPane);
		
		//create scene with addon, width, height
		Scene scene = new Scene(base, 500, 500);
		
		//set and show the scene
		stage.setScene(scene);
		stage.show();
	}
	
	public void initButtonActions() {
		//set action for "back" button
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				//clear grid
				gridPane.getChildren().clear();
				
				
				//add make call button to grid
				gridPane.add(makeCallButton, 0, 0);
			}
		});	
		
		//set action for "make call" button
		makeCallButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				//clear grid
				gridPane.getChildren().clear();
				
				//add buttons and field for making call to grid
				gridPane.add(ipCallMessage, 0, 0);
				gridPane.add(ipCallRecipient, 0, 1);
				gridPane.add(callButton, 1, 1);
				gridPane.add(backButton, 0, 4);
			}
		});
		
		//set action for "call" button
		callButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ev) {
				//clear grid
				gridPane.getChildren().clear();
				
				callingIpLabel.setText("Calling: " + ipCallRecipient.getText());
				
				//TODO
				//Error handling for ip address input
				
				//add buttons and field for making call to grid
				gridPane.add(callingIpLabel, 0, 0);
				gridPane.add(backButton, 0, 4);
			}
		});
		
	
	}
}