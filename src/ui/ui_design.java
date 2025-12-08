package ui;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class ui_design extends Application {
    @Override
    public void start(Stage mystage) throws Exception {
        Group root = new Group();
        Scene scene = new Scene(root,Color.BLACK);

        mystage.setTitle("Real-time Traffic Simulation");
        mystage.setScene(scene);
        mystage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}