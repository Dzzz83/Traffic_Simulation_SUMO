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
    import javafx.scene.text.TextAlignment;
    import wrapperSUMO.ControlPanel;
    import de.tudresden.sumo.objects.SumoPosition2D;
    import javafx.scene.control.Label;

    import java.util.List;
    import java.util.Map;

    public class Controller {

        // initialize variables
        // @FXML is a special tag that tells Java to look into fxml file and link the correct object this file
        @FXML private Canvas mapCanvas;
        @FXML private Button startBtn;
        @FXML private Button stopBtn;
        @FXML private Label menuIcon;
        @FXML private VBox sidebar;
        @FXML private Button addVehicleBtn;
        @FXML private Button stressTestBtn;

        // buttons
        @FXML private Button EdgeIDBtn;
        @FXML private Button RouteIDBtn;
        @FXML private Button VehicleIDBtn;
        @FXML private Button TrafficLightIDBtn;

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

        // logical variables
        private ControlPanel panel;
        private AnimationTimer simulationLoop;
        private Map<String, List<SumoPosition2D>> mapShapes = null;

        // variables to highlight edgeID on the map
        private boolean showEdgesID = false;
        private boolean showTrafficLightID = false;
        private boolean showRouteID = false;
        private boolean showVehicleID = false;

        // variables for map
        private double SCALE = 1.0;       // initial Zoom
        private double OFFSET_X = 0;      // initial Pan X
        private double OFFSET_Y = 0;      // initial Pan Y
        private double lastMouseX, lastMouseY; // for dragging calculation

        // logic variables to show/hide to sidebar
        private boolean isSidebarVisible = true;


        private long lastUpdate = 0;
        private long simulationDelay = 100_000_000; // Default 100ms in nanoseconds

        @FXML
        public void initialize() {
            System.out.println("Starting the GUI ...");

            // initialize Control Panel
            panel = new ControlPanel();

            // connect to SUMO and load Map
            System.out.println("Connecting to SUMO to fetch map...");
            if (panel.startSimulation()) {
                connectionStatus.setText("Connection: Connected");
                connectionStatus.setStyle("-fx-text-fill: green");
            }
            else {
                connectionStatus.setText("Connection: Disconnected");
                connectionStatus.setStyle("-fx-text-fill: red");
            }

            // get the map data
            mapShapes = panel.getMapShape();

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
                        // Only update if enough time has passed (throttling)
                        if (now - lastUpdate >= simulationDelay) {
                            updateSimulation();
                            lastUpdate = now;
                        }
                    }
                }
            };

            trafficIdCombo.getItems().addAll("Junction_1", "Junction_2");
            trafficIdCombo.getSelectionModel().selectFirst();
        }

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

            // toggle view EdgeID button
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

            // toggle view TrafficLightID button
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
                    System.out.println(String.valueOf(newVal));
                });
            }
            if (sliderGreen != null) {
                sliderGreen.valueProperty().addListener((obs, oldVal, newVal) -> {
                    greenValue.setText(String.format("%.2fs", newVal.doubleValue()));
                    System.out.println(String.valueOf(newVal));
                });
            }
            if (sliderYellow != null) {
                sliderYellow.valueProperty().addListener((obs, oldVal, newVal) -> {
                    yellowValue.setText(String.format("%.2fs", newVal.doubleValue()));
                    System.out.println("Red duration set to: " + newVal.intValue());
                });
            }

            if (delaySlider != null) {
                // Set initial value
                simulationDelay = (long) (delaySlider.getValue() * 1_000_000);

                delaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    // Convert ms to nanoseconds
                    simulationDelay = (long) (newVal.doubleValue() * 1_000_000);
                    delayValue.setText(String.format("%.0f", newVal.doubleValue()));
                    System.out.println("Delay set to: " + newVal.intValue() + "ms");
                });
            }

            if (inFlowSlider != null) {
                inFlowSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                        inFlowValue.setText(String.format("%.1f", newVal.doubleValue()));
                        System.out.println("In Flow: " + newVal);
                });
            }

            if (maxSpeedSlider != null) {
                maxSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    maxSpeedValue.setText(String.format("%.1f", newVal.doubleValue()));
                    System.out.println("Max Speed: " + newVal);
                        });
            }

            // auto mode
            if (autoModeToggle != null) {
                autoModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        autoModeToggle.setText("OFF");
                        autoModeToggle.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        System.out.println("Auto mode: OFF");
                    }
                    else {
                        autoModeToggle.setText("ON");
                        autoModeToggle.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                        System.out.println("Auto mode: ON");
                    }
                });
            }
        }


        // button logic
        // startButton
        @FXML
        public void onStartClick() {
            System.out.println("Resuming Simulation Loop...");
            simulationLoop.start();
            startBtn.setDisable(true);
            stopBtn.setDisable(false);
        }
        // stop button
        @FXML
        public void onStopClick() {
            System.out.println("Stopping Simulation...");
            simulationLoop.stop();
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
        }
        // add vehicle button (haven't fully implemented)
        @FXML
        public void onAddVehicleClick() {
            System.out.println("Adding Vehicle...");
            String vehId = "veh_" + System.currentTimeMillis();

            try {
                int departure_time = (int) panel.getCurrentTime();
                panel.addVehicle(vehId, "DEFAULT_VEHTYPE", "r_0", departure_time, 50.0, 10.0, (byte) -2);
                panel.step();
                drawMap();
            } catch (Exception e) {
                System.err.println("Failed to add vehicle: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // add the stress test button
        public void onStressTestClick()
        {
            System.out.println("Performing Stress Test");
            final int vehicleCount = 500;
            try
            {
                // call the function stressTest
                panel.stressTest(vehicleCount);
                // redraw map
                drawMap();
                System.out.println("Adding " + vehicleCount + " to the simulation");
            }
            catch (Exception e)
            {
                System.out.println("Failed to perform stress test");
                e.printStackTrace();
            }
        }

        private void updateStats() {
            int count = panel.getVehicleCount();
            numberVehicles.setText(String.valueOf(count));

            if (!panel.getVehicleIDs().isEmpty()) {
                double avgSpeed = panel.getVehicleSpeed(panel.getVehicleIDs().getFirst());
                averageSpeed.setText(String.valueOf(avgSpeed));
            }
        }

        // step and draw map accordingly
        private void updateSimulation() {
            panel.step();
            updateStats();
            drawMap();
        }

        // draw map function
        private void drawMap() {
            GraphicsContext gc = mapCanvas.getGraphicsContext2D();

            // set background color
            gc.setFill(Color.web("#147213")); // green
            gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

            // draw Roads
            if (mapShapes != null) {
                if (showEdgesID){
                    gc.setStroke(Color.CYAN);
                    gc.setLineWidth(3);
                    gc.setFill(Color.WHITE);
                    gc.setTextAlign(TextAlignment.CENTER);
                }
                else {
                    gc.setStroke(Color.GRAY);
                    gc.setLineWidth(2.0);
                }
                for (Map.Entry<String, List<SumoPosition2D>> entry : mapShapes.entrySet()) {
                    String laneID = entry.getKey();
                    List<SumoPosition2D> points = entry.getValue();

                    double[] xPoints = new double[points.size()];
                    double[] yPoints = new double[points.size()];

                    for (int i = 0; i < points.size(); i++) {
                        // transform: Scale + Pan + Flip Y
                        xPoints[i] = (points.get(i).x * SCALE) + OFFSET_X;
                        yPoints[i] = mapCanvas.getHeight() - ((points.get(i).y * SCALE) + OFFSET_Y);
                    }

                    // draw the road
                    gc.strokePolyline(xPoints, yPoints, points.size());

                    // draw the edgeID label to display which edge it is
                    if (showEdgesID && points.size() > 1) {
                        // get edgeID from laneID (example: laneID "E1_0" -> edgeID "E1")
                        String edgeID = laneID;
                        int _index = laneID.lastIndexOf('_');
                        if (_index != -1) {
                            edgeID = laneID.substring(0, _index);
                        }
                        if (!edgeID.startsWith(":")) {
                            int midIndex = points.size() / 2;
                            double midX = xPoints[midIndex];
                            double midY = yPoints[midIndex];
                            gc.fillText(edgeID, midX, midY);
                        }
                    }
                }
            }

            // draw vehicles
            if (panel.isRunning()) {
                List<String> vehicles = panel.getVehicleIDs();

                for (String vehId : vehicles) {
                    SumoPosition2D pos = panel.getPosition(vehId);
                    double vehX = (pos.x * SCALE) + OFFSET_X;
                    double vehY = mapCanvas.getHeight() - ((pos.y * SCALE) + OFFSET_Y);

                    // draw size scales slightly with zoom so they don't vanish
                    double size = Math.max(5, 5 * SCALE);
                    gc.setFill(Color.YELLOW);
                    gc.fillOval(vehX - size/2, vehY - size/2, size, size);

                    if (showVehicleID) {
                        gc.setFill(Color.LIME);
                        gc.fillText(vehId, vehX, vehY - 8); // Above vehicle
                    }

                    if (showRouteID) {
                        String routeID = panel.getVehicleRouteID(vehId);

                        gc.setFill(Color.GREEN);
                        gc.fillText(routeID, vehX, vehY + 15);

                        String currentEdgeID = panel.getRoadID(vehId);

                        String laneID = currentEdgeID.startsWith(":") ? currentEdgeID + "_0" : currentEdgeID + "_0";

                        if (mapShapes.containsKey(laneID)) {
                            List<SumoPosition2D> roadPoints = mapShapes.get(laneID);
                            if (roadPoints != null && roadPoints.size() > 1) {
                                int mid = roadPoints.size() / 2;
                                double roadX = (roadPoints.get(mid).x * SCALE) + OFFSET_X;
                                double roadY = mapCanvas.getHeight() - ((roadPoints.get(mid).y * SCALE) + OFFSET_Y);

                                gc.setFill(Color.MAGENTA);
                                gc.fillText("RouteID: " + routeID, roadX, roadY);
                            }
                        }
                    }
                }
            }
        }
    }