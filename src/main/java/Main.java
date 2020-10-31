import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button; 
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class Main extends Application {
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) {
		//set title of app
		stage.setTitle("Communicator");
		
		//create base and grid for adding elements
		StackPane base = new StackPane();
        GridPane gridPane = new GridPane();

		//configure alignment of grid
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		gridPane.setAlignment(Pos.CENTER);		
		
		
		//create button for making calls
		Button callButton = new Button("Make Call");
		

		//add button to grid
		gridPane.add(callButton, 0, 0);
		
		//add grid to base of scene
		base.getChildren().add(gridPane);
		
		//create scene with addon, width, height
		Scene scene = new Scene(base, 500, 500);
		
		//set and show the scene
		stage.setScene(scene);
		stage.show();
	}
}