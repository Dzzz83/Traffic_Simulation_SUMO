// this file serves as the central code that connects backend (ControlPanel.java) and frontend (ui_design.java & ControlPanel.fxml)
package ui;

import de.tudresden.sumo.cmd.Edge;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;

import wrapperSUMO.ControlPanel;
import wrapperSUMO.TrafficLightWrapper;
import wrapperSUMO.TrafficConnectInfo;
import de.tudresden.sumo.objects.SumoPosition2D;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager; // use for logging
import org.apache.logging.log4j.Logger;

public class Controller {
    private static final Logger LOG = LogManager.getLogger(Controller.class.getName());

    // initialize variables
    // @FXML is a special tag that tells Java to look into fxml file and link the correct object this file
    @FXML private Canvas mapCanvas;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private Label menuIcon;
    @FXML private VBox sidebar;

    // buttons
    @FXML private Button EdgeIDBtn;
    @FXML private Button RouteIDBtn;
    @FXML private Button VehicleIDBtn;
    @FXML private Button TrafficLightIDBtn;
    @FXML private Button optimize_traffic;

    @FXML private ComboBox<String> trafficIdCombo;
    @FXML private ToggleButton autoModeToggle;

    // sliders
    @FXML private Slider delaySlider;
    @FXML private Slider inFlowSlider;
    @FXML private Slider maxSpeedSlider;
    @FXML private Slider sliderRed;
    @FXML private Slider sliderGreen;
    @FXML private Slider sliderYellow;

    // sliders values
    @FXML private Label delayValue;
    @FXML private Label inFlowValue;
    @FXML private Label maxSpeedValue;
    @FXML private Label redValue;
    @FXML private Label greenValue;
    @FXML private Label yellowValue;

    // stats
    @FXML private Label connectionStatus;
    @FXML private Label numberVehicles;
    @FXML private Label averageSpeed;
    @FXML private Label congestionDensity;
    @FXML private Label numberOfEdges;

    // chart
    @FXML private LineChart<Number, Number> avgSpeedChart;

    // traffic light
    @FXML private Label labelRed;
    @FXML private Label labelGreen;
    @FXML private Label labelYellow;
    private TrafficLightWrapper tlsWrapper;
    private static final String NET_XML_PATH = "src/map/demo.net.xml";
    // logical variables
    private ControlPanel panel;
    private TrafficLightWrapper traffic_control;
    private AnimationTimer simulationLoop;
    private Map<String, List<SumoPosition2D>> mapShapes = null;
    // SumoPosition2D represents a single point on the map (X, Y)
    // List<SumoPosition2D> represents a stroke which is a collection of points that make up a continuous line (X, Y), (X1, Y1), (X2, Y2)
    // List<List .... is a collections of lines on the map
    // initialize laneSeperators to temporary hold the dashed lines coordinates on the map
    private List<List<SumoPosition2D>> laneSeperators = new ArrayList<>();
    // initialize solidCenterLines to temporary hold all the solid lines on the map
    private List<List<SumoPosition2D>> solidCenterLines = new ArrayList<>();

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

    //variable for stress test 2 button
    private boolean isStressTest2Active = false;

    // logic variables to show/hide to sidebar
    private boolean isSidebarVisible = true;

    // variables for delay button
    private long lastUpdate = 0;
    private long simulationDelay = 100_000_000; // Default 100ms in nanoseconds

    // Semi-Transparent Black color for windshield
    private static final Color WINDSHIELD_COLOR = Color.color(0, 0, 0, 0.5);
    private static final Color ASPHALT_COLOR = Color.web("#404040");
    private static final Color GRASS_COLOR = Color.web("#2E7D32");

    // Traffic light optimization
    private boolean isOptimizationActive = false;
    private int lastPhaseIndex = -1;
    private int lastOptimizedPhase = -1;
    private String lastSelectedId = "";
    @FXML
    // initialize the GUI function
    public void initialize() {
        LOG.info("Starting the GUI ...");

        // initialize Control Panel
        panel = new ControlPanel();

        // connect to SUMO and load Map
        LOG.info("Connecting to SUMO to fetch map...");
        if (panel.startSimulation()) {
            connectionStatus.setText("Connection: Connected");
            connectionStatus.setStyle("-fx-text-fill: green");
            tlsWrapper = panel.getTrafficLightWrapper();
            tlsWrapper.isRunning = true;
            tlsWrapper.loadConnectionDirections("src/map/demo.net.xml");
        }
        else {
            connectionStatus.setText("Connection: Disconnected");
            connectionStatus.setStyle("-fx-text-fill: red");
        }
        mapShapes = panel.getMapShape();
        // traffic light
        List<String> tlsIds = panel.getTrafficLightIDs();
        trafficIdCombo.getItems().clear();
        if (tlsIds != null && !tlsIds.isEmpty())
        {
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

        // calculate, create and store the lane separators
        generateLaneSeparators();

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

        // make the edge button function
        if (EdgeIDBtn != null) {
            EdgeIDBtn.setOnAction(e -> {
                showEdgesID = !showEdgesID;

                if (showEdgesID) {
                    EdgeIDBtn.setStyle("-fx-background-color: #add8e6;");
                }
                else {
                    EdgeIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        if (TrafficLightIDBtn != null) {
            TrafficLightIDBtn.setOnAction(e -> {
                showTrafficLightID= !showTrafficLightID;

                if (showTrafficLightID) {
                    TrafficLightIDBtn.setStyle("-fx-background-color: #add8e6;");
                }
                else {
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
                }
                else {
                    RouteIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        // toggle view VehicleID button
        if (VehicleIDBtn != null) {
            VehicleIDBtn.setOnAction(e -> {
                showVehicleID= !showVehicleID;

                if (showVehicleID) {
                    VehicleIDBtn.setStyle("-fx-background-color: #add8e6;");
                }
                else {
                    VehicleIDBtn.setStyle("");
                }
                drawMap();
            });
        }

        // slider traffic lights status
        if (sliderRed != null) {
            sliderRed.valueProperty().addListener((obs, oldVal, newVal) ->{
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

        if (inFlowSlider != null) {
            inFlowSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                inFlowValue.setText(String.format("%.1f", newVal.doubleValue()));
                LOG.info("In Flow: " + newVal);
            });
        }

        if (maxSpeedSlider != null) {
            maxSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double speed = newVal.doubleValue();
                maxSpeedValue.setText(String.format("%.1f m/s", speed));
                panel.setGlobalMaxSpeed(speed);
                LOG.info("Max Speed: " + speed);
            });
        }

        // auto mode
        if (autoModeToggle != null) {
            autoModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    autoModeToggle.setText("OFF");
                    autoModeToggle.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                    LOG.info("Auto mode: OFF");
                }
                else {
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
    // add vehicle button function
    @FXML
    public void onAddVehicleClick()
    {
        LOG.info("Adding Vehicle...");
        String vehId = "veh_" + System.currentTimeMillis();

        try
        {
            List <String> routeIDs = panel.getRouteIDs();
            if (routeIDs.isEmpty()) {
                System.err.println("No routes found in the map file");
                return;
            }
            // pick the route based on counter
            String targetRoute = routeIDs.get(clickRouteIndex);
            // get current time
            int departure_time = (int) panel.getCurrentTime();
            // add vehicle
            panel.addVehicle(vehId, "DEFAULT_VEHTYPE", targetRoute, departure_time, 50.0, 10.0, (byte) -2);
            System.out.println("Spawned " + vehId + " on route: " + targetRoute);
            // update the counter for the next click
            clickRouteIndex = (clickRouteIndex + 1) % routeIDs.size();
            // redraw map
            drawMap();
        }
        catch (Exception e)
        {
            System.err.println("Failed to add vehicle: " + e.getMessage());
            e.printStackTrace();
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
    @FXML
    public void onOptimizeClick() {
        isOptimizationActive = !isOptimizationActive;

        if (isOptimizationActive) {
            // State: ACTIVE
            LOG.info("Auto-Optimization: ENABLED");
            optimize_traffic.setText("DISABLE OPTIMIZATION");
            optimize_traffic.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-font-weight: bold;"); // Amber color for "Working"
        } else {
            // State: INACTIVE
            LOG.info("Auto-Optimization: DISABLED");
            optimize_traffic.setText("ENABLE OPTIMIZATION");
            optimize_traffic.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-weight: bold;"); // Back to Purple

            // Optional: Reset last phase so it triggers immediately if re-enabled
            lastPhaseIndex = -1;
        }
    }
    // add the stress test button
    public void onStressTestClick()
    {
        LOG.info("Performing Stress Test");
        final int vehicleCount = 500;
        try
        {
            // call the function stressTest
            panel.stressTest(vehicleCount);
            // redraw map
            drawMap();
            LOG.info("Adding " + vehicleCount + " to the simulation");
        }
        catch (Exception e)
        {
            LOG.error("Failed to perform stress test");
            e.printStackTrace();
        }
    }

    @FXML
    // stress test 2 button function
    public void onStressTest2Click() {
        // toggle ON/OFF
        isStressTest2Active = !isStressTest2Active;
        System.out.println("Stress Test 2 is now: " + (isStressTest2Active ? "Active" : "Inactive"));
    }

    @FXML
    // restart button
    public void onRestartClick() {
        LOG.info("Restarting map...");

        // stop the JavaFX drawing loop first!
        if (simulationLoop != null) {
            simulationLoop.stop();
        }
        // call the restart logic in the backend
        panel.restartSimulation();
        tlsWrapper = panel.getTrafficLightWrapper();
        if (tlsWrapper != null) {
            tlsWrapper.isRunning = true;
            // reload the traffic light directions from the map file
            tlsWrapper.loadConnectionDirections(NET_XML_PATH);
        }
        // reset the UI variables
        clickRouteIndex = 0;
        // redraw the empty map
        drawMap();
        // update the UI buttons
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        LOG.info("Map returned to beginning state. Press Start to begin.");
    }

    private void updateStats() {
        int count = panel.getVehicleCount();
        numberVehicles.setText("Vehicles: " + count);
        int edgeCount = panel.getEdgeCount();
        numberOfEdges.setText("Edges: " + edgeCount);


        if (!panel.getVehicleIDs().isEmpty()) {
            double avgSpeed = panel.getGlobalMeanSpeed();
            averageSpeed.setText(String.format("Avg Speed: %.2f km/h", avgSpeed * 3.6));
        }

        if (panel.isRunning()) {
            double currentSpeed = panel.getGlobalMeanSpeed();
            currentSpeed *= 3.6;

            timeSeconds += 0.1;
            speedSeries.getData().add(new XYChart.Data<>(timeSeconds, currentSpeed));

            if (speedSeries.getData().size() > 50) {
                speedSeries.getData().remove(0);
            }
        }
    }

    // step and draw map accordingly
    private void updateSimulation() {
        panel.step();
        if (isOptimizationActive && tlsWrapper != null) {
            try {
                // Get the ID directly from the Dropdown Menu
                String selectedId = trafficIdCombo.getValue();

                // process only if a valid ID is selected
                if (selectedId != null && !selectedId.isEmpty()) {

                    if (!selectedId.equals(lastSelectedId)) {
                        lastOptimizedPhase = -1;
                        lastSelectedId = selectedId;
                    }

                    // 2. Get the current phase index
                    int currentPhase = tlsWrapper.getCurrentPhaseIndex(selectedId);

                    // check whether this is new phase
                    if (currentPhase != lastOptimizedPhase) {

                        String state = tlsWrapper.getRedYellowGreenState(selectedId);

                        if (state != null && (state.contains("G") || state.contains("g"))) {
                            LOG.info("Phase Change Detected on " + selectedId + " (Phase " + currentPhase + "). Optimizing...");
                            tlsWrapper.update_phase_based_traffic_level(selectedId);
                        }

                        // update our memory
                        lastOptimizedPhase = currentPhase;
                    }
                }
            } catch (Exception e) {
                LOG.error("Error in optimization loop: " + e.getMessage());
            }
        }
        if (isStressTest2Active) {
            // get a list of all vehicles currently present on the map
            List<String> vehicles = panel.getVehicleIDs();
            for (String id : vehicles) {
                // generate random values for R, G, B
                int r = (int) (Math.random() * 256);
                int g = (int) (Math.random() * 256);
                int b = (int) (Math.random() * 256);

                // call ControlPanel function
                panel.setColor(id, r, g, b, 255);
            }
        }
        updateStats();
        drawMap();
    }
    // a function to take raw, geometric data from SUMO and transforms into drawable pixels on JavaFx
    // take in GraphicsContext gc which will handling all the drawing-related logic and points which is a collections of (X, Y) coordinates
    public void drawPolyLine(GraphicsContext gc, List<SumoPosition2D> points)
    {
        if (points.isEmpty()) return;
        // initialize 2 arrays to store X and Y coordinates
        double[] xPoints = new double[points.size()];
        double[] yPoints = new double[points.size()];
        // loop over all the raw SUMO points and converts them to screen pixel location
        for (int i = 0; i < points.size(); i++)
        {
            // take raw X and Y points and adjust with the current zoom level and shift the coordinates based on offset
            xPoints[i] = (points.get(i).x * SCALE ) + OFFSET_X;
            yPoints[i] = mapCanvas.getHeight() - ((points.get(i).y * SCALE) + OFFSET_Y);
        }
        // take in (X, Y) and draw the line
        gc.strokePolyline(xPoints, yPoints, points.size());
    }

    // edge id button function. labels the road with its id
    public void drawEdgeLabel(GraphicsContext gc, String laneID, List<SumoPosition2D> points)
    {
        // Safety check: need at least 2 points to determine direction
        if (points.size() < 2) return;

        // Clean the ID string
        String edgeID = laneID;
        int _index = laneID.lastIndexOf('_');
            if (_index != -1) {
            edgeID = laneID.substring(0, _index);
        }

        // Find the middle segment of the road
        int midIndex = points.size() / 2;
        // Ensure it do not go out of bounds if midIndex is the last point
            if (midIndex >= points.size() - 1) {
            midIndex = points.size() - 2;
        }

        SumoPosition2D p1 = points.get(midIndex);
        SumoPosition2D p2 = points.get(midIndex + 1);

        // Calculate the direction vector (dx, dy)
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;

        // Calculate length to normalize
        double length = Math.sqrt(dx * dx + dy * dy);

            if (length == 0) return; // Prevent division by zero

        // Normalize (make length 1.0)
        double unitX = dx / length;
        double unitY = dy / length;

        // Rotate 90 degrees clockwise
        // Vector (x, y) rotated 90 deg clockwise is (y, -x)
        double normalX = unitY;
        double normalY = -unitX;

        // Calculate Offset Position
        // Shift by 6 meters to the right
        double offsetDistance = 7.0; // still experimenting

        // We use the midpoint of the segment as the base
        double worldX = ((p1.x + p2.x) / 2.0) + (normalX * offsetDistance);
        double worldY = ((p1.y + p2.y) / 2.0) + (normalY * offsetDistance);

        // Convert World Coordinates to Screen Coordinates
        // NOTE: Flip the Y-axis calculation for the text to stick to the map correctly
        double screenX = (worldX * SCALE) + OFFSET_X;
        double screenY = mapCanvas.getHeight() - ((worldY * SCALE) + OFFSET_Y);

        // Draw Text with Outline
            gc.setTextAlign(TextAlignment.CENTER);

        // Draw black outline
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2.5); // Thicker line for shadow effect
            gc.strokeText(edgeID, screenX, screenY);

        // Draw white text on top
            gc.setFill(Color.WHITE);
            gc.fillText(edgeID, screenX, screenY);
    }

    // draw map function
    private void drawMap() {
        // initialize gc
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        // fill the entire screen with green color
        gc.setFill(GRASS_COLOR);
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        // check if there is map data
        if (mapShapes == null || mapShapes.isEmpty())
        {
            return;
        }

        // draw the roads part
        // debug mode (Show Edge IDs)
        if (showEdgesID) {
            gc.setFill(Color.WHITE);  // white text
            gc.setTextAlign(TextAlignment.CENTER);

            // loop through all roads and draw them with their ID names
            for (Map.Entry<String, List<SumoPosition2D>> entry : mapShapes.entrySet()) {
                String laneID = entry.getKey();
                // Logic to extract clean Edge ID
                String edgeID = laneID;
                int _index = laneID.lastIndexOf('_');
                if (_index != -1) {
                    edgeID = laneID.substring(0, _index);
                }

                drawPolyLine(gc, entry.getValue());

                if (!edgeID.startsWith(":") && entry.getValue().size() > 1) {
                    drawEdgeLabel(gc, entry.getKey(), entry.getValue());
                }
            }
        }
        // normal mode: draw the roads normally (border --> asphalt --> white lines)
        else {
            // calculate road width relative to zoom (SCALE)
            double baseRoadWidth = Math.max(2.0, 4.5 * SCALE);
            double visualWidth = baseRoadWidth * 1.2;

            // smooth line endings
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.setLineDashes(null);

            // draw the outline, border
            gc.setStroke(Color.LIGHTGRAY);
            // draw it a little bit wider than the asphalt
            gc.setLineWidth(visualWidth + (0.5 * SCALE));

            // loop through each point and draw the border
            for (List<SumoPosition2D> points : mapShapes.values())
            {
                drawPolyLine(gc, points);
            }

            // draw the asphalt road
            gc.setStroke(ASPHALT_COLOR);
            gc.setLineWidth(visualWidth);

            // draw asphalt on top of the border
            for (List<SumoPosition2D> points: mapShapes.values())
            {
                drawPolyLine(gc, points);
            }

            // draw white lines
            if (SCALE > 0.5)
            {
                // set the width of the white line
                double lineWidth = 0.3 * SCALE;

                // draw the white dashed lines
                gc.setStroke(Color.WHITESMOKE);
                gc.setLineWidth(lineWidth);
                // make it ends squarely
                gc.setLineCap(StrokeLineCap.BUTT);
                // draw the line in 3 meters then dashes for another 3 meters
                gc.setLineDashes(3.0 * SCALE, 3.0 * SCALE);

                for (List<SumoPosition2D> points : laneSeperators) {
                    drawPolyLine(gc, points);
                }

                // continuous white lines
                // reset the line
                gc.setLineDashes(null);
                gc.setLineWidth(lineWidth);
                gc.setStroke(Color.WHITE);


                for (List<SumoPosition2D> points : solidCenterLines) {
                    drawPolyLine(gc, points);
                }

                // Cleanup: Reset settings for next drawing
                gc.setLineDashes(null);
                gc.setLineCap(StrokeLineCap.ROUND);
            }
        }

        // draw traffic light
        if (tlsWrapper != null && panel.isRunning()) {
            drawTrafficLights(gc);
        }


        // draw vehicles
        // check if simulation is running
        if (panel.isRunning())
        {
            // get the list of vehicles
            List<String> vehicles = panel.getVehicleIDs();

            // calculate car size
            double carLength = Math.max(8.0, 4.5 * SCALE);
            double carWidth = Math.max(4.0, 2.0 * SCALE);

            // loop through every vehicle in the simulation
            for (String id: vehicles)
            {
                // get the position
                SumoPosition2D pos = panel.getPosition(id);
                // convert to screen pixel
                double x = (pos.x * SCALE) + OFFSET_X;
                double y = mapCanvas.getHeight() - ((pos.y * SCALE) + OFFSET_Y);

                // get the angle of the vehicle
                double angle = panel.getVehicleAngle(id);

                // fetch the dynamic color from the Stress Test
                Color vehColor = panel.getVehicleColor(id);
                gc.setFill(vehColor);

                // call draw car function
                drawCar(gc, x, y, angle, carLength, carWidth);

                if (showVehicleID) {
                    gc.setFill(Color.LIME);
                    gc.fillText(id, x, y - 8); // Above vehicle
                }

                if (showRouteID) {
                    String routeID = panel.getVehicleRouteID(id);

                    gc.setFill(Color.GREEN);
                    gc.fillText(routeID, x, y + 15);

                    String currentEdgeID = panel.getRoadID(id);

                    String laneID = currentEdgeID.startsWith(":") ? currentEdgeID + "_0" : currentEdgeID + "_0";

                    if (mapShapes.containsKey(laneID)) {
                        List<SumoPosition2D> roadPoints = mapShapes.get(laneID);
                        if (roadPoints != null && roadPoints.size() > 1) {
                            int mid = roadPoints.size() / 2;
                            double roadX = (roadPoints.get(mid).x * SCALE) + OFFSET_X;
                            double roadY = mapCanvas.getHeight() - ((roadPoints.get(mid).y * SCALE) + OFFSET_Y);

                            gc.setFill(Color.MAGENTA);
                            gc.fillText(routeID, roadX, roadY);
                        }
                    }
                }
            }
        }
    }


// main method for draw traffic lights based on its incoming edge
    private void drawTrafficLights(GraphicsContext gc) {
        if (tlsWrapper == null || !panel.isRunning) return;
        // If map is zoomed out too far, don't draw anything to keep it clean.
        if (SCALE < 0.2) return;

        List<String> trafficLightIds = tlsWrapper.getTrafficLightIDs();

        for (String trafficid : trafficLightIds) {
            Map<String, List<TrafficConnectInfo>> connectionsByEdge = tlsWrapper.get_traffic_connections(trafficid);
            Map<String, List<SumoPosition2D>> edgeGeometry = panel.get_traffic_light_pos(trafficid);
            int secondsLeft = (int) tlsWrapper.getRemainingTimeForConnection(trafficid);

            for (Map.Entry<String, List<TrafficConnectInfo>> edgeEntry : connectionsByEdge.entrySet()) {
                String incomingEdgeID = edgeEntry.getKey();
                List<TrafficConnectInfo> edgeConnections = edgeEntry.getValue();

                if (edgeGeometry.containsKey(incomingEdgeID)) {
                    List<SumoPosition2D> shape = edgeGeometry.get(incomingEdgeID);
                    if (shape == null || shape.isEmpty() || shape.size() < 2) continue;

                    // A. Calculate Position (Stop Line)
                    SumoPosition2D stopPos = shape.get(shape.size() - 1);
                    double drawX = (stopPos.x * SCALE) + OFFSET_X;
                    double drawY = mapCanvas.getHeight() - ((stopPos.y * SCALE) + OFFSET_Y);

                    // B. Calculate Rotation (Perpendicular to the road end)
                    SumoPosition2D prevPos = shape.get(shape.size() - 2);
                    double prevX = (prevPos.x * SCALE) + OFFSET_X;
                    double prevY = mapCanvas.getHeight() - ((prevPos.y * SCALE) + OFFSET_Y);

                    // Angle of the road in degrees
                    double angle = Math.toDegrees(Math.atan2(drawY - prevY, drawX - prevX));

                    // We want the box perpendicular.
                    // If road goes Right (0 deg), Box should be Vertical (90 deg).
                    // So we rotate by (RoadAngle + 90).
                    double rotation = angle + 90;

                    // C. Draw with Transformation
                    gc.save(); // Save current state (non-rotated)
                    gc.translate(drawX, drawY); // Move origin to the stop line
                    gc.rotate(rotation); // Rotate the entire context

                    // draw at (0,0)
                    drawDirectionalTrafficLight(gc, 0, 0, secondsLeft, edgeConnections, trafficid);

                    gc.restore(); // Restore state for next iteration
                }
            }
        }
    }

    // drawing box with arrow
    private void drawDirectionalTrafficLight(GraphicsContext gc, double x, double y, int secondsLeft, List<TrafficConnectInfo> connections, String trafficId) {
        if (connections.isEmpty()) return;

        // If zoomed out (SCALE < 0.6), draw a smaller
        boolean isDetailed = SCALE > 0.6;

        // 1. Organize Slots
        Map<String, TrafficConnectInfo> slotMap = new HashMap<>();
        for (TrafficConnectInfo info : connections) slotMap.putIfAbsent(info.getDirection().toLowerCase(), info);

        double sizeFactor = Math.min(1.0, Math.max(0.6, SCALE)); // Clamp between 0.6x and 1.0x size

        double lightRadius = (isDetailed ? 4 : 2.5) * sizeFactor;
        double padding = 3 * sizeFactor;
        double spacing = 2 * sizeFactor;
        double slotSize = (lightRadius * 2);
        double slotStep = slotSize + spacing;

        String[] directions = {"t", "l", "s", "r"}; // The visual order

        double boxWidth = (slotStep * 4) + padding;
        double boxHeight = slotSize + (padding * 2);

        if (isDetailed) boxHeight += (10 * sizeFactor);

        // Draw centered on (x,y) - remember (x,y) is (0,0) relative to rotation
        double startX = x - (boxWidth / 2);
        double startY = y; // Draw "above" the stop line (relative to rotation)

        // 3. Draw Housing
        gc.setFill(Color.web("#222222")); // Softer black
        gc.setStroke(Color.web("#111111"));
        gc.setLineWidth(0.5);
        // Using a large corner radius creates a "Capsule" or "Pill" shape
        gc.fillRoundRect(startX, startY, boxWidth, boxHeight, 10 * sizeFactor, 10 * sizeFactor);
        gc.strokeRoundRect(startX, startY, boxWidth, boxHeight, 10 * sizeFactor, 10 * sizeFactor);

        // 4. Draw Signals
        double currentX = startX + padding + lightRadius;
        double lightY = startY + padding + lightRadius;

        for (String dir : directions) {
            TrafficConnectInfo info = slotMap.get(dir);
            Color arrowColor = Color.web("#444444"); // Default Inactive (Dark Grey)

            if (info != null) {
                char state = tlsWrapper.getStateForConnection(trafficId, info.getLinkIndex());
                // Map State to Neon Colors
                if (state == 'r' || state == 'R') arrowColor = Color.web("#FF3333"); // Neon Red
                else if (state == 'y' || state == 'Y') arrowColor = Color.web("#FFCC00"); // Warm Yellow
                else if (state == 'g' || state == 'G') arrowColor = Color.web("#00FF66"); // Neon Green
            }

            // Draw The Arrow
            if (isDetailed) {
                // Draw actual arrow shape
                drawArrow(gc, currentX, lightY, lightRadius, dir, arrowColor);
            } else {
                // Zoomed out: draw a small dot
                gc.setFill(arrowColor);
                gc.fillOval(currentX - lightRadius, lightY - lightRadius, lightRadius * 2, lightRadius * 2);
            }

            // Draw Timer
            if (isDetailed && secondsLeft >= 0 && info != null) {
                gc.setFill(Color.web("#DDDDDD"));
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8 * sizeFactor));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.valueOf(secondsLeft), currentX, startY + boxHeight - padding);
            }

            currentX += slotStep;
        }
    }

    //draw arrow in traffic signal
    private void drawArrow(GraphicsContext gc, double cx, double cy, double r, String dir, Color color) {
        gc.setFill(color);
        gc.setStroke(color);
        gc.setLineWidth(1.5);

        // Simple geometric drawing based on direction
        switch (dir) {
            case "s": // Straight
                gc.fillPolygon(
                        new double[]{cx, cx - r, cx + r},
                        new double[]{cy - r, cy + r, cy + r},
                        3
                );
                break;
            case "l": // Left
                gc.fillPolygon(
                        new double[]{cx - r, cx + r/2, cx + r/2},
                        new double[]{cy, cy - r, cy + r},
                        3
                );
                break;
            case "r": // Right
                gc.fillPolygon(
                        new double[]{cx + r, cx - r/2, cx - r/2},
                        new double[]{cy, cy - r, cy + r},
                        3
                );
                break;
            case "t":
                // Turn around
                gc.setFill(Color.TRANSPARENT); // Outline only for the hook
                gc.beginPath();
                gc.moveTo(cx + r/2, cy - r/2);
                gc.arcTo(cx - r, cy - r/2, cx - r, cy + r, r); // The Curve
                gc.stroke();
                // Little tip
                gc.setFill(color);
                gc.fillPolygon(
                        new double[]{cx + r/2, cx, cx + r},
                        new double[]{cy + r/2, cy + r, cy + r},
                        3
                );
                break;
            default: // Fallback circle
                gc.fillOval(cx - r, cy - r, r*2, r*2);
        }
    }

    // calculates a smooth parallel line on the left to draw a straight or curved line that is always parallel to the road
    // what this function does is take the center line of the leftmost lane of the current direction and calculate to help draw a line
    // which can be dashed line or solid line 1.6 meters to the left and it will always be parallel to the center line
    // accepts the list of lane center points and the shift distance
    private List<SumoPosition2D> calculateLeftBorder(List<SumoPosition2D> points, double offset) {
        // create a list to store the coordinates of the parallel line
        List<SumoPosition2D> shiftedLine = new ArrayList<>();
        // if the input has less than 2 points then can't calculate
        if (points.size() < 2)
        {
            return shiftedLine;
        }

        for (int i = 0; i < points.size(); i++) {
            // store the Normal Vector component
            double avgNx = 0, avgNy = 0;

            // 1. Normal from Previous Segment (i & i-1)
            // check if already go past the first point
            if (i > 0) {
                // calculate the distance between the 2 points
                // Ex: (Xi, Yi) = (5, 5)
                // (Xi-1, Yi-1) = (4, 5)
                double dx = points.get(i).x - points.get(i - 1).x; // dx = Xi - Xi-1 = 1
                double dy = points.get(i).y - points.get(i - 1).y; // dy = Yi - Yi-1 = 0
                // calculate the length of the segment
                double len = Math.sqrt(dx*dx + dy*dy); // sqrt(1 + 0) = 1
                // check if the length is greater than zero
                // calculate the perpendicular vector
                if (len > 0) {

                    avgNx -= dy / len; // -= (0/1) ==> avgNx = 0 - 0 = 0
                    avgNy += dx / len; // += (1/1) ==> avgNx = 0 + 1 = 1
                    // ==> new vector = (0,1)
                }
            }

            // 2. Normal from Next Segment (i & i+1)
            // check if i is not the last point of the road
            if (i < points.size() - 1) {
                // calculate the distancy between the 2 points
                // Ex: (Xi, Yi) = (5, 5)
                // (Xi+1, Yi+1) = (6, 5)
                double dx = points.get(i + 1).x - points.get(i).x; // dx = 1
                double dy = points.get(i + 1).y - points.get(i).y; // dy = 0
                // calculate the length of the segment
                double len = Math.sqrt(dx*dx + dy*dy); // len = 1
                // check if the length is greater than zero
                if (len > 0) {
                    // calculate the perpendicular vector
                    avgNx -= dy / len; // ==> avgNx = 0
                    avgNy += dx / len; // ==> avgNx = 2
                }
            }

            // 3. Average the normals for a smooth curve
            double len = Math.sqrt(avgNx*avgNx + avgNy*avgNy); // len = 2
            if (len > 0) {
                avgNx /= len; // ==> avgNx = 0
                avgNy /= len; // ==> avgNy = 1
            }

            // 4. Apply Offset
            // (Xi, Yi) = (5, 5)
            // (avgNx, avgNy) = (0, 1)
            double newX = points.get(i).x + (avgNx * offset); // newX = 5.0
            double newY = points.get(i).y + (avgNy * offset); // newY = 6.6

            // add the new points to the shiftedLine
            shiftedLine.add(new SumoPosition2D(newX, newY));
        }
        return shiftedLine;
    }

    // Draws car with rotation
    // take in gc, (x,y) points of the car, its angle, its size (length, width)
    private void drawCar(GraphicsContext gc, double x, double y, double angle, double length, double width) {
        // save the current state
        gc.save();

        // make the car's postion as (0, 0) temporary for easier drawing
        // move the pen and grid to the car's center
        gc.translate(x, y);

        // rotate the grid corresponding to the direction of the car
        gc.rotate(angle);

        // draw car body (Centered at 0,0)
        // width is X, length is Y. We draw it pointing UP (-y) because in JavFX, the Y-axis starts at the top and increases as the car
        // moving down so the car must move (-y) if it is moving "up" in the screen.
        // fill the car's color
        //gc.setFill(Color.RED);
        // set the color for the outline
        gc.setStroke(Color.BLACK);
        // set the thickness of the outline
        gc.setLineWidth(1.0);

        // Draw the main body rectangle (*)
        gc.fillRoundRect(-width / 2, -length / 2, width, length, 3, 3);
        gc.strokeRoundRect(-width / 2, -length / 2, width, length, 3, 3);

        // draw windshield
        // draw a dark box near the "front" (which is up, -y)
        gc.setFill(WINDSHIELD_COLOR); // Semi-transparent black
        // (*)
        gc.fillRoundRect(-width / 2 + 1, -length / 2 + 2, width - 2, length / 4, 2, 2);

        // headlights
        gc.setFill(Color.WHITE); // headlight color
        double lightSize = width / 4;
        gc.fillOval(-width / 2 + 1, -length / 2, lightSize, lightSize); // left headlight
        gc.fillOval(width / 2 - 1 - lightSize, -length / 2, lightSize, lightSize); // right headline

        gc.restore(); // restore state so next car isn't messed up
    }
    // function to generate dashed lines and solid lines
    private void generateLaneSeparators() {
        // clear the old data
        laneSeperators.clear();
        solidCenterLines.clear();

        // check mapShapes
        if (mapShapes == null)
        {
            return;
        }

        // create a multi-level hash map that contains the outer map key: <string> edgeID (Ex: edge0, edge1)
        // Inner map: <Integer> lane index (Ex: 0, 1, 2)
        // Innermost value : List<SumoPosition2D> which is the center line coordinates for a specific lane
        Map<String, Map<Integer, List<SumoPosition2D>>> edgeGroups = new HashMap<>();

        // Group lanes by Edge ID
        // mapShapes hold the raw geometry data from SUMO which contains laneID <String> (Ex: "edge0_0") and List<SumoPosition2D> which
        // is list of coordinates of the center line (X1,Y1) (X2, Y2) ....
        // Map stores its data as pairs of (Key, Value)
        // entrySet() returns a collections of pairs which can be iterated over
        // ==> data = ("edge0_0", List A)
        for (Map.Entry<String, List<SumoPosition2D>> data: mapShapes.entrySet()) {
            // get the key value which is the laneID ("edge0_0")
            String laneID = data.getKey();
            // skip any internal lanes (starts with :)
            if (laneID.startsWith(":"))
            {
                continue;
            }
            // get the last index of "_" in the string which is index 5 in "edge0_0"
            int underscoreIndex = laneID.lastIndexOf("_");
            // check if there is underscore
            if (underscoreIndex != -1) {
                // get the edgeID only ("edge0")
                String edgeID = laneID.substring(0, underscoreIndex);
                // get the lane number only ("0")
                String indexStr = laneID.substring(underscoreIndex + 1);
                try {
                    // convert string to int
                    int index = Integer.parseInt(indexStr);
                    // get the map for a road ("edge0")
                    // Ex: innerMap will contains (Key, Value) ==> (0, [(X1, Y1), (X2, Y2), ....]
                    Map<Integer, List<SumoPosition2D>> innerMap = edgeGroups.get(edgeID);

                    // check if the innerMap for that edge exists
                    if (innerMap == null)
                    {
                        // create a tree map for this edge
                        innerMap = new TreeMap<>();

                        // put the empty inner map with the tag "edgeID"
                        /*
                         edgeGroups = {
                           "edgeA": { ... },
                           "edgeB": { ... },
                           "edge0": [ empty ]
                        }
                         */
                        edgeGroups.put(edgeID, innerMap);
                    }

                    // add the current lane's data to it.
                    // get the index "0" and get the points in that edge and put in the inner map
                    /*
                         edgeGroups = {
                           "edgeA": { ... },
                           "edgeB": { ... },
                           "edge0": [ ... ]
                        }
                         */
                    innerMap.put(index, data.getValue());

                }
                catch (NumberFormatException e)
                {
                    LOG.error("Something went wrong");
                }
            }
        }

        // prevents drawing two overlapping dashed lines for 2-way roads
        // create a set to saves the already drawn line
        Set<String> alreadyDrawDashed = new HashSet<>();

        // loop over every single lanes one by one
        for (Map<Integer, List<SumoPosition2D>> road : edgeGroups.values()) {
            // check if road has data
            if (road.isEmpty())
            {
                continue;
            }

            // 1. Internal Separators (Between lanes of the same direction)
            // only do this code if road has 2 or more lane
            if (road.size() >= 2) {
                // get the set of keys (0, 1, 2)
                Set<Integer> keys = road.keySet();

                // create an empty array of the right size
                Integer[] idxs = new Integer[keys.size()];

                // put the keys in the array
                int i = 0;
                for (Integer key : keys) {
                    idxs[i] = key;
                    i++;
                }

                // loop over each gap in the lane
                for (i = 0; i < idxs.length - 1; i++) {
                    // get the raw list of coordinates for the left lane
                    List<SumoPosition2D> laneA = road.get(idxs[i]);
                    // get the raw list of coordinates for the right lane
                    List<SumoPosition2D> laneB = road.get(idxs[i+1]);

                    // initialize an array list to store the dashlines
                    List<SumoPosition2D> separator = new ArrayList<>();

                    // lanes in the same roads should have the same number of points but sometimes there is error so get the minimum
                    // number of points between 2 lanes to prevent crashing
                    int numPoints = Math.min(laneA.size(), laneB.size());
                    // calculate the middle coordinates between the lanes
                    for (i = 0; i < numPoints; i++) {
                        double midX = (laneA.get(i).x + laneB.get(i).x) / 2.0;
                        double midY = (laneA.get(i).y + laneB.get(i).y) / 2.0;
                        // add the calculated points into the list
                        separator.add(new SumoPosition2D(midX, midY));
                    }
                    // add the list into laneSeparator
                    laneSeperators.add(separator);
                }
            }

            // find the highest index
            int maxIndex = ((TreeMap<Integer, List<SumoPosition2D>>) road).lastKey();
            // the lane with the highest index is the leftmost lane
            List<SumoPosition2D> leftmostLane = road.get(maxIndex);

            // calculate the parallel line to the left with offset 1.6m
            List<SumoPosition2D> leftParallelLine = calculateLeftBorder(leftmostLane, 1.6);

            // check if a road has multiple lanes (e.g: highways)
            if (road.size() > 1)
            {
                // draw the continuous white line
                solidCenterLines.add(leftParallelLine);
            }
            else
            {
                // 2 lanes road
                // check if there is data in leftParalleLine
                if (!leftParallelLine.isEmpty()) {
                    // picks the middle point of the leftParalleLine
                    int midIdx = leftParallelLine.size() / 2;
                    SumoPosition2D mid = leftParallelLine.get(midIdx);

                    // creating an unique key
                    // round the X and Y coordinates and store them as a string
                    // Ex: X = 501.999 ; Y = 199,999
                    // round ==> 502_200
                    String key = Math.round(mid.x) + "_" + Math.round(mid.y);

                    // add the key into the list
                    if (alreadyDrawDashed.add(key)) {
                        laneSeperators.add(leftParallelLine);
                    }
                }
            }
        }
    }
}