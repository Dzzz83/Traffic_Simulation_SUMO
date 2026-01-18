// this file serves as the central code that connects backend (ControlPanel.java) and frontend (ui_design.java & ControlPanel.fxml)
package app;

import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.text.FontWeight;

import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.StrokeLineCap;
import wrapperSUMO.ControlPanel;
import wrapperSUMO.TrafficLightWrapper;
import de.tudresden.sumo.objects.SumoPosition2D;
import javafx.scene.control.Label;

// import java.awt.*;
import javafx.scene.text.Font;
import java.security.spec.ECField;
import java.util.*;
import java.awt.geom.Line2D;
import java.util.List;


import org.apache.logging.log4j.LogManager; // use for logging
import org.apache.logging.log4j.Logger;

public class Controller {
    /**
     * Logger instance for the Controller.
     * Used for recording simulation lifecycle events (start, stop, errors) and debugging info.
     */
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
    private Button addVehicleBtn;
    @FXML
    private Button TrafficLightIDBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Button exportPdfBtn;

    @FXML
    private ComboBox<String> trafficIdCombo;
    @FXML
    private ToggleButton autoModeToggle;

    // vehicle numbers form
    @FXML
    private VBox vehicleInputForm;
    @FXML
    private javafx.scene.control.TextField vehicleCountInput;
    // vehicle type
    @FXML
    private ComboBox<String> vehicleTypeCombo;
    // filter vehicle
    @FXML
    private ComboBox<String> filterVehicleCombo;

    // sliders
    /**
     * Slider for controlling the simulation delay (speed).
     * Values range from 0ms (fastest) to 1000ms (slowest).
     */
    @FXML
    private Slider delaySlider;
    /**
     * Slider for setting the global maximum speed limit for all edges.
     * Modifies the speed limit in the SUMO simulation in real-time.
     */
    @FXML
    private Slider maxSpeedSlider;
    /**
     * Slider for adjusting the phase duration of the currently selected traffic light.
     * Allows the user to manually override the duration of the current green/red phase.
     */
    @FXML
    private Slider sliderPhaseDuration;
    @FXML
    private Label labelPhaseDuration;

    // sliders values
    @FXML
    private Label delayValue;
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
    @FXML
    private Label congestionHotspot;

    // chart
    @FXML
    private LineChart<Number, Number> avgSpeedChart;
    @FXML
    private BarChart<String, Number> waitingTimeChart;

    // traffic light
    @FXML
    private Label labelRed;
    @FXML
    private Label labelGreen;
    @FXML
    private Label labelYellow;
    @FXML private Button optimize_all_traffic;

    @FXML
    private Label XYZCoord;

    // traffic light
    private boolean isOptimizationActive = false;
    private boolean isGlobalOptimizationActive = false;
    @FXML private Button optimize_traffic;
    private int lastOptimizedPhase = -1;
    private String lastSelectedId = "";
    private TrafficLightWrapper tlsWrapper;
    private static final String NET_XML_PATH = "src/SumoConfig/demo.net.xml";
    private Map<String, SumoPosition2D> trafficLightPositions = new HashMap<>();
    private boolean arePositionsLoaded = false;
    // Interaction states (traffic light)
    private String hoveredTrafficLightId = null;
    private boolean isUserDraggingSlider = false;

    // logical variables
    private ControlPanel panel;
    private AnimationTimer simulationLoop;
    private Map<String, List<SumoPosition2D>> mapShapes = null;

    private MapDraw mapDraw;
    private MapDraw3D mapDraw3D;

    private MapRenderer currentRenderer;

    // variables to highlight edgeID on the map
    private boolean showEdgesID = false;
    private boolean showTrafficLightID = false;
    private boolean showRouteID = false;
    private boolean showVehicleID = false;

    // variables for line chart
    private XYChart.Series<Number, Number> speedSeries;
    private double timeSeconds = 0;

    // variables for bar chart
    private XYChart.Series<String, Number> timeDataSeries;

    // Data history storage
    private List<SimulationStats> sessionHistory = new LinkedList<>(); // can only hold SimulationStats objects and use linkedlist because it can grow dynamically
    private ReportManager reportManager = new ReportManager();
    private boolean isRecording = false;

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

    private List<Double> cameraCoords = new ArrayList<>();

    private boolean isSpawnMode = false;
    private String hoveredEdge = null;
    private int hoveredSegmentIndex = -1;
    private List<String> selectedRouteEdges = new ArrayList<>();
    private List<String> validNextEdges = new ArrayList<>();

    @FXML
    // initialize the GUI function
    public void initialize() {
        LOG.info("Starting the GUI ...");

        // initialize Control Panel
        panel = new ControlPanel();

        mapDraw = new MapDraw(mapCanvas);
        mapDraw3D = new MapDraw3D();

        currentRenderer = mapDraw;
        mapDraw.panel = panel;
        mapDraw3D.panel = panel;

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

        // setup line chart
        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Real-time Speed");
        avgSpeedChart.getData().add(speedSeries);

        // setup bar chart
        timeDataSeries = new XYChart.Series<>();
        timeDataSeries.setName("Waiting Time Distribution");
        timeDataSeries.getData().add(new XYChart.Data<>("<30s", 0));
        timeDataSeries.getData().add(new XYChart.Data<>("30-60s", 0));
        timeDataSeries.getData().add(new XYChart.Data<>(">60s", 0));
        waitingTimeChart.getData().add(timeDataSeries);
        waitingTimeChart.setAnimated(false);

        // traffic light
        List<String> tlsIds = panel.getTrafficLightIDs();
        if (tlsIds != null) {
            for (String id : tlsIds) {
                SumoPosition2D pos = tlsWrapper.getTrafficLightPosition(id);
                if (pos != null) {
                    trafficLightPositions.put(id, pos);
                }
            }
            arePositionsLoaded = true;
        }
        trafficIdCombo.getItems().clear();
        if (tlsIds != null && !tlsIds.isEmpty()) {
            trafficIdCombo.getItems().addAll(tlsIds);
            trafficIdCombo.getSelectionModel().selectFirst();
        }

        autoModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            String selectedId = trafficIdCombo.getValue();
            if (selectedId == null) return;

            if (newVal) {
                autoModeToggle.setText("ON");
                autoModeToggle.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                // Call backend:
                panel.turnOnTrafficLight(selectedId);
            } else {
                // Button is off -> disable Lights "off"
                autoModeToggle.setText("OFF");
                autoModeToggle.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                panel.turnOffTrafficLight(selectedId);
            }
        });

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

                    // control the camera in 3D
                    if (currentRenderer instanceof MapDraw3D) {
                        ((MapDraw3D) currentRenderer).updateCamera(keyInputSet);
                        currentRenderer.drawAll();
                    }
                    // same but for 2D
                    else {
                        drawMap();
                    }

                    // display the coords
                    displayCoords();

                    // only update if enough time has passed
                    if (now - lastUpdate >= simulationDelay) {
                        updateSimulation();
                        lastUpdate = now;
                        if (!isUserDraggingSlider && sliderPhaseDuration != null) {
                            String selectedId = trafficIdCombo.getValue();

                            // Check if a valid ID is selected and we have the wrapper
                            if (selectedId != null && tlsWrapper != null) {
                                double remaining = tlsWrapper.getRemainingTimeForConnection(selectedId);

                                // Update the slider and label automatically
                                sliderPhaseDuration.setValue(remaining);
                                labelPhaseDuration.setText(String.format("%.1fs", remaining));
                            }
                        }
                    }

                }
            }
        };
        // redefine vehicle types that appear on the inject vehicle form
        vehicleTypeCombo.getItems().addAll(
                "DEFAULT_VEHTYPE",
                "Delivery",
                "DEFAULT_TAXITYPE",
                "Evehicle"
        );
        // default the first type
        vehicleTypeCombo.getSelectionModel().selectFirst();

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

        mapCanvas.setOnMouseMoved(event -> {
            if (isSpawnMode) {
                String detectedLane = findEdge(event.getX(), event.getY());

                String detectedEdge = null;
                if (detectedLane != null) {
                    detectedEdge = detectedLane;
                    if (detectedLane.contains("_") && !detectedLane.startsWith(":")) {
                        int lastUnderscore = detectedLane.lastIndexOf('_');
                        if (detectedLane.substring(lastUnderscore + 1).matches("\\d+")) {
                            detectedEdge = detectedLane.substring(0, lastUnderscore);
                        }
                    }
                }

                boolean isValid = false;
                if (detectedEdge != null) {
                    if (selectedRouteEdges.isEmpty()) {
                        isValid = true;
                    }
                    else if (validNextEdges.contains(detectedEdge)) {
                        isValid = true;
                    }
                }

                String newHover = isValid ? detectedLane : null;

                if ((newHover != null && !newHover.equals(hoveredEdge)) ||
                        (newHover == null && hoveredEdge != null) ||
                        (newHover != null && hoveredSegmentIndex != -1)) {

                    hoveredEdge = newHover;
                    drawMap();
                }
            }
            // traffic light hover
            if (arePositionsLoaded) {
                // Reverse the math from MapDraw to find Mouse position in SUMO
                double mouseX = event.getX();
                double mouseY = event.getY();
                double worldMouseX = (mouseX - OFFSET_X) / SCALE;
                double worldMouseY = ((mapCanvas.getHeight() - mouseY) - OFFSET_Y) / SCALE;

                String foundId = null;
                double detectionRadius = 30.0;

                for (Map.Entry<String, SumoPosition2D> entry : trafficLightPositions.entrySet()) {
                    SumoPosition2D pos = entry.getValue();
                    // Calculate distance in World Coordinates
                    double dist = Math.sqrt(Math.pow(worldMouseX - pos.x, 2) + Math.pow(worldMouseY - pos.y, 2));

                    if (dist < detectionRadius) {
                        foundId = entry.getKey();
                        break;
                    }
                }
                boolean changed = (foundId == null && hoveredTrafficLightId != null) ||
                        (foundId != null && !foundId.equals(hoveredTrafficLightId));

                if (changed) {
                    hoveredTrafficLightId = foundId;

                    // Change cursor to HAND if hovering over a light
                    if (hoveredTrafficLightId != null) {
                        mapCanvas.setCursor(javafx.scene.Cursor.HAND);
                    } else {
                        mapCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
                    }

                    drawMap();
                }
            }
        });

        // 5. Mouse click interaction
        mapCanvas.setOnMouseClicked(event -> {
            if (isSpawnMode && hoveredEdge != null) {
                String currentEdgeId = hoveredEdge;
                if (hoveredEdge.contains("_") && !hoveredEdge.startsWith(":")) {
                    int lastUnderscore = hoveredEdge.lastIndexOf('_');
                    if (hoveredEdge.substring(lastUnderscore + 1).matches("\\d+")) {
                        currentEdgeId = hoveredEdge.substring(0, lastUnderscore);
                    }
                }
                if (selectedRouteEdges.isEmpty() || !selectedRouteEdges.getLast().equals(currentEdgeId)) {
                    selectedRouteEdges.add(currentEdgeId);
                    LOG.info("Added edge " + currentEdgeId);
                    validNextEdges = panel.getValidEdges(currentEdgeId);
                    addVehicleBtn.setText("Add Vehicle(" + selectedRouteEdges.size() + ")");
                    addVehicleBtn.setStyle("-fx-background-color: #4CAF50;");
                    drawMap();
                }
            }

            else if (hoveredTrafficLightId != null) {
                trafficIdCombo.setValue(hoveredTrafficLightId);
                LOG.info("Map Clicked: Selected Traffic Light " + hoveredTrafficLightId);
                drawMap();
            }
            else if (hoveredTrafficLightId != null) {
                trafficIdCombo.setValue(hoveredTrafficLightId);
                LOG.info("Syncing Dropdown: Map Clicked -> " + hoveredTrafficLightId);

                drawMap();
            }
        });
    }

    /**
     * Initializes UI controls and listeners for interactive components.
     * <p>
     * Sets up listeners for:
     * <ul>
     * <li>{@code delaySlider}: Updates the simulation loop delay.</li>
     * <li>{@code maxSpeedSlider}: Sends global speed limit commands to SUMO.</li>
     * <li>{@code sliderPhaseDuration}: Updates traffic light phase timings on release.</li>
     * </ul>
     * </p>
     */
    private void setupControls() {
        startBtn.setOnAction(e -> onStartClick());
        stopBtn.setOnAction(e -> onStopClick());

        /*
         * Toggles the display of Edge IDs on the map.
         * Updates the button style to indicate active state and triggers a map redraw.
         */
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
        if (sliderPhaseDuration != null) {
            // When user clicks the slider -> stop auto-updating
            sliderPhaseDuration.setOnMousePressed(e -> {
                isUserDraggingSlider = true;
            });

            // when user releases the slider -> send command to SUMO
            sliderPhaseDuration.setOnMouseReleased(e -> {
                isUserDraggingSlider = false;

                String selectedId = trafficIdCombo.getValue();
                if (selectedId != null && panel != null) {
                    double newDuration = sliderPhaseDuration.getValue();
                    // Call the new helper method we added to ControlPanel
                    tlsWrapper.setPhaseDuration(selectedId, newDuration);
                }
            });

            // Visual update while dragging
            sliderPhaseDuration.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (labelPhaseDuration != null) {
                    labelPhaseDuration.setText(String.format("%.1fs", newVal.doubleValue()));
                }
            });
        }
        // make delay slider function
        if (delaySlider != null) {
            // Set initial value
            simulationDelay = (long) (delaySlider.getValue() * 1_000_000);

            delaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                simulationDelay = (long) (newVal.doubleValue() * 1_000_000);
                delayValue.setText(String.format("%.0f", newVal.doubleValue()));
                LOG.info("Delay set to: " + newVal.intValue() + "ms");
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
        //
        filterVehicleCombo.getItems().addAll("All", "Passenger", "Taxi", "Delivery", "Evehicle");
        filterVehicleCombo.getSelectionModel().select("All");
    }

    /**
     * Handling the event when the vehicle filter ComboBox value changes.
     * <p>
     * Updates the {@link MapDraw} instance with the selected filter type
     * (e.g., limiting view to only "Electric Vehicles").
     * </p>
     *
     * @param event The ActionEvent triggered by the ComboBox selection.
     */
    @FXML
    public void onFilterVehicleChange(ActionEvent event) {
        String selectedType = filterVehicleCombo.getValue();

        if (mapDraw != null) {
            mapDraw.setVehicleFilter(selectedType);
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

    // Helper methods to detect and highlight the edge
    private double screenToWorldY(double screenY) {
        return ((mapCanvas.getHeight() - screenY) - OFFSET_Y) / SCALE;
    }

    private double screenToWorldX(double screenX) {
        return (screenX - OFFSET_X) / SCALE;
    }

    private double worldToScreenX(double worldX) {
        return (worldX * SCALE) + OFFSET_X;
    }

    private double worldToScreenY(double worldY) {
        return mapCanvas.getHeight() - ((worldY * SCALE) + OFFSET_Y);
    }

    private String findEdge(double screenX, double screenY) {
        if (mapShapes == null) return null;

        double worldX = screenToWorldX(screenX);
        double worldY = screenToWorldY(screenY);

        double minDistance = 15.0;
        String bestEdge = null;
        int bestIndex = -1;

        for (Map.Entry<String, List<SumoPosition2D>> entry : mapShapes.entrySet()) {
            List<SumoPosition2D> points = entry.getValue();

            for (int i = 0; i < points.size() - 1; i++) {
                SumoPosition2D p1 = points.get(i);
                SumoPosition2D p2 = points.get(i + 1);

                double dist = Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, worldX, worldY);

                if (dist < minDistance) {
                    minDistance = dist;
                    bestEdge = entry.getKey();
                    bestIndex = i;
                }
            }
        }

        if (bestEdge != null) {
            this.hoveredSegmentIndex = bestIndex;
            return bestEdge;
        }
        return null;
    }

    private void drawSegmentHighlight(GraphicsContext gc, SumoPosition2D p1, SumoPosition2D p2) {
        double x1 = worldToScreenX(p1.x);
        double y1 = worldToScreenY(p1.y);
        double x2 = worldToScreenX(p2.x);
        double y2 = worldToScreenY(p2.y);

        gc.setStroke(javafx.scene.paint.Color.LIMEGREEN);
        gc.setLineWidth(7);
        gc.setLineCap(StrokeLineCap.BUTT);
        gc.strokeLine(x1, y1, x2, y2);

        gc.setStroke(Color.web("#404040"));
        gc.setLineWidth(4);
        gc.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Toggles the "Spawn Mode" for vehicle injection.
     * <p>
     * When enabled, the user can select a road segment (Edge) on the map to define a route.
     * Changes the cursor to {@code CROSSHAIR} to indicate selection mode.
     * </p>
     */
    @FXML
    public void onAddVehicleClick() {
        // Scenario 1: user toggles the ON/OFF
        isSpawnMode = !isSpawnMode;

        if (isSpawnMode) {
            LOG.info("Select Mode ON: Select route");
            addVehicleBtn.setStyle("-fx-background-color: #808080;");
            mapCanvas.getScene().setCursor(Cursor.CROSSHAIR);

            selectedRouteEdges.clear();
            validNextEdges.clear();
            hoveredEdge = null;

            // hide the number of vehicles form until the button "Add Vehicle" is clicked
            vehicleInputForm.setVisible(false);
        }
        // Scenario 2: user has finished selecting routes and want to add vehicles
        else {
            if (!selectedRouteEdges.isEmpty()) {
                LOG.info("Route selected. Prompting for vehicle count...");

                vehicleCountInput.setText("1");
                vehicleInputForm.setVisible(true); // the form pops up
                vehicleInputForm.toFront(); // make sure it is on the top layer to be visible
            }
            else {
                resetSpawningVehicleUI();
            }
        }
    }

    /**
     * Confirms the vehicle spawn request.
     * <p>
     * Reads the vehicle count from the input field and triggers the spawning of vehicles
     * onto the currently selected route edge.
     * </p>
     */
    @FXML
    public void onConfirmSpawnClick() {
        try {
            String input = vehicleCountInput.getText();
            int count = Integer.parseInt(input);

            if (count > 0) {
                spawnMultipleVehicles(count);

                vehicleInputForm.setVisible(false);
                resetSpawningVehicleUI();
            } else {
                LOG.warn("Please enter a number greater than 0");
            }
        } catch (NumberFormatException e) {
            LOG.error("Invalid number format entered: " + vehicleCountInput.getText());
        }
    }

    @FXML
    public void onCancelSpawnClick() {
        LOG.info("Spawn cancelled by user.");
        vehicleInputForm.setVisible(false);
        resetSpawningVehicleUI();
    }

    private void resetSpawningVehicleUI() {
        selectedRouteEdges.clear();
        validNextEdges.clear();
        hoveredEdge = null;

        isSpawnMode = false;
        addVehicleBtn.setText("Select Route");
        addVehicleBtn.setStyle("-fx-background-color: #2196F3;");
        if (mapCanvas.getScene() != null) {
            mapCanvas.getScene().setCursor(Cursor.DEFAULT);
        }
        drawMap();
    }

    /**
     * Spawns a batch of vehicles onto the selected route.
     *
     * @param count The number of vehicles to spawn.
     * Vehicles are spawned with a 2-second time gap to prevent collisions.
     */

    private void spawnMultipleVehicles(int count) {
        long timestamp = System.currentTimeMillis();
        String tempRouteId = "route_" + timestamp;

        // vehicle type
        String selectedType = vehicleTypeCombo.getValue();

        try {
            panel.addRoute(tempRouteId, selectedRouteEdges);
            double currentTime = panel.getCurrentTime();

            for (int i = 0; i < count; i++) {
                String vehId = "veh_" + timestamp + "_" + i;
                // define a gap when spawning vehicles (2 seconds) to prevent traffic jam.
                int departTime = (int) (currentTime + (i * 2));
                panel.addVehicle(vehId, selectedType, tempRouteId, departTime, 0.0, 5.0, (byte) 0);
            }
            LOG.info("Successfully spawned " + count + " vehicles.");
        } catch (Exception e) {
            LOG.error("Failed to spawn vehicle batch: " + e.getMessage());
        }
    }

    @FXML
    public void onExportClick() {
        if (!isRecording) {
            LOG.info("User requested Data Export...");

            sessionHistory.clear();

            isRecording = true;

            exportBtn.setText("Stop & Save");
        }
        else {
            LOG.info("Stopping Recording & Saving...");

            isRecording = false;
            exportBtn.setText("Export");
            String filename = "TrafficReport.csv";

            ExportTask myTask = new ExportTask(reportManager, new LinkedList<>(sessionHistory), filename);
            Thread exportThread = new Thread(myTask);

            exportThread.start();
        }
    }

    @FXML
    public void onExportPdfClick() {
        if (sessionHistory.isEmpty()) {
            LOG.warn("No data to export! Please run the simulation first.");
            return;
        }
        LOG.info("Generating PDF report....");
        String filename = "TrafficReport.pdf";
        PdfReportManager pdfManager = new PdfReportManager();
        pdfManager.generatePdf(filename, sessionHistory, avgSpeedChart, waitingTimeChart);
    }

    // is improving, have to create a longer route.
    private void spawnVehicleOnSelectedRoute() {
        String vehId = "veh_" + System.currentTimeMillis();
        String tempRouteId = "route_" + System.currentTimeMillis();

        try {
            panel.addRoute(tempRouteId, selectedRouteEdges);

            int departTime = (int) panel.getCurrentTime();
            panel.addVehicle(vehId, "DEFAULT_VEHTYPE", tempRouteId, departTime, 0.0, 5.0, (byte) 0);
            LOG.info("Spawned vehicle on route of length: " + selectedRouteEdges.size());
        } catch (Exception e) {
            LOG.error("Failed to spawn vehicle: " + e.getMessage());
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
    public void onRestoreAutoClick() {
        LOG.info("User requested: RESTORING AUTOMATIC PROGRAM.");
        // This calls the method that sets the program back to "0"
        panel.turnOnAllLights();
    }
    @FXML
    public void onOptimizeClick() {
        isOptimizationActive = !isOptimizationActive;
        if (isOptimizationActive) {
            LOG.info("Auto-Optimization: ENABLED");
            optimize_traffic.setText("DISABLE OPTIMIZATION");
            // Amber color
            optimize_traffic.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-font-weight: bold;");
        } else {
            LOG.info("Auto-Optimization: DISABLED");
            optimize_traffic.setText("ENABLE OPTIMIZATION");
            // Purple color
            optimize_traffic.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-weight: bold;");

            lastOptimizedPhase = -1;
            lastSelectedId = "";
        }
    }

    @FXML
    public void onOptimizeAllClick() {
        isGlobalOptimizationActive = !isGlobalOptimizationActive;

        if (isGlobalOptimizationActive) {
            LOG.info("Global Optimization: ENABLED");
            optimize_all_traffic.setText("DISABLE ALL OPTIMIZATION");
            optimize_all_traffic.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold;");

            if (isOptimizationActive) {
                onOptimizeClick(); // Toggle the other button off
            }
        } else {
            LOG.info("Global Optimization: DISABLED");
            optimize_all_traffic.setText("OPTIMIZE ALL");
            optimize_all_traffic.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white;");
        }
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
        initialize3D();
        SubScene subScene = mapDraw3D.getSubScene();
        if (subScene == null) {
            LOG.error("The subscene is not initialized yet");
            return;
        }
        if (mapCanvas.isVisible()) {
            mapCanvas.setVisible(false);
            subScene.setVisible(true);
            currentRenderer = mapDraw3D;
            subScene.requestFocus();
        } else {
            mapCanvas.setVisible(true);
            subScene.setVisible(false);
            currentRenderer = mapDraw;
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
        if (panel.isRunning()) {
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

            double currentSpeed = panel.getGlobalMeanSpeed(); // Get the GLOBAL mean speed (average speed for every vehicle on the map)
            currentSpeed *= 3.6; // m/s to km/h

            timeSeconds += 0.1; // X-axis to show the time
            speedSeries.getData().add(new XYChart.Data<>(timeSeconds, currentSpeed)); // Data point for the chart


            List<Double> waits = panel.getAccumulatedWaitingTimes();
            int stage1 = 0;
            int stage2 = 0;
            int stage3 = 0;

            for (double w : waits) {
                if (w < 30) stage1++;
                else if (w < 60) stage2++;
                else stage3++;
            }

            timeDataSeries.getData().get(0).setYValue(stage1);
            timeDataSeries.getData().get(1).setYValue(stage2);
            timeDataSeries.getData().get(2).setYValue(stage3);

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
            if (isRecording) {
                sessionHistory.add(new SimulationStats(timeSeconds, currentSpeed, totalCO2, congestion));
            }

            String hotspot = panel.getMostCongestedEdge();
            congestionHotspot.setText("Hotspot: " + hotspot);
        }
    }

    private void displayCoords()
    {
        // get the coords
        cameraCoords = mapDraw3D.getCameraCoords();
        try
        {
            if (cameraCoords.isEmpty()) {
                LOG.error("Failed to get the camera coordinates");
            } else {
                String coords = "X: " + cameraCoords.get(0) + " Y: " + cameraCoords.get(1) + " Z: " + cameraCoords.get(2);
                XYZCoord.setText(coords);
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to set the coords");
        }
    }
    // step and draw map accordingly
    private void updateSimulation() {
        panel.step();
        if (isGlobalOptimizationActive && tlsWrapper != null) {
            try {
                // Get ALL Traffic Light IDs
                List<String> allIds = panel.getTrafficLightIDs();

                // Check every single traffic light
                if (allIds != null) {
                    for (String id : allIds) {
                        // Wrapper now handles the memory and logic for each ID
                        tlsWrapper.checkAndOptimize(id);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error in Global Optimization: " + e.getMessage());
            }
        }
        else if (isOptimizationActive && tlsWrapper != null) {
            try {
                String selectedId = trafficIdCombo.getValue();
                if (selectedId != null && !selectedId.isEmpty()) {
                    // Reuse the same smart method
                    tlsWrapper.checkAndOptimize(selectedId);
                }
            } catch (Exception e) {
                LOG.error("Error in Single Optimization: " + e.getMessage());
            }
        }
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
        // redraw the entire map, including the new positions and colors of vehicles in 3D
        if (currentRenderer instanceof MapDraw3D) {
            currentRenderer.drawAll();
        }
        // same but for 2D
        else {
            drawMap();
        }
    }

    private void drawMap() {
        if (mapDraw == null) {
            return;
        }
        mapDraw.setScale(this.SCALE);
        mapDraw.setOffsetX(this.OFFSET_X);
        mapDraw.setOffsetY(this.OFFSET_Y);
        mapDraw.setMapShapes(this.mapShapes);

        mapDraw.setShowEdgesID(this.showEdgesID);
        mapDraw.setShowVehicleID(this.showVehicleID);
        mapDraw.setShowRouteID(this.showRouteID);

        mapDraw.panel = this.panel;
        mapDraw.tlsWrapper = this.tlsWrapper;

        mapDraw.drawAll();

        GraphicsContext gc = mapCanvas.getGraphicsContext2D();

        // feature for highlighting the valid route when drag mouse to.
        if (isSpawnMode && hoveredEdge != null && mapShapes.containsKey(hoveredEdge)) {
            List<SumoPosition2D> points = mapShapes.get(hoveredEdge);
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(6);
            gc.setLineCap(StrokeLineCap.ROUND);
            drawPath(gc, points);
        }

        if (!selectedRouteEdges.isEmpty()) {
            for (String edgeId : selectedRouteEdges) {
                String lane0 = edgeId + "_0";
                if (mapShapes.containsKey(lane0)) {
                    List<SumoPosition2D> points = mapShapes.get(lane0);

                    gc.setStroke(Color.LIMEGREEN);
                    gc.setLineWidth(6);
                    gc.setLineCap(StrokeLineCap.ROUND);
                    drawPath(gc, points);
                }
                else {
                    for (String mapLaneId : mapShapes.keySet()) {
                        if (mapLaneId.startsWith(edgeId + "_")) {
                            List<SumoPosition2D> points = mapShapes.get(mapLaneId);
                            gc.setStroke(Color.LIMEGREEN);
                            gc.setLineWidth(6);
                            gc.setLineCap(StrokeLineCap.ROUND);
                            drawPath(gc, points);
                            break;
                        }
                    }
                }
            }
        }
        if (arePositionsLoaded) {
            for (Map.Entry<String, SumoPosition2D> entry : trafficLightPositions.entrySet()) {
                String id = entry.getKey();
                SumoPosition2D pos = entry.getValue();

                // --- CORRECT MATH FROM MAPDRAW.JAVA ---
                double screenX = (pos.x * SCALE) + OFFSET_X;
                // The critical fix: Subtract from height to flip Y-axis correctly
                double screenY = mapCanvas.getHeight() - ((pos.y * SCALE) + OFFSET_Y);

                // 1. Draw Standard Marker (Orange Dot)
                gc.setFill(Color.ORANGE);
                gc.fillOval(screenX - 4, screenY - 4, 8, 8);

                // 2. Draw "Selected" Ring (Yellow)
                String selectedId = trafficIdCombo.getValue();
                if (id != null && id.equals(selectedId)) {
                    gc.setStroke(Color.YELLOW);
                    gc.setLineWidth(3);
                    gc.strokeOval(screenX - 10, screenY - 10, 20, 20);
                }

                // 3. Draw "Hover" Glow (Cyan + Text)
                if (id != null && id.equals(hoveredTrafficLightId)) {
                    gc.setFill(Color.rgb(0, 255, 255, 0.5));
                    gc.fillOval(screenX - 15, screenY - 15, 30, 30);

                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // Fixed Font import
                    gc.fillText(id, screenX + 15, screenY - 5);
                }
            }
        }
    }

    private void drawPath(GraphicsContext gc, List<SumoPosition2D> points) {
        for (int i = 0; i < points.size() - 1; i++) {
            SumoPosition2D p1 = points.get(i);
            SumoPosition2D p2 = points.get(i + 1);
            gc.strokeLine(worldToScreenX(p1.x), worldToScreenY(p1.y),
                    worldToScreenX(p2.x), worldToScreenY(p2.y));
        }
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
            // set up the 3D
            mapDraw3D.setup();
            // get the subscene
            SubScene subScene = mapDraw3D.getSubScene();
            // set its visibility to 0
            subScene.setVisible(false);

            // add subScene first to not cover the control panel
            rootPane.getChildren().addFirst(subScene);

            // do "event" if detects key pressed
            subScene.setOnKeyPressed(event ->{
                // get the key input
                KeyCode keyInput = event.getCode();
                // add to the set
                keyInputSet.add(keyInput);
            });

            // do "event" if detects key released
            subScene.setOnKeyReleased(event ->{
                // get the keyInput
                KeyCode keyInput = event.getCode();
                // remove from the set
                keyInputSet.remove(keyInput);
            });

            // do "event" if detects mouse pressed
            subScene.setOnMousePressed(event ->{
                subScene.requestFocus();
                // get the mouse position

                lastMouseX = event.getX();
                lastMouseY = event.getY();
            });

            // do "event" if detects mouse dragged
            subScene.setOnMouseDragged(event ->{
                // calculate the change in mouse position
                double deltaMouseX = event.getX() - lastMouseX;
                double deltaMouseY = event.getY() - lastMouseY;

                // assign the lastMousePosition to the current position
                lastMouseX = event.getX();
                lastMouseY = event.getY();

                // call the function
                mapDraw3D.controlCameraEye(deltaMouseX, deltaMouseY, 0.1);
            });
        }
        // check roads if empty then draw roads
        boolean check = mapDraw3D.allRoadBoxes.isEmpty();
        if (check)
        {
            mapDraw3D.drawRoad();
        }
    }
}