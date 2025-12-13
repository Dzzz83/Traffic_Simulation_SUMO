// this file serves as the central code that connects backend (ControlPanel.java) and frontend (ui_design.java & ControlPanel.fxml)
    package ui;

    import javafx.animation.AnimationTimer;
    import javafx.fxml.FXML;
    import javafx.scene.canvas.Canvas;
    import javafx.scene.canvas.GraphicsContext;
    import javafx.scene.control.*;
    import javafx.scene.paint.Color;
    import wrapperSUMO.ControlPanel;
    import de.tudresden.sumo.objects.SumoPosition2D;

    import java.util.List;
    import java.util.Map;

    public class Controller {

        // initialize variables
        // @FXML is a special tag that tells Java to look into fxml file and link the correct object this file
        @FXML private Canvas mapCanvas;
        @FXML private Button startBtn;
        @FXML private Button stopBtn;
        @FXML private Button addVehicleBtn;
        @FXML private ComboBox<String> trafficIdCombo;
        @FXML private ToggleButton autoModeToggle;
        @FXML private Slider sliderRed;
        @FXML private Slider sliderGreen;
        @FXML private Slider sliderYellow;
        @FXML private Label labelRed;
        @FXML private Label labelGreen;
        @FXML private Label labelYellow;

        private ControlPanel panel;
        private AnimationTimer simulationLoop;
        private Map<String, List<SumoPosition2D>> mapShapes = null;


        private double SCALE = 1.0;       // initial Zoom
        private double OFFSET_X = 0;      // initial Pan X
        private double OFFSET_Y = 0;      // initial Pan Y
        private double lastMouseX, lastMouseY; // for dragging calculation

        @FXML
        public void initialize() {
            System.out.println("Starting the GUI ...");

            // initialize Control Panel
            panel = new ControlPanel();

            // connect to SUMO and load Map
            System.out.println("Connecting to SUMO to fetch map...");
            panel.startSimulation();
            // get the map data
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

            setupMapInteractions();
            OFFSET_X = mapCanvas.getWidth() / 4;
            OFFSET_Y = mapCanvas.getHeight() / 4;
            drawMap();

            simulationLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (panel.isRunning()) {
                        updateSimulation();
                    }
                }
            };
            // setup Interactive Map Controls (Zoom & Pan)
            setupMapInteractions();

            // Center the map
            OFFSET_X = mapCanvas.getWidth() / 4;
            OFFSET_Y = mapCanvas.getHeight() / 4;
            drawMap();

            // setup loop
            simulationLoop = new AnimationTimer() {
                // overide the default "handle(long now)" with a custom handle
                // overide also helps check the spelling
                @Override
                public void handle(long now) {
                    if (panel.isRunning()) {
                        updateSimulation();
                    }
                }
            };

            trafficIdCombo.getItems().addAll("Junction_1", "Junction_2");
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
            panel.stopSimulation();
            simulationLoop.stop();
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
        }
        // add vehicle button (haven't fully implemented)
        @FXML
        public void onAddVehicleClick() {
            System.out.println("Adding Vehicle...");
            String vehId = "veh_" + System.currentTimeMillis();
            panel.addVehicle(vehId, "DEFAULT_VEHTYPE", "route_0", 0, 50.0, 10.0, (byte)0);
            drawMap();
        }
        @FXML
        public void onTurnAllOffClick() {
            System.out.println("User requested: Turning ALL Lights OFF.");
            panel.turnOffAllLights();
        }

        // Action for the new button: Turn ALL Lights ON
        @FXML
        public void onTurnAllOnClick() {
            System.out.println("User requested: Turning ALL Lights ON.");
            panel.turnOnAllLights();
        }
        @FXML
        public void turn_all_lights_red() {
            System.out.println("User requested: FORCING ALL LIGHTS TO RED.");
            panel.turn_all_light_red();
            // Note: The UI drawing will update automatically on the next simulation step
            // because it calls panel.getRedYellowGreenState()
        }

        @FXML
        public void turn_all_lights_green() {
            System.out.println("User requested: FORCING ALL LIGHTS TO GREEN.");
            panel.turn_all_light_green();
        }

        @FXML
        public void onRestoreAutoClick() {
            System.out.println("User requested: RESTORING AUTOMATIC PROGRAM.");
            // This calls the method that sets the program back to "0"
            panel.turnOnAllLights();
        }

        // step and draw map accordingly
        private void updateSimulation() {
            panel.step();
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
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(2.0);

                for (List<SumoPosition2D> points : mapShapes.values()) {
                    double[] xPoints = new double[points.size()];
                    double[] yPoints = new double[points.size()];

                    for (int i = 0; i < points.size(); i++) {
                        // transform: Scale + Pan + Flip Y
                        xPoints[i] = (points.get(i).x * SCALE) + OFFSET_X;
                        yPoints[i] = mapCanvas.getHeight() - ((points.get(i).y * SCALE) + OFFSET_Y);
                    }
                    gc.strokePolyline(xPoints, yPoints, points.size());
                }
            }

            // draw traffic light
            List<String> trafficLightId = panel.getTrafficLightIDs();
            for (String trafficid : trafficLightId)
            {
                Map<String, List<SumoPosition2D>> position = panel.get_traffic_light_pos(trafficid);
                String state = panel.getRedYellowGreenState(trafficid);
                int laneIndex = 0;

                for (Map.Entry<String, List<SumoPosition2D>> entry : position.entrySet())
                {
                    List<SumoPosition2D> shape = entry.getValue();
                    if (shape == null || shape.isEmpty())
                    {
                        continue;
                    }
                    SumoPosition2D position2D = shape.get(shape.size() - 1);
                    double x = (position2D.x * SCALE) + OFFSET_X;
                    double y = mapCanvas.getHeight() - ((position2D.y * SCALE) + OFFSET_Y);

                    Color color;

                    if (state != null && state.length() > laneIndex) {
                        char s = state.charAt(laneIndex);
                        switch (s) {
                            case 'r': color = Color.RED; break;
                            case 'y': color = Color.YELLOW; break;
                            case 'g': color = Color.GREEN; break;
                            default: color = Color.GRAY; break;
                        }
                    }
                    else
                    {
                        color = Color.GRAY;
                    }

                    double size = 10;
                    gc.setFill(color);
                    gc.fillOval(x - size/2, y - size/2, size, size);

                    laneIndex++;
                }
            }

            // draw vehicles
            if (panel.isRunning()) {
                List<String> vehicles = panel.getVehicleIDs();
                gc.setFill(Color.YELLOW);

                for (String id : vehicles) {
                    SumoPosition2D pos = panel.getPosition(id);
                    double x = (pos.x * SCALE) + OFFSET_X;
                    double y = mapCanvas.getHeight() - ((pos.y * SCALE) + OFFSET_Y);

                    // draw size scales slightly with zoom so they don't vanish
                    double size = Math.max(5, 5 * SCALE);
                    gc.fillOval(x - size/2, y - size/2, size, size);
                }
            }
        }
    }