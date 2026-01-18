package app;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wrapperSUMO.ControlPanel;
import wrapperSUMO.TrafficLightWrapper;
import wrapperSUMO.TrafficConnectInfo;
import de.tudresden.sumo.objects.SumoPosition2D;

/**
 * MapDraw handles the 2D visualization of the SUMO simulation using a JavaFX Canvas.
 * It manages the rendering of road networks, traffic lights, and vehicles,
 * supporting features like zooming, panning, and ID labeling.
 * * This class implements the {@link MapRenderer} interface to allow for
 * interchangeable use within the simulation controller.
 */
public class MapDraw implements MapRenderer
{
    private static final Logger LOG = LogManager.getLogger(MapDraw.class.getName());
    /**
     * The active filter for vehicle rendering.
     * Defaults to "All".
     */
    private String activeVehicleFilter = "All";
    /**
     * Updates the active vehicle filter.
     *
     * @param filter The new filter criteria (e.g., "Passenger", "Taxi", "Delivery").
     */
    public void setVehicleFilter(String filter) {
        this.activeVehicleFilter = filter;
    }

    private Canvas canvas;

    private ControlPanel panel;
    public TrafficLightWrapper tlsWrapper;
    private Map<String, List<SumoPosition2D>> mapShapes;

    public double SCALE = 1.0;
    public double OFFSET_X = 0;
    public double OFFSET_Y = 0;

    /**
     * Flag to toggle the rendering of Edge IDs on the map.
     * When true, text labels for edge IDs are drawn over the road segments.
     */
    public boolean showEdgesID = false;
    /**
     * Flag to toggle the rendering of Vehicle IDs.
     * When true, the vehicle's unique ID is drawn above the vehicle body.
     */
    public boolean showVehicleID = false;
    /**
     * Flag to toggle the rendering of the vehicle's current Route ID.
     * When true, the route ID is drawn below the vehicle body.
     */
    public boolean showRouteID = false;

    private static final Color ASPHALT_COLOR = Color.web("#404040");
    private static final Color GRASS_COLOR = Color.web("#2E7D32");

    private List<List<SumoPosition2D>> dashedWhiteLines = new ArrayList<>();
    private List<List<SumoPosition2D>> solidCenterLines = new ArrayList<>();

    private final VehicleRenderer carRenderer = new CarRenderer();
    private final VehicleRenderer deliveryRenderer = new DeliveryRenderer();
    private final VehicleRenderer taxiRenderer = new TaxiRenderer();
    private final VehicleRenderer evRenderer = new EvehicleRenderer();

    /**
     * Constructs a MapDraw instance and associates it with a JavaFX Canvas.
     * * @param canvas The Canvas object where the 2D map will be rendered.
     */
    public MapDraw(Canvas canvas)
    {
        this.canvas = canvas;
    }
    /**
     * Executes the complete drawing sequence for the 2D map.
     * It clears the canvas and redraws the road infrastructure,
     * active traffic lights, and all simulated vehicles.
     */
    @Override
    public void drawAll() {
        if (canvas == null) {
            return;
        }

        drawRoads();

        if (tlsWrapper != null && panel != null && panel.isRunning()) {
            drawTrafficLights(canvas.getGraphicsContext2D());
        }

        drawAllVehicles();
    }

    /**
     * Manages the rendering of the road network infrastructure.
     * Depending on current settings, it renders either the detailed
     * road textures (asphalt, markings) or a debug view with IDs.
     */
    public void drawRoads() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (mapShapes == null || mapShapes.isEmpty()) return;

        drawGrass(gc);

        if (dashedWhiteLines.isEmpty()) {
            generateWhiteLines();
        }

        if (showEdgesID) {
            drawDebugRoads(gc);
        } else {
            drawBaseRoads(gc);
        }
    }
    /**
     * Fills the entire background of the canvas with a grass texture color.
     * * @param gc The GraphicsContext used to issue draw calls to the canvas.
     */
    public void drawGrass(GraphicsContext gc)
    {
        gc.setFill(GRASS_COLOR);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawDebugRoads(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        for (Map.Entry<String, List<SumoPosition2D>> entry : mapShapes.entrySet()) {
            drawPolyLine(gc, entry.getValue());

            String laneID = entry.getKey();
            if (!laneID.startsWith(":") && entry.getValue().size() > 1) {
                drawEdgeLabel(gc, laneID, entry.getValue());
            }
        }
    }
    /**
     * Renders the stylized road infrastructure in multiple layers.
     * This method draws the road curb and asphalt layers using a painter's algorithm
     * approach and conditionally renders road markings based on the current zoom level.
     * * @param gc the GraphicsContext used for drawing on the canvas.
     */
    private void drawBaseRoads(GraphicsContext gc) {
        double baseRoadWidth = Math.max(2.0, 4.5 * SCALE);

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // draw first layer: curb
        double firstLayerWidth = baseRoadWidth + (1.0 * SCALE);
        drawRoadLayer(gc, Color.LIGHTGRAY, firstLayerWidth);

        // draw second layer: asphalt
        drawRoadLayer(gc, ASPHALT_COLOR, baseRoadWidth);

        // draw dashed white lines and solid white lines
        if (SCALE > 0.5) {
            drawRoadMarkings(gc);
        }
    }
    /**
     * Draws a specific layer of the road network with a designated color and width.
     * It iterates through all map shapes to apply the stroke to every road segment.
     * * @param gc    the GraphicsContext used for drawing.
     * @param color the Color to be applied to this road layer.
     * @param width the stroke thickness, adjusted by the current scale.
     */
    private void drawRoadLayer(GraphicsContext gc, Color color, double width) {
        gc.setStroke(color);
        gc.setLineWidth(width);
        for (List<SumoPosition2D> points : mapShapes.values()) {
            drawPolyLine(gc, points);
        }
    }
    /**
     * Renders the lane markings on top of the asphalt layer.
     * This includes dashed lines for lane separation and solid lines for road boundaries
     * or centerlines. Dash patterns are dynamically scaled based on the zoom level.
     * * @param gc the GraphicsContext used for drawing.
     */
    private void drawRoadMarkings(GraphicsContext gc) {
        double lineWidth = 0.3 * SCALE;
        gc.setLineWidth(lineWidth);

        // draw dashed Lines
        gc.setStroke(Color.WHITESMOKE);
        gc.setLineCap(StrokeLineCap.BUTT);
        gc.setLineDashes(2.0 * SCALE, 3.0 * SCALE);
        for (List<SumoPosition2D> points : dashedWhiteLines) {
            drawPolyLine(gc, points);
        }

        // draw solid Lines
        gc.setLineDashes(null);
        gc.setStroke(Color.WHITE);
        for (List<SumoPosition2D> points : solidCenterLines) {
            drawPolyLine(gc, points);
        }

        gc.setLineCap(StrokeLineCap.ROUND); // Reset cap
    }

    //Helper to select the correct renderer based on XML ID
    private VehicleRenderer getRenderer(String typeID) {
        if (typeID == null) return carRenderer;

        switch (typeID) {
            case "Delivery":         return deliveryRenderer;
            case "Evehicle":         return evRenderer;
            case "DEFAULT_TAXITYPE": return taxiRenderer;
            case "DEFAULT_VEHTYPE":
            default:                 return carRenderer;
        }
    }

    /**
     * Determines if a specific vehicle should be drawn based on the active filter.
     *
     * @param vehicleTypeID The type ID of the vehicle being rendered.
     * @return true if the vehicle matches the active filter or if filter is "All"; false otherwise.
     */
    private boolean shouldDrawVehicle(String vehicleTypeID) {
        if ("All".equals(activeVehicleFilter)) {
            return true;
        }

        switch (activeVehicleFilter) {
            case "Passenger":
                return "DEFAULT_VEHTYPE".equals(vehicleTypeID);
            case "Taxi":
                return "DEFAULT_TAXITYPE".equals(vehicleTypeID);
            case "Delivery":
                return "Delivery".equals(vehicleTypeID);
            case "Evehicle":
                return "Evehicle".equals(vehicleTypeID);
            default:
                return true;
        }
    }

    public void drawAllVehicles() {
        if (panel == null || !panel.isRunning()) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();

        //Get the list of current vehicle IDs
        List<String> vehicles = panel.getVehicleIDs();

        double carLength = 4.5 * SCALE;
        if (carLength < 8.0) carLength = 8.0;

        double carWidth = 2.0 * SCALE;
        if (carWidth < 4.0) carWidth = 4.0;

        for (String id : vehicles) {
            SumoPosition2D pos = panel.getPosition(id);
            double x = (pos.x * SCALE) + OFFSET_X;
            double y = canvas.getHeight() - ((pos.y * SCALE) + OFFSET_Y);
            double angle = panel.getVehicleAngle(id);

            Color color;
            //Select the specific renderer
            try {
                color = panel.getVehicleColor(id);

                //Ask the panel for the Type ID
                String typeID = panel.getVehicleTypeID(id);
                if (!shouldDrawVehicle(typeID)) {
                    continue;
                }
                VehicleRenderer renderer = getRenderer(typeID);
                //Draw
                renderer.draw(gc, x, y, angle, carLength, carWidth, color);
            } catch (Exception e) {
                color = Color.YELLOW;
            }

            if (showVehicleID) {
                gc.setFill(Color.LIME);
                gc.fillText(id, x, y - 8);
            }

            if (showRouteID) {
                try {
                    String routeID = panel.getVehicleRouteID(id);
                    gc.setFill(Color.RED);
                    gc.fillText(routeID, x, y + 15);
                } catch (Exception e) {}
            }
        }
    }

    /**
     * translates simulation coordinates into screen pixels and renders them as a polyline.
     * this method applies scaling and offsets for zooming and panning, and inverts the y-axis
     * to map the simulation's cartesian coordinate system to the javafx canvas coordinate system.
     * * @param gc the graphicscontext used for drawing on the canvas.
     * @param points the list of sumoposition2d coordinates representing the shape to be drawn.
     */
    public void drawPolyLine(GraphicsContext gc, List<SumoPosition2D> points)
    {
        if (points.isEmpty()) {
            return;
        }
        double[] xPoints = new double[points.size()];
        double[] yPoints = new double[points.size()];
        for (int i = 0; i < points.size(); i++)
        {
            // apply zoom scale and horizontal offset
            xPoints[i] = (points.get(i).x * SCALE ) + OFFSET_X;
            // invert the y-axis to match the javafx
            yPoints[i] = canvas.getHeight() - ((points.get(i).y * SCALE) + OFFSET_Y);
        }
        // draw the connected line segment on the canvas
        gc.strokePolyline(xPoints, yPoints, points.size());
    }

    /**
     * Draws the ID label of a road edge at the midpoint of its geometry.
     * <p>
     * Calculates the center point and perpendicular vector of the middle segment
     * to offset the text slightly away from the road line for better readability.
     * </p>
     *
     * @param gc     The GraphicsContext used for drawing.
     * @param laneID The unique identifier string of the lane/edge.
     * @param points The list of 2D coordinates defining the shape of the edge.
     */
    public void drawEdgeLabel(GraphicsContext gc, String laneID, List<SumoPosition2D> points)
    {
        if (points.size() < 2) return;
        String edgeID = laneID;
        int _index = laneID.lastIndexOf('_');
        if (_index != -1) {
            edgeID = laneID.substring(0, _index);
        }

        int midIndex = points.size() / 2;
        if (midIndex >= points.size() - 1) {
            midIndex = points.size() - 2;
        }

        SumoPosition2D p1 = points.get(midIndex);
        SumoPosition2D p2 = points.get(midIndex + 1);

        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;

        double unitX = dx / length;
        double unitY = dy / length;
        double normalX = unitY;
        double normalY = -unitX;

        double offsetDistance = 7.0;
        double worldX = ((p1.x + p2.x) / 2.0) + (normalX * offsetDistance);
        double worldY = ((p1.y + p2.y) / 2.0) + (normalY * offsetDistance);

        double screenX = (worldX * SCALE) + OFFSET_X;
        double screenY = canvas.getHeight() - ((worldY * SCALE) + OFFSET_Y);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2.5);
        gc.strokeText(edgeID, screenX, screenY);
        gc.setFill(Color.WHITE);
        gc.fillText(edgeID, screenX, screenY);
    }
    /**
     * performs vector normalization by dividing a component by the vector length.
     * this helper ensures that the resulting directional components are within a unit range.
     * * @param length the calculated magnitude of the vector.
     * @param var the specific x or y component to be normalized.
     * @return the normalized component, or 0.0 if length is zero or an error occurs.
     */
    private double normalizeVector(double length, double var)
    {
        try {
            if (length > 0)
            {
                return var / length;
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to normalize vectors");
        }
        return 0.0;
    }
    /**
     * calculates a new path that runs parallel to the provided coordinates at a specific offset.
     * this is used to determine road boundaries or centerlines by calculating perpendicular
     * vectors for each segment of the road.
     * * @param points the original list of coordinates representing the lane center.
     * @param offset the distance to shift the line (positive for left, negative for right).
     * @return a list of coordinates representing the shifted parallel path.
     */
    // find the line parallel to the left most lane
    private List<SumoPosition2D> calculateLeftParallelLine(List<SumoPosition2D> points, double offset) {

        List<SumoPosition2D> leftParallelLine = new ArrayList<>();
        if (points.size() < 2) {
            return leftParallelLine;
        }

        for (int i = 0; i < points.size(); i++) {
            // create variables to store the 90 degrees vector
            double leftDirectionalX = 0, leftDirectionalY = 0;

            if (i > 0) {
                // calculate dx, dy from the current point to the point before
                double dx = points.get(i).x - points.get(i - 1).x;
                double dy = points.get(i).y - points.get(i - 1).y;
                // calculate the length of the segment between the 2 points using Pythargon
                double len = Math.sqrt(dx * dx + dy * dy);

                // to find the perpendicular vector, take (x, y) --> (-x, y)
                // normalize dx, dy

                leftDirectionalX -= normalizeVector(len, dy);
                leftDirectionalY += normalizeVector(len, dx);
            }

            if (i < points.size() - 1) {
                // calculate the points from the next point to the current point
                double dx = points.get(i + 1).x - points.get(i).x;
                double dy = points.get(i + 1).y - points.get(i).y;
                // calculate the length of the segment between the 2 points using Pythargon
                double len = Math.sqrt(dx*dx + dy*dy);

                // to find the perpendicular vector, take (x, y) --> (-x, y)
                // normalize dx, dy
                if (len > 0) {
                    leftDirectionalX -= normalizeVector(len, dy);
                    leftDirectionalY += normalizeVector(len, dx);
                }
            }

            // calculate the length
            double len = Math.sqrt(leftDirectionalX * leftDirectionalX + leftDirectionalY * leftDirectionalY);
            if (len > 0) {
                // normalize the vectors
                leftDirectionalX /= len;
                leftDirectionalY /= len;

            }

            // calculate the left parallel points
            double newX = points.get(i).x + (leftDirectionalX * offset);
            double newY = points.get(i).y + (leftDirectionalY * offset);

            SumoPosition2D leftParallelPoint = new SumoPosition2D(newX, newY);

            // add to the list
            leftParallelLine.add(leftParallelPoint);
        }
        return leftParallelLine;
    }

    /**
     * coordinates the generation of all white road markings.
     * it groups lanes by their parent edge, sorts them, and then generates either
     * dashed lines (for lane separation) or solid lines (for road boundaries).
     */
    private void generateWhiteLines() {
        dashedWhiteLines.clear();
        solidCenterLines.clear();
        if (mapShapes == null) return;

        // create the hashset
        Map<String, Map<Integer, List<SumoPosition2D>>> edgeGroups = createRoadHashMap();
        Set<String> alreadyDrawn = new HashSet<>();

        for (Map<Integer, List<SumoPosition2D>> laneMap : edgeGroups.values()) {
            if (laneMap.isEmpty()) {
                continue;
            }

            List<Integer> sortedIndices = new ArrayList<>(laneMap.keySet());
            Collections.sort(sortedIndices);

            // draw dashed white lines
            createDashedWhiteLine(laneMap, sortedIndices);

            // draw the left parallel line
            createLeftBoundaryLine(laneMap, sortedIndices, alreadyDrawn);
        }
    }

    /**
     * parses the raw map data to group individual lanes into their respective road edges.
     * it filters out internal sumo junctions and organizes coordinates by edge id and lane index.
     * * @return a nested map structure where the primary key is the edge id and the value is a map of lanes.
     */
    // create road hash map
    private Map<String, Map<Integer, List<SumoPosition2D>>> createRoadHashMap()
    {
        // create a hashmap
        Map<String, Map<Integer, List<SumoPosition2D>>> roadMap = new HashMap<>();
        // loop through each pairs of keys and values
        for (Map.Entry<String, List<SumoPosition2D>> entry : mapShapes.entrySet()){
            // extract the key
            String fullEdgeID = entry.getKey();

            // skip internal edges
            if (fullEdgeID.startsWith(":")){
                continue;
            }

            // get the position of "_"
            int underScoreIndex = fullEdgeID.lastIndexOf("_");

            if (underScoreIndex != -1) {
                // get the edgeID
                String edgeID = fullEdgeID.substring(0, underScoreIndex);
                try {
                    // get the laneID
                    int laneID = Integer.parseInt(fullEdgeID.substring(underScoreIndex + 1));
                    // create a hashmap for that edgeID
                    roadMap.putIfAbsent(edgeID, new HashMap<>());
                    // put laneID and 2D data into the hashmap
                    roadMap.get(edgeID).put(laneID, entry.getValue());
                }
                catch (NumberFormatException e) {
                    LOG.error("Could not parse lane index for: " + fullEdgeID);
                }
            }
        }
        return roadMap;
    }
    /**
     * generates coordinates for dashed lines that separate multiple lanes on the same road.
     * it calculates the midpoint between adjacent lanes to place the separator correctly.
     * * @param laneMap the map containing lane indices and their coordinates for a specific edge.
     * @param sortedIndicies the list of lane indices sorted from right to left.
     */
    private void createDashedWhiteLine(Map<Integer, List<SumoPosition2D>> laneMap, List<Integer> sortedIndicies){
        for (int i = 0; i < sortedIndicies.size() - 1; i++)
        {
            // get laneA and laneB
            List<SumoPosition2D> laneA = laneMap.get(sortedIndicies.get(i));
            List<SumoPosition2D> laneB = laneMap.get(sortedIndicies.get(i+1));

            // create a list for the middle line
            List<SumoPosition2D> separator = new ArrayList<>();
            // find number of points
            int numberOfPoints = Math.min(laneA.size(), laneB.size());

            for (int j = 0; j < numberOfPoints; j++)
            {
                // find the middle point
                double midX = (laneA.get(j).x + laneB.get(j).x) / 2.0;
                double midY = (laneA.get(j).y + laneB.get(j).y) / 2.0;
                SumoPosition2D centerPoint = new SumoPosition2D(midX, midY);
                // add to the list
                separator.add(centerPoint);
            }
            // add the middle line into dashedWhiteLines
            dashedWhiteLines.add(separator);
        }
    }
    /**
     * generates the leftmost boundary line for a road edge.
     * depending on the lane count and road type, it assigns the boundary as either
     * a solid center line or a dashed outer marking.
     * * @param laneMap the map of lanes for the current edge.
     * @param sortedIndices the sorted indices of the lanes.
     * @param alreadyDrawn a set used to track and prevent duplicate drawing of identical boundaries.
     */
    private void createLeftBoundaryLine(Map<Integer, List<SumoPosition2D>> laneMap, List<Integer> sortedIndices, Set<String> alreadyDrawn)
    {
        // get the index of the leftMostLane
        int maxIndex = sortedIndices.getLast();
        // get the leftMostLane
        List<SumoPosition2D> leftMostLane = laneMap.get(maxIndex);
        // calculate the road boundary
        List<SumoPosition2D> roadBoundary = calculateLeftParallelLine(leftMostLane, 1.6);

        // draw solid lines if the road has more than 1 lane
        if (sortedIndices.size() > 1){
            solidCenterLines.add(roadBoundary);
        }
        // draw dashed lines if the road has 1 lane
        else if (!roadBoundary.isEmpty())
        {
            // create a unique key
            int midIdx = roadBoundary.size() / 2;
            SumoPosition2D mid = roadBoundary.get(midIdx);
            String key = Math.round(mid.x) + "_" + Math.round(mid.y);
            // add the key to the set
            if (alreadyDrawn.add(key)) {
                // if true, add the line to dashedWhiteLine
                dashedWhiteLines.add(roadBoundary);
            }
        }
    }


    /**
     * Renders all traffic lights onto the simulation canvas.
     * <p>
     * This method iterates through all registered traffic lights. For each connection,
     * it calculates the position and rotation of the stop line based on the lane geometry.
     * It then applies a coordinate transformation (translate and rotate) to the
     * {@link GraphicsContext} before drawing the specific light configuration.
     * </p>
     * <p>
     * <strong>Optimization:</strong> Rendering is skipped if the current zoom scale
     * is below 0.2 to improve performance.
     * </p>
     * * @param gc The JavaFX GraphicsContext used for drawing.
     */
    public void drawTrafficLights(GraphicsContext gc) {
        if (tlsWrapper == null || !panel.isRunning()) return;
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

                    SumoPosition2D stopPos = shape.get(shape.size() - 1);
                    double drawX = (stopPos.x * SCALE) + OFFSET_X;
                    double drawY = canvas.getHeight() - ((stopPos.y * SCALE) + OFFSET_Y);

                    SumoPosition2D prevPos = shape.get(shape.size() - 2);
                    double prevX = (prevPos.x * SCALE) + OFFSET_X;
                    double prevY = canvas.getHeight() - ((prevPos.y * SCALE) + OFFSET_Y);

                    double angle = Math.toDegrees(Math.atan2(drawY - prevY, drawX - prevX));
                    double rotation = angle + 90;

                    gc.save();
                    gc.translate(drawX, drawY);
                    gc.rotate(rotation);
                    drawDirectionalTrafficLight(gc, 0, 0, secondsLeft, edgeConnections, trafficid);
                    gc.restore();
                }
            }
        }
    }
    /**
     * Draws a specific traffic light box containing directional arrows.
     * <p>
     * Adapts the detail level (text vs. simple dots) based on the current zoom {@code SCALE}.
     * Colors the arrows Red, Yellow, or Green based on the simulation state.
     * </p>
     * * @param gc The graphics context.
     * @param x The center X coordinate.
     * @param y The center Y coordinate.
     * @param secondsLeft The countdown timer value to display (if detailed mode is on).
     * @param connections The list of lane connections controlled by this light.
     * @param trafficId The ID of the traffic light (for state lookup).
     */
    private void drawDirectionalTrafficLight(GraphicsContext gc, double x, double y, int secondsLeft, List<TrafficConnectInfo> connections, String trafficId) {
        if (connections.isEmpty()) return;
        boolean isDetailed = SCALE > 0.6;
        Map<String, TrafficConnectInfo> slotMap = new HashMap<>();
        for (TrafficConnectInfo info : connections) slotMap.putIfAbsent(info.getDirection().toLowerCase(), info);

        double sizeFactor = Math.min(1.0, Math.max(0.6, SCALE));
        double lightRadius = (isDetailed ? 4 : 2.5) * sizeFactor;
        double padding = 3 * sizeFactor;
        double spacing = 2 * sizeFactor;
        double slotSize = (lightRadius * 2);
        double slotStep = slotSize + spacing;

        String[] directions = {"t", "l", "s", "r"};
        double boxWidth = (slotStep * 4) + padding;
        double boxHeight = slotSize + (padding * 2);
        if (isDetailed) boxHeight += (10 * sizeFactor);

        double startX = x - (boxWidth / 2);
        double startY = y;

        gc.setFill(Color.web("#222222"));
        gc.setStroke(Color.web("#111111"));
        gc.setLineWidth(0.5);
        gc.fillRoundRect(startX, startY, boxWidth, boxHeight, 10 * sizeFactor, 10 * sizeFactor);
        gc.strokeRoundRect(startX, startY, boxWidth, boxHeight, 10 * sizeFactor, 10 * sizeFactor);

        double currentX = startX + padding + lightRadius;
        double lightY = startY + padding + lightRadius;

        for (String dir : directions) {
            TrafficConnectInfo info = slotMap.get(dir);
            Color arrowColor = Color.web("#444444");

            if (info != null) {
                char state = tlsWrapper.getStateForConnection(trafficId, info.getLinkIndex());
                if (state == 'r' || state == 'R') arrowColor = Color.web("#FF3333");
                else if (state == 'y' || state == 'Y') arrowColor = Color.web("#FFCC00");
                else if (state == 'g' || state == 'G') arrowColor = Color.web("#00FF66");
            }

            if (isDetailed) {
                drawArrow(gc, currentX, lightY, lightRadius, dir, arrowColor);
            } else {
                gc.setFill(arrowColor);
                gc.fillOval(currentX - lightRadius, lightY - lightRadius, lightRadius * 2, lightRadius * 2);
            }

            if (isDetailed && secondsLeft >= 0 && info != null) {
                gc.setFill(Color.web("#DDDDDD"));
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8 * sizeFactor));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.valueOf(secondsLeft), currentX, startY + boxHeight - padding);
            }
            currentX += slotStep;
        }
    }

    private void drawArrow(GraphicsContext gc, double cx, double cy, double r, String dir, Color color) {
        gc.setFill(color);
        gc.setStroke(color);
        gc.setLineWidth(1.5);

        switch (dir) {
            case "s":
                gc.fillPolygon(new double[]{cx, cx - r, cx + r}, new double[]{cy - r, cy + r, cy + r}, 3);
                break;
            case "l":
                gc.fillPolygon(new double[]{cx - r, cx + r/2, cx + r/2}, new double[]{cy, cy - r, cy + r}, 3);
                break;
            case "r":
                gc.fillPolygon(new double[]{cx + r, cx - r/2, cx - r/2}, new double[]{cy, cy - r, cy + r}, 3);
                break;
            case "t":
                gc.setFill(Color.TRANSPARENT);
                gc.beginPath();
                gc.moveTo(cx + r/2, cy - r/2);
                gc.arcTo(cx - r, cy - r/2, cx - r, cy + r, r);
                gc.stroke();
                gc.setFill(color);
                gc.fillPolygon(new double[]{cx + r/2, cx, cx + r}, new double[]{cy + r/2, cy + r, cy + r}, 3);
                break;
            default:
                gc.fillOval(cx - r, cy - r, r*2, r*2);
        }
    }
    @Override
    public void setMapShapes(Map<String, List<SumoPosition2D>> mapShapes) {
        this.mapShapes = mapShapes;
    }
    @Override
    public void setScale(double scale) {
        this.SCALE = scale;
    }
    @Override
    public void setOffsetX(double offsetX) {
        this.OFFSET_X = offsetX;
    }
    @Override
    public void setOffsetY(double offsetY) {
        this.OFFSET_Y = offsetY;
    }
    @Override
    public void setShowEdgesID(boolean show) {
        this.showEdgesID = show;
    }
    @Override
    public void setShowVehicleID(boolean show) {
        this.showVehicleID = show;
    }
    @Override
    public void setShowRouteID(boolean show) {
        this.showRouteID = show;
    }
    @Override
    public void setPanel(ControlPanel panel) {
        this.panel = panel;
    }
}