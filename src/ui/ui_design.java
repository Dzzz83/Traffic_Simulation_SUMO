package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ui_design extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // loads the design from the FXML file
        Parent root = FXMLLoader.load(getClass().getResource("ControlPanel.fxml"));

        // create a "Scene" which holds all the buttons, sliders, and the map
        Scene scene = new Scene(root);
        primaryStage.setTitle("Real-time Traffic Simulation");
        // put the scene inside the main window (Stage)
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}