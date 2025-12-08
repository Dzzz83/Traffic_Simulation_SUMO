package ui;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class ui_design extends Application {
    @Override
    public void start(Stage mystage) throws Exception {
        // Define stats section title
        var statsSection = new Label("Stats");

        // set positions for the title
        statsSection.setLayoutX(1000);
        statsSection.setLayoutY(1000);

        // Define buttons
        var startButton = new Button("Start");
        var stopButton = new Button("Stop");

        // set positions for the buttons
        startButton.setLayoutX(100);
        startButton.setLayoutY(100);

        // apply colors to buttons
        startButton.setStyle("-fx-background-color: #009933; -fx-text-color: #f0f0f0;");
        stopButton.setStyle("-fx-background-color: #ff0000; -fx-text-color: #f0f0f0; fx-background-radius: 10;");

        // Define layout
        var layout = new VBox(10, statsSection, startButton, stopButton);

        // Wrap layout in scene
        var scence = new Scene(layout, 20000, 2000);

        // set title, scene, and show them
        mystage.setTitle("Real-time Traffic Simulation");
        mystage.setScene(scence);
        mystage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}