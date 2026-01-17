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

public class MapDraw
{
    private static final Logger LOG = LogManager.getLogger(MapDraw.class.getName());
    private String activeVehicleFilter = "All";

    public void setVehicleFilter(String filter) {
        this.activeVehicleFilter = filter;
    }

    private Canvas canvas;

    public ControlPanel panel;
    public TrafficLightWrapper tlsWrapper;
    public Map<String, List<SumoPosition2D>> mapShapes;

    public double SCALE = 1.0;
    public double OFFSET_X = 0;
    public double OFFSET_Y = 0;

    public boolean showEdgesID = false;
    public boolean showVehicleID = false;
    public boolean showRouteID = false;

    private static final Color ASPHALT_COLOR = Color.web("#404040");
    private static final Color GRASS_COLOR = Color.web("#2E7D32");

    private List<List<SumoPosition2D>> dashedWhiteLines = new ArrayList<>();
    private List<List<SumoPosition2D>> solidCenterLines = new ArrayList<>();

    private final VehicleRenderer carRenderer = new CarRenderer();
    private final VehicleRenderer deliveryRenderer = new DeliveryRenderer();
    private final VehicleRenderer taxiRenderer = new TaxiRenderer();
    private final VehicleRenderer evRenderer = new EvehicleRenderer();

    public MapDraw(Canvas canvas)
    {
        this.canvas = canvas;
    }

    public void drawAll() {
        if (canvas == null) return;

        drawRoads();

        if (tlsWrapper != null && panel != null && panel.isRunning()) {
            drawTrafficLights(canvas.getGraphicsContext2D());
        }

        drawAllVehicles();
    }

    public void drawRoads() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 1. Draw Background
        gc.setFill(GRASS_COLOR);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (mapShapes == null || mapShapes.isEmpty()) return;

        // 2. Generate markings if missing
        if (dashedWhiteLines.isEmpty()) {
            generateLaneSeparators();
        }

        // 3. Debug Mode (Show Edge IDs)
        if (showEdgesID) {
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            for (Map.Entry<String, List<SumoPosition2D>> entry : mapShapes.entrySet()) {
                String laneID = entry.getKey();
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
        // 4. Normal Mode
        else {
            double baseRoadWidth = 4.5 * SCALE;
            if (baseRoadWidth < 2.0) baseRoadWidth = 2.0;

            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.setLineDashes(null);

            // Layer 1: Curb (Light Gray)
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(baseRoadWidth + (1.0 * SCALE));
            for (List<SumoPosition2D> points : mapShapes.values()) {
                drawPolyLine(gc, points);
            }

            // Layer 2: Asphalt (Dark Gray)
            gc.setStroke(ASPHALT_COLOR);
            gc.setLineWidth(baseRoadWidth);
            for (List<SumoPosition2D> points : mapShapes.values()) {
                drawPolyLine(gc, points);
            }

            // Layer 3: White Markings
            if (SCALE > 0.5) {
                double lineWidth = 0.3 * SCALE;

                // Dashed Lines
                gc.setStroke(Color.WHITESMOKE);
                gc.setLineWidth(lineWidth);
                gc.setLineCap(StrokeLineCap.BUTT);
                gc.setLineDashes(2.0 * SCALE, 3.0 * SCALE);

                for (List<SumoPosition2D> points : dashedWhiteLines) {
                    drawPolyLine(gc, points);
                }

                // Solid Lines
                gc.setLineDashes(null);
                gc.setLineWidth(lineWidth);
                gc.setStroke(Color.WHITE);

                for (List<SumoPosition2D> points : solidCenterLines) {
                    drawPolyLine(gc, points);
                }

                gc.setLineDashes(null);
                gc.setLineCap(StrokeLineCap.ROUND);
            }
        }
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
                    gc.setFill(Color.GREEN);
                    gc.fillText(routeID, x, y + 15);
                } catch (Exception e) {}
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    public void drawPolyLine(GraphicsContext gc, List<SumoPosition2D> points)
    {
        if (points.isEmpty()) return;
        double[] xPoints = new double[points.size()];
        double[] yPoints = new double[points.size()];
        for (int i = 0; i < points.size(); i++)
        {
            xPoints[i] = (points.get(i).x * SCALE ) + OFFSET_X;
            yPoints[i] = canvas.getHeight() - ((points.get(i).y * SCALE) + OFFSET_Y);
        }
        gc.strokePolyline(xPoints, yPoints, points.size());
    }

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

    private List<SumoPosition2D> calculateLeftParallelLine(List<SumoPosition2D> points, double offset) {
        List<SumoPosition2D> leftParallelLine = new ArrayList<>();
        if (points.size() < 2) return leftParallelLine;

        for (int i = 0; i < points.size(); i++) {
            double avgNx = 0, avgNy = 0;

            if (i > 0) {
                double dx = points.get(i).x - points.get(i - 1).x;
                double dy = points.get(i).y - points.get(i - 1).y;
                double len = Math.sqrt(dx*dx + dy*dy);
                if (len > 0) {
                    avgNx -= dy / len;
                    avgNy += dx / len;
                }
            }

            if (i < points.size() - 1) {
                double dx = points.get(i + 1).x - points.get(i).x;
                double dy = points.get(i + 1).y - points.get(i).y;
                double len = Math.sqrt(dx*dx + dy*dy);
                if (len > 0) {
                    avgNx -= dy / len;
                    avgNy += dx / len;
                }
            }

            double len = Math.sqrt(avgNx*avgNx + avgNy*avgNy);
            if (len > 0) {
                avgNx /= len;
                avgNy /= len;
            }

            double newX = points.get(i).x + (avgNx * offset);
            double newY = points.get(i).y + (avgNy * offset);
            leftParallelLine.add(new SumoPosition2D(newX, newY));
        }
        return leftParallelLine;
    }

    private void generateLaneSeparators() {
        dashedWhiteLines.clear();
        solidCenterLines.clear();
        if (mapShapes == null) return;

        Map<String, Map<Integer, List<SumoPosition2D>>> edgeGroups = new HashMap<>();

        for (Map.Entry<String, List<SumoPosition2D>> data: mapShapes.entrySet()) {
            String laneID = data.getKey();
            if (laneID.startsWith(":")) continue;
            int underscoreIndex = laneID.lastIndexOf("_");
            if (underscoreIndex != -1) {
                String edgeID = laneID.substring(0, underscoreIndex);
                String indexStr = laneID.substring(underscoreIndex + 1);
                try {
                    int index = Integer.parseInt(indexStr);
                    edgeGroups.computeIfAbsent(edgeID, k -> new TreeMap<>()).put(index, data.getValue());
                } catch (NumberFormatException e) {
                    LOG.error("Something went wrong");
                }
            }
        }

        Set<String> alreadyDrawDashed = new HashSet<>();

        for (Map<Integer, List<SumoPosition2D>> lane : edgeGroups.values()) {
            if (lane.isEmpty()) continue;

            if (lane.size() >= 2) {
                List<Integer> keys = new ArrayList<>(lane.keySet());
                for (int i = 0; i < keys.size() - 1; i++) {
                    List<SumoPosition2D> laneA = lane.get(keys.get(i));
                    List<SumoPosition2D> laneB = lane.get(keys.get(i+1));
                    List<SumoPosition2D> separator = new ArrayList<>();
                    int numPoints = Math.min(laneA.size(), laneB.size());
                    for (int j = 0; j < numPoints; j++) {
                        double midX = (laneA.get(j).x + laneB.get(j).x) / 2.0;
                        double midY = (laneA.get(j).y + laneB.get(j).y) / 2.0;
                        separator.add(new SumoPosition2D(midX, midY));
                    }
                    dashedWhiteLines.add(separator);
                }
            }

            int maxIndex = ((TreeMap<Integer, List<SumoPosition2D>>) lane).lastKey();
            List<SumoPosition2D> leftmostLane = lane.get(maxIndex);
            List<SumoPosition2D> leftParallelLine = calculateLeftParallelLine(leftmostLane, 1.6);

            if (lane.size() > 1) {
                solidCenterLines.add(leftParallelLine);
            } else {
                if (!leftParallelLine.isEmpty()) {
                    int midIdx = leftParallelLine.size() / 2;
                    SumoPosition2D mid = leftParallelLine.get(midIdx);
                    String key = Math.round(mid.x) + "_" + Math.round(mid.y);
                    if (alreadyDrawDashed.add(key)) {
                        dashedWhiteLines.add(leftParallelLine);
                    }
                }
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
}