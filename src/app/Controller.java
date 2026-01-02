// this file serves as the central code that connects backend (ControlPanel.java) and frontend (ui_design.java & ControlPanel.fxml)
package app;

import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import javafx.scene.shape.Box;
import wrapperSUMO.ControlPanel;
import wrapperSUMO.TrafficLightWrapper;
import de.tudresden.sumo.objects.SumoPosition2D;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager; // use for logging
import org.apache.logging.log4j.Logger;

public class Controller {
    private static final Logger LOG = LogManager.getLogger(Controller.class.getName());

    // initialize variables
    // @FXML is a special tag that tells Java to look into fxml file and link the correct object this file
    @FXML
    private Canvas mapCanvas;
    @FXML
    private StackPane rootPane;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Label menuIcon;
    @FXML
    private VBox sidebar;

    // buttons
    @FXML
    private Button EdgeIDBtn;
    @FXML
    private Button RouteIDBtn;
    @FXML
    private Button VehicleIDBtn;
    @FXML
    private Button TrafficLightIDBtn;

    @FXML
    private ComboBox<String> trafficIdCombo;
    @FXML
    private ToggleButton autoModeToggle;

    // sliders
    @FXML
    private Slider delaySlider;
    @FXML
    private Slider inFlowSlider;
    @FXML
    private Slider maxSpeedSlider;
    @FXML
    private Slider sliderRed;
    @FXML
    private Slider sliderGreen;
    @FXML
    private Slider sliderYellow;

    // sliders values
    @FXML
    private Label delayValue;
    @FXML
    private Label inFlowValue;
    @FXML
    private Label maxSpeedValue;
    @FXML
    private Label redValue;
    @FXML
    private Label greenValue;
    @FXML
    private Label yellowValue;

    // stats
    @FXML
    private Label connectionStatus;
    @FXML
    private Label numberVehicles;
    @FXML
    private Label averageSpeed;
    @FXML
    private Label congestionDensity;
    @FXML
    private Label numberOfEdges;
    @FXML
    private Label numOfTL;
    @FXML
    private Label co2Emission;

    // chart
    @FXML
    private LineChart<Number, Number> avgSpeedChart;

    // traffic light
    @FXML
    private Label labelRed;
    @FXML
    private Label labelGreen;
    @FXML
    private Label labelYellow;
    private TrafficLightWrapper tlsWrapper;
    private static final String NET_XML_PATH = "src/SumoConfig/demo.net.xml";
    // logical variables
    private ControlPanel panel;
    private AnimationTimer simulationLoop;
    private Map<String, List<SumoPosition2D>> mapShapes = null;

    private MapDraw mapDraw;
    private MapDraw3D mapDraw3D;

    // variables to highlight edgeID on the map
    private boolean showEdgesID = false;
    private boolean showTrafficLightID = false;
    private boolean showRouteID = false;
    private boolean showVehicleID = false;

    // variables for chart
    private XYChart.Series<Number, Number> speedSeries;
    private double timeSeconds = 0;

    // variables for map
    // SCALE = 1.0 ==> 1 meter in SUMO is 1 pixel on the screen
    private double SCALE = 1.0;       // initial Zoom
    // these 2 variables show the distance the camera has been moved away from the origin (0,0)
    private double OFFSET_X = 0;      // initial Pan X
    private double OFFSET_Y = 0;      // initial Pan Y
    // variables to store the position of the mouse in the past to implement click and drag feature
    private double lastMouseX, lastMouseY; // for dragging calculation

    // variable remembers the route for the next click
    private int clickRouteIndex = 0;

    // variable for stress test 2 button
    private boolean isStressTest2Active = false;

    // logic variables to show/hide to sidebar
    private boolean isSidebarVisible = true;

    // variables for delay button
    private long lastUpdate = 0;
    private long simulationDelay = 100_000_000; // Default 100ms in nanoseconds


    private HashSet<KeyCode> keyInputSet = new HashSet<KeyCode>();

    @FXML
    // initialize the GUI function
    public void initialize() {
        LOG.info("Starting the GUI ...");

        // initialize Control Panel
        panel = new ControlPanel();

        mapDraw = new MapDraw(mapCanvas);
        mapDraw3D = new MapDraw3D();

        // connect to SUMO and load Map
        LOG.info("Connecting to SUMO to fetch map...");
        if (panel.startSimulation()) {
            connectionStatus.setText("Connection: Connected");
            connectionStatus.setStyle("-fx-text-fill: green");
            tlsWrapper = panel.getTrafficLightWrapper();
            tlsWrapper.isRunning = true;
            tlsWrapper.loadConnectionDirections(NET_XML_PATH);
        } else {
            connectionStatus.setText("Connection: Disconnected");
            connectionStatus.setStyle("-fx-text-fill: red");
        }
        mapShapes = panel.getMapShape();

        initialize3D();

        // traffic light
        List<String> tlsIds = panel.getTrafficLightIDs();
        trafficIdCombo.getItems().clear();
        if (tlsIds != null && !tlsIds.isEmpty()) {
            trafficIdCombo.getItems().addAll(tlsIds);
            trafficIdCombo.getSelectionModel().selectFirst();
        }

        autoModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            String selectedId = trafficIdCombo.getValue();
            if (selectedId == null) return;

            if (newVal) {
                // Button is ON -> Enable Logic "0" (Standard Program)
                autoModeToggle.setText("ON");
                autoModeToggle.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                // Call backend:
                panel.turnOnTrafficLight(selectedId);
            } else {
                // Button is OFF -> Disable Lights "off"
                autoModeToggle.setText("OFF");
                autoModeToggle.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                // Call backend:
                panel.turnOffTrafficLight(selectedId);
            }
        });

        // Helper listener for Red Slider
        sliderRed.valueProperty().addListener((obs, oldVal, newVal) -> {
            int seconds = newVal.intValue();
            labelRed.setText(seconds + "s");
        });

        sliderGreen.valueProperty().addListener((obs, oldVal, newVal) -> {
            int seconds = newVal.intValue();
            labelGreen.setText(seconds + "s");
        });

        sliderYellow.valueProperty().addListener((obs, oldVal, newVal) -> {
            int seconds = newVal.intValue();
            labelYellow.setText(seconds + "s");
        });

        // setup chart

        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Real-time Speed");
        avgSpeedChart.getData().add(speedSeries);

        // setup UI interactions
        setupMapInteractions();
        setupMenu();
        setupControls();

        // Center the map
        OFFSET_X = mapCanvas.getWidth() / 4;
        OFFSET_Y = mapCanvas.getHeight() / 4;
        drawMap();

        // setup loop
        simulationLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (panel.isRunning()) {

                    mapDraw3D.updateCamera(keyInputSet);

                    // only update if enough time has passed
                    if (now - lastUpdate >= simulationDelay) {
                        updateSimulation();
                        lastUpdate = now;
                    }
                }
            }
        };

        // initialize the traffic light selection dropdown menu
        trafficIdCombo.getItems().addAll("Junction_1", "Junction_2");
        trafficIdCombo.getSelectionModel().selectFirst();
    }

    // menu toggle function
    private void setupMenu() {
        menuIcon.setOnMouseClicked(event -> {
            isSidebarVisible = !isSidebarVisible;
            sidebar.setVisible(isSidebarVisible);
            sidebar.setManaged(isSidebarVisible);
        });
    }

    private void setupMapInteractions() {
        // zoom in zoom out feature
        mapCanvas.setOnScroll(event -> {
            double zoomFactor = 1.1; // 10% zoom per scroll
            if (event.getDeltaY() < 0) {
                SCALE /= zoomFactor; // zoom out logic
            } else {
                SCALE *= zoomFactor; // zoom in logic
            }
            drawMap(); // redraw map according to the zoom
        });

        // drag mouse feature
        // capture mouse position when user clicks
        mapCanvas.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
        });

        // calculate movement when dragged logic
        mapCanvas.setOnMouseDragged(event -> {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;

            OFFSET_X += deltaX;


            // dragging down
            OFFSET_Y -= deltaY;

            lastMouseX = event.getX();
            lastMouseY = event.getY();

            drawMap(); // redraw map when done calculating
        });
    }

    private void setupControls() {
        startBtn.setOnAction(e -> onStartClick());
        stopBtn.setOnAction(e -> onStopClick());

        // make the edgeID button function
        if (EdgeIDBtn != null) {
            EdgeIDBtn.setOnAction(e -> {
                showEdgesID = !showEdgesID;

                if (showEdgesID) {
                    EdgeIDBtn.setStyle("-fx-background-color: #add8e6;");
                } else {
                    EdgeIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        // make the traffic light id button function
        if (TrafficLightIDBtn != null) {
            TrafficLightIDBtn.setOnAction(e -> {
                showTrafficLightID = !showTrafficLightID;

                if (showTrafficLightID) {
                    TrafficLightIDBtn.setStyle("-fx-background-color: #add8e6;");
                } else {
                    TrafficLightIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        // toggle view RouteID button
        if (RouteIDBtn != null) {
            RouteIDBtn.setOnAction(e -> {
                showRouteID = !showRouteID;

                if (showRouteID) {
                    RouteIDBtn.setStyle("-fx-background-color: #add8e6;");
                } else {
                    RouteIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        // toggle view VehicleID button
        if (VehicleIDBtn != null) {
            VehicleIDBtn.setOnAction(e -> {
                showVehicleID = !showVehicleID;

                if (showVehicleID) {
                    VehicleIDBtn.setStyle("-fx-background-color: #add8e6;");
                } else {
                    VehicleIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        // slider traffic lights status
        if (sliderRed != null) {
            sliderRed.valueProperty().addListener((obs, oldVal, newVal) -> {
                redValue.setText(String.format("%.2fs", newVal.doubleValue()));
                LOG.info("set Red Traffic Light to: " + String.valueOf(newVal));
            });
        }
        if (sliderGreen != null) {
            sliderGreen.valueProperty().addListener((obs, oldVal, newVal) -> {
                greenValue.setText(String.format("%.2fs", newVal.doubleValue()));
                LOG.info("set Green Traffic Light to: " + String.valueOf(newVal));
            });
        }
        if (sliderYellow != null) {
            sliderYellow.valueProperty().addListener((obs, oldVal, newVal) -> {
                yellowValue.setText(String.format("%.2fs", newVal.doubleValue()));
                LOG.info("set Yellow Traffic Light to: " + newVal.intValue());
            });
        }

        // make delay slider function
        if (delaySlider != null) {
            // Set initial value
            simulationDelay = (long) (delaySlider.getValue() * 1_000_000);

            delaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                // Convert ms to nanoseconds
                simulationDelay = (long) (newVal.doubleValue() * 1_000_000);
                delayValue.setText(String.format("%.0f", newVal.doubleValue()));
                LOG.info("Delay set to: " + newVal.intValue() + "ms");
            });
        }

        // temporarily not function, next time.
        if (inFlowSlider != null) {
            inFlowSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                inFlowValue.setText(String.format("%.1f", newVal.doubleValue()));
                LOG.info("In Flow: " + newVal);
            });
        }

        // make the maxSpeed slider function
        if (maxSpeedSlider != null) {
            maxSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double speed = newVal.doubleValue();
                maxSpeedValue.setText(String.format("%.1f m/s", speed));
                panel.setGlobalMaxSpeed(speed);
                LOG.info("Max Speed: " + speed);
            });
        }

        // auto mode is currently for fun, does no real impact
        if (autoModeToggle != null) {
            autoModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    autoModeToggle.setText("OFF");
                    autoModeToggle.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                    LOG.info("Auto mode: OFF");
                } else {
                    autoModeToggle.setText("ON");
                    autoModeToggle.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    LOG.info("Auto mode: ON");
                }
            });
        }
    }


    // button logic
    // startButton
    @FXML
    public void onStartClick() {
        LOG.info("Resuming Simulation Loop...");
        simulationLoop.start();
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
    }

    // stop button
    @FXML
    public void onStopClick() {
        LOG.info("Stopping Simulation...");
        simulationLoop.stop();
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    @FXML
    // add vehicle button function
    public void onAddVehicleClick() {
        LOG.info("Adding Vehicle...");
        // generate a unique ID for the vehicle using the current system time
        String vehId = "veh_" + System.currentTimeMillis();

        try {
            // fetch all available route IDs from the SUMO map
            List<String> routeIDs = panel.getRouteIDs();
            // stop if no routes exist in the map files
            if (routeIDs.isEmpty()) {
                System.err.println("No routes found in the map file");
                return;
            }
            // select the specific route using counter (clickRouteIndex)
            String targetRoute = routeIDs.get(clickRouteIndex);
            // get the current simulation time so the car spawns immediately
            int departure_time = (int) panel.getCurrentTime();

            // send the command to SUMO to add the vehicle with specific parameters
            // parameters: (ID, Type, Route, DepartTime, Position, Speed, LaneIndex)
            panel.addVehicle(vehId, "DEFAULT_VEHTYPE", targetRoute, departure_time, 50.0, 10.0, (byte) -2);

            System.out.println("Spawned " + vehId + " on route: " + targetRoute);
            // update the counter for the next click
            // The modulo (%) ensures that after the last route, the index resets to 0
            clickRouteIndex = (clickRouteIndex + 1) % routeIDs.size();
            // refresh the canvas to show the new vehicle immediately
            drawMap();
        } catch (Exception e) {
            // log an error if the connection to SUMO fails or the ID is invalid
            System.err.println("Failed to add vehicle: " + e.getMessage());

        }
    }

    @FXML
    public void onTurnAllOffClick() {
        LOG.info("User requested: Turning ALL Lights OFF.");
        panel.turnOffAllLights();
    }

    // Action for the new button: Turn ALL Lights ON
    @FXML
    public void onTurnAllOnClick() {
        LOG.info("User requested: Turning ALL Lights ON.");
        panel.turnOnAllLights();
    }

    @FXML
    public void turn_all_lights_red() {
        LOG.info("User requested: FORCING ALL LIGHTS TO RED.");
        panel.turn_all_light_red();
        // Note: The UI drawing will update automatically on the next simulation step
        // because it calls panel.getRedYellowGreenState()
    }

    @FXML
    public void turn_all_lights_green() {
        LOG.info("User requested: FORCING ALL LIGHTS TO GREEN.");
        panel.turn_all_light_green();
    }

    @FXML
    public void onRestoreAutoClick() {
        LOG.info("User requested: RESTORING AUTOMATIC PROGRAM.");
        // This calls the method that sets the program back to "0"
        panel.turnOnAllLights();
    }

    // stress test button
    public void onStressTestClick() {
        LOG.info("Performing Stress Test");
        final int vehicleCount = 500;
        try {
            // call the function stressTest
            panel.stressTest(vehicleCount);
            // redraw map
            drawMap();
            LOG.info("Adding " + vehicleCount + " to the simulation");
        } catch (Exception e) {
            LOG.error("Failed to perform stress test");

        }
    }

    @FXML
    // stress test 2 button
    public void onStressTest2Click() {
        // switch the state between true and false
        isStressTest2Active = !isStressTest2Active;
        // print a message to the console
        System.out.println("Stress Test 2 is now: " + (isStressTest2Active ? "Active" : "Inactive"));
    }

    @FXML
    // 3D button
    public void on3DClick() {
        SubScene subScene = mapDraw3D.getSubScene();
        if (subScene == null) {
            LOG.error("The subscene is not initialized yet");
            return;
        }
        if (mapCanvas.isVisible()) {
            mapCanvas.setVisible(false);
            subScene.setVisible(true);
        } else {
            mapCanvas.setVisible(true);
            subScene.setVisible(false);
        }

        Group roadGroup = mapDraw3D.getRoadGroup();
        boolean check = (roadGroup.getChildren().isEmpty());

        if (check) {
            mapDraw3D.mapShapes = this.mapShapes;
            mapDraw3D.drawRoad();
        }
        // make the subScene focus on the keyboard
        if (subScene.isVisible()) {
            subScene.requestFocus();
        }
    }

    @FXML
    // restart button
    public void onRestartClick() {
        // log a message to the console for debugging
        LOG.info("Restarting map...");

        // stop the animation timer so it doesn't try to draw while the map is reloading
        if (simulationLoop != null) {
            simulationLoop.stop();
        }
        // tell the backend to close the current SUMO connection and start a new one
        panel.restartSimulation();

        // re-link the traffic light logic to the new simulation connection
        tlsWrapper = panel.getTrafficLightWrapper();
        if (tlsWrapper != null) {
            tlsWrapper.isRunning = true;
            // reload the traffic light directions from the map file
            tlsWrapper.loadConnectionDirections(NET_XML_PATH);
        }

        // reset the counter so the next car added starts back at the first route
        clickRouteIndex = 0;
        // clear the canvas and draw the original map road
        drawMap();
        // reset the buttons so the user can start the simulation again
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        // log that the reset is complete
        LOG.info("Map returned to beginning state. Press Start to begin");
    }

    private void updateStats() {
        int count = panel.getVehicleCount(); // Get the vehicle count from the ControlPanel
        numberVehicles.setText("Vehicles: " + count);
        int edgeCount = panel.getEdgeCount(); // Get the edge count from the ControlPanel
        numberOfEdges.setText("Edges: " + edgeCount);
        int trafficLightCount = panel.getTrafficLightCount();
        numOfTL.setText("Traffic Lights: " + trafficLightCount);


        if (!panel.getVehicleIDs().isEmpty()) {
            double avgSpeed = panel.getGlobalMeanSpeed(); // Get Avg speed from the ControlPanel
            averageSpeed.setText(String.format("Avg Speed: %.2f km/h", avgSpeed * 3.6)); // Only take 2 decimal points and times 3.6 to be km/h rather than m/s
        }

        if (panel.isRunning()) {
            double currentSpeed = panel.getGlobalMeanSpeed(); // Get the GLOBAL mean speed (average speed for every vehicle on the map)
            currentSpeed *= 3.6; // m/s to km/h

            timeSeconds += 0.1; // X-axis to show the time
            speedSeries.getData().add(new XYChart.Data<>(timeSeconds, currentSpeed)); // Data point for the chart

            if (speedSeries.getData().size() > 50) { // Only keep the last 50 data point and remove all before it
                speedSeries.getData().remove(0);
            }
            double congestion = panel.getCongestionPercentage();
            // Color to gain insight
            String color = "green";
            if (congestion > 30) color = "orange";
            if (congestion > 60) color = "red";

            congestionDensity.setText(String.format("Density: %.1f%%", congestion));
            congestionDensity.setStyle("-fx-text-fill: " + color + ";");

            // 5. Show CO2
            double totalCO2 = panel.getTotalCO2();
            // mg/s to grams/s
            co2Emission.setText(String.format("CO2 Emission: %.2f g/s", totalCO2 / 1000.0));
        }
    }

    // step and draw map accordingly
    private void updateSimulation() {
        panel.step();
        if (isStressTest2Active) {
            // get a list of all vehicles currently active in the simulation
            List<String> vehicles = panel.getVehicleIDs();

            // Loop through every vehicle found on the map
            for (String id : vehicles) {
                // Generate a random number (0-255) for the Red, Green, and Blue channels
                int r = (int) (Math.random() * 256);
                int g = (int) (Math.random() * 256);
                int b = (int) (Math.random() * 256);

                // send the command to SUMO to change this specific vehicle's color
                panel.setColor(id, r, g, b, 255);
            }
        }
        // refresh the UI labels
        updateStats();
        // redraw the entire map, including the new positions and colors of vehicles
        drawMap();
        updateVehicle3D();
    }

    private void drawMap() {
        if (mapDraw == null) {
            return;
        }

        mapDraw.SCALE = this.SCALE;
        mapDraw.OFFSET_X = this.OFFSET_X;
        mapDraw.OFFSET_Y = this.OFFSET_Y;

        mapDraw.mapShapes = this.mapShapes;
        mapDraw.panel = this.panel;
        mapDraw.tlsWrapper = this.tlsWrapper;

        mapDraw.showEdgesID = this.showEdgesID;
        mapDraw.showVehicleID = this.showVehicleID;
        mapDraw.showRouteID = this.showRouteID;

        mapDraw.drawAll();
    }

    private void initialize3D()
    {
        if (mapDraw3D == null) {
            return;
        }

        mapDraw3D.panel = this.panel;
        mapDraw3D.mapShapes = this.mapShapes;

        boolean check1 = (mapDraw3D.getSubScene() == null);

        if (check1)
        {
            mapDraw3D.setup();
            SubScene subScene = mapDraw3D.getSubScene();
            subScene.setVisible(false);
            rootPane.getChildren().add(subScene);

            subScene.setOnKeyPressed(event ->{
                KeyCode keyInput = event.getCode();
                keyInputSet.add(keyInput);
            });

            subScene.setOnKeyReleased(event ->{
                KeyCode keyInput = event.getCode();
                keyInputSet.remove(keyInput);
            });

        }
        boolean check = mapDraw3D.allRoadBoxes.isEmpty();
        if (check)
        {
            mapDraw3D.drawRoad();
        }
    }

    private void updateVehicle3D()
    {
        mapDraw3D.updateVehicles();
    }
}