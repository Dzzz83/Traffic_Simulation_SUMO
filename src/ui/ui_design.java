package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ui_design extends Application { // <--- Extends Application

    @Override
    public void start(Stage primaryStage) throws Exception {
        // loads the design from the FXML file
        Parent root = FXMLLoader.load(getClass().getResource("ControlPanel.fxml"));

        Scene scene = new Scene(root);
        primaryStage.setTitle("Real-time Traffic Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}