// this file serves as the central code that connects backend (ControlPanel.java) and frontend (ui_design.java & ControlPanel.fxml)
    package ui;

    import javafx.animation.AnimationTimer;
    import javafx.fxml.FXML;
    import javafx.scene.canvas.Canvas;
    import javafx.scene.canvas.GraphicsContext;
    import javafx.scene.control.Button;
    import javafx.scene.control.ComboBox;
    import javafx.scene.control.Slider;
    import javafx.scene.control.ToggleButton;
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