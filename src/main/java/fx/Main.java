package  main.java.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Label label = new Label("SUMO + JavaFX Project");
        Scene scene = new Scene(label, 400, 200);
        stage.setScene(scene);
        stage.setTitle("SUMO Simulation UI");
        stage.show();

        // Start SUMO simulation
        ProcessBuilder pb = new ProcessBuilder(
                "sumo-gui",
                "-c", "C:/path/to/your/config.sumocfg"
        );
        pb.start();
    }

    public static void main(String[] args) {
        launch();
    }
}
