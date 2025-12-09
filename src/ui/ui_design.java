package ui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.Scene;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class ui_design extends Application {
    @Override
    public void start(Stage mystage) throws Exception {

        // Define window size
        double windowWidth = 2000;
        double windowHeight = 2000;

        double mapWidth = 1500;
        double mapHeight = 1500;

        double canvasWidth = 2000;
        double canvasHeight = 2000;

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawMap(gc, mapWidth, mapHeight);

        // map layer
        Pane mapLayer = new Pane(canvas);

        // set mouse scrolling to zoom/in out
        mapLayer.setOnScroll(event -> {
            event.consume();

            double zoomSize = 1.05;
            double deltaY = event.getDeltaY();

            // zoom in/out
            if (deltaY < 0) {
                zoomSize = 1 / zoomSize;
            }

            double newScale = mapLayer.getScaleX() * zoomSize;

            if (newScale > 0.1 && newScale < 10) {
                mapLayer.setScaleX(newScale);
                mapLayer.setScaleY(newScale);
            }
        });

        // set mouse drag to move around the map
        final double[] anchor = new double[4]; // [startX, startY, startTranslateX, startTranslateY]

        mapLayer.setOnMousePressed(event -> {
            anchor[0] = event.getSceneX();
            anchor[1] = event.getSceneY();
            anchor[2] = mapLayer.getTranslateX();
            anchor[3] = mapLayer.getTranslateY();
        });

        mapLayer.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - anchor[0];
            double deltaY = event.getSceneY() - anchor[1];

            mapLayer.setTranslateX(anchor[2] + deltaX);
            mapLayer.setTranslateY(anchor[3] + deltaY);
        });


        // Define sections title
        var controlPanelSection = new Label("Control Panel");
        var statsSection = new Label("Stats");

        // set positions for the title
        controlPanelSection.setStyle("-fx-font-family: Arial; -fx-text-fill: black; -fx-font-size: 30px;");
        controlPanelSection.setLayoutX(120);
        controlPanelSection.setLayoutY(50);

        statsSection.setStyle("-fx-font-family: Arial; -fx-text-fill: black; -fx-font-size: 30px;");
        statsSection.setLayoutX(160);
        statsSection.setLayoutY(350);

        // Define stats attributes
        Label connectionStatus = new Label("Connection Status: ");
        connectionStatus.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label numberVehicles = new Label("Number of Vehicles: ");
        numberVehicles.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label averageSpeed = new Label("Average Speed: ");
        averageSpeed.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label congestionDensity = new Label("Congestion Density: ");
        congestionDensity.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Label numberRoads = new Label("Number of Roads: ");
        numberRoads.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Define Stats Container to stack all the attributes
        VBox statsContainer = new VBox(10);

        statsContainer.setAlignment(Pos.TOP_LEFT);

        statsContainer.getChildren().addAll(connectionStatus, numberVehicles, averageSpeed, congestionDensity, numberRoads);

        statsContainer.setLayoutX(20);
        statsContainer.setLayoutY(400);

        // Define buttons
        var startButton = new Button("Start");
        var stopButton = new Button("Stop");
        var EdgeIDButton = new Button("Edge ID");
        var RouteIDButton = new Button("Traffic ID");
        var VehicleIDButton = new Button("Vehicle ID");
        var TrafficLightIDButton = new Button("Traffic Light ID");

        // set positions for the buttons
        startButton.setLayoutX(150);
        startButton.setLayoutY(100);

        stopButton.setLayoutX(230);
        stopButton.setLayoutY(100);

        EdgeIDButton.setLayoutX(20);
        EdgeIDButton.setLayoutY(150);

        RouteIDButton.setLayoutX(120);
        RouteIDButton.setLayoutY(150);

        VehicleIDButton.setLayoutX(220);
        VehicleIDButton.setLayoutY(150);

        TrafficLightIDButton.setLayoutX(320);
        TrafficLightIDButton.setLayoutY(150);

        // apply colors to buttons
        startButton.setStyle("-fx-background-color: #009933; -fx-text-color: #f0f0f0;");
        stopButton.setStyle("-fx-background-color: #ff0000; -fx-text-color: #f0f0f0;");

        // define timelapse sliders and its components
        Label timeLapseLabel = new Label("Time Lapse");
        timeLapseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Slider timeLapseSlider = new Slider(0, 5, 0.1); // min, max, initial
        timeLapseSlider.setPrefWidth(200);

        Label valueLapseLabel = new Label("Value Lapse");
        valueLapseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        timeLapseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLapseLabel.setText(String.format("%.1f TIMES", newVal));
        });

        HBox timeLapseControlBar= new HBox(15);
        timeLapseControlBar.setAlignment(Pos.CENTER);
        timeLapseControlBar.getChildren().addAll(timeLapseLabel, timeLapseSlider, valueLapseLabel);

        timeLapseControlBar.setLayoutX(20);
        timeLapseControlBar.setLayoutY(200);

        // Define inflow sliders and its components
        Label inFlowLabel = new Label("Inflow");
        inFlowLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Slider inFlowSlider = new Slider(0, 5, 0.1); // min, max, initial
        inFlowSlider.setPrefWidth(200);

        Label inFlowValue = new Label("Value Inflow");
        inFlowValue.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        inFlowSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            inFlowValue.setText(String.format("%.1f TIMES", newVal));
        });

        HBox inFlowControlBar= new HBox(15);
        inFlowControlBar.setAlignment(Pos.CENTER);
        inFlowControlBar.getChildren().addAll(inFlowLabel, inFlowSlider, inFlowValue);

        inFlowControlBar.setLayoutX(20);
        inFlowControlBar.setLayoutY(250);

        // Define the maxSpeed slider and its components
        Label maxSpeedLabel = new Label("Max Speed");
        maxSpeedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Slider maxSpeedSlider = new Slider(0, 5, 0.1); // min, max, initial
        maxSpeedSlider.setPrefWidth(200);

        Label maxSpeedValue = new Label("Value Inflow");
        inFlowValue.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        maxSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxSpeedValue.setText(String.format("%.1f TIMES", newVal));
        });

        HBox maxSpeedControlBar= new HBox(15);
        maxSpeedControlBar.setAlignment(Pos.CENTER);
        maxSpeedControlBar.getChildren().addAll(maxSpeedLabel, maxSpeedSlider, maxSpeedValue);

        maxSpeedControlBar.setLayoutX(20);
        maxSpeedControlBar.setLayoutY(300);

        // Menu icon to show/hide the whole Control Panel and Stats
        Label menuIcon = new Label("☰");
        menuIcon.setStyle("-fx-font-size: 30px; -fx-cursor: hand; -fx-text-fill: white;");
        menuIcon.setLayoutX(20);
        menuIcon.setLayoutY(10);

        // Define the sidebar
        Pane sidebar = new Pane();

        sidebar.setStyle("-fx-background-color: rgba(240, 240, 240, 0.85); -fx-background-radius: 10;");
        sidebar.setLayoutX(40);
        sidebar.setLayoutY(80);

        // Define sidebar stacking
        sidebar.getChildren().addAll(controlPanelSection, statsSection, statsContainer, startButton, stopButton, EdgeIDButton, RouteIDButton, VehicleIDButton, TrafficLightIDButton, timeLapseControlBar, inFlowControlBar, maxSpeedControlBar);

        // Logic to show/hide sidebar
        menuIcon.setOnMouseClicked(event -> {
            boolean isVisible = sidebar.isVisible();
            sidebar.setVisible(!isVisible);
        });

        // Define layout
        Pane UILayout = new Pane();
        UILayout.setPickOnBounds(false);
        UILayout.getChildren().addAll(sidebar, menuIcon);

        StackPane UIandMapLayout = new StackPane();
        UIandMapLayout.setStyle("-fx-background-color: black;");
        UIandMapLayout.getChildren().addAll(mapLayer, UILayout);

        // Wrap layout in scene
        var scene = new Scene(UIandMapLayout, windowWidth, windowHeight);

        // set title, scene, and show them
        mystage.setTitle("Real-time Traffic Simulation");
        mystage.setScene(scene);
        mystage.show();
    }

    private void drawMap(GraphicsContext gc, double width, double height) {
        try {
            // Define the file to pass to the xml parser
            File mapFile = new File("/Users/lamquangthien/Downloads/Traffic_Simulation_SUMO/src/demo.net.xml");

            // Init the xml parser
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(mapFile);
            doc.getDocumentElement().normalize();

            Element location = (Element) doc.getElementsByTagName("location").item(0);
            String convBoundary = location.getAttribute("convBoundary");

            String[] bounds = convBoundary.split(",");

            double minX = Double.parseDouble(bounds[0]);
            double minY = Double.parseDouble(bounds[1]);
            double maxX = Double.parseDouble(bounds[2]);
            double maxY = Double.parseDouble(bounds[3]);

            double mapWidth = maxX - minX;
            double mapHeight = maxY - minY;

            double scaleX = width / mapWidth;
            double scaleY = height / mapHeight;
            double scale = Math.min(scaleX, scaleY) * 0.9;

            double OffsetX = (width - (mapWidth * scale)) / 2;
            double OffsetY = (height - (mapHeight * scale)) / 2;

            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(2);

            NodeList lanes =  doc.getElementsByTagName("lane");

            for (int i = 0; i < lanes.getLength(); i++) {
                Element lane = (Element) lanes.item(i);
                String shape = lane.getAttribute("shape");

                if (shape == null || shape.isEmpty()) continue;

                String[] pairs = shape.split(" ");

                double[] xPoints = new double[pairs.length];
                double[] yPoints = new double[pairs.length];

                for (int j = 0; j < pairs.length; j++) {
                    String[] coords = pairs[j].split(",");
                    double rawX = Double.parseDouble(coords[0]);
                    double rawY = Double.parseDouble(coords[1]);

                    xPoints[j] = ((rawX - minX) * scale) + OffsetX;
                    yPoints[j] = ((maxY - rawY) * scale) + OffsetY;
                }

                gc.strokePolyline(xPoints, yPoints, pairs.length);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}