package app;

import java.util.List;
import java.util.Map;
import de.tudresden.sumo.objects.SumoPosition2D;
import wrapperSUMO.ControlPanel;

/**
 * the maprenderer interface defines the essential contract for simulation visualization.
 * it ensures that any rendering engine, whether 2d or 3d, provides standard methods
 * for drawing the simulation state and configuring visual overlays.
 */
public interface MapRenderer {
    /**
     * executes the primary rendering loop to draw all simulation elements.
     * this typically includes roads, vehicles, and infrastructure components.
     */
    void drawAll();

    /**
     * updates the internal geometry data used to render the road network.
     * @param mapShapes a map containing edge identifiers and their corresponding list of coordinates.
     */
    void setMapShapes(Map<String, List<SumoPosition2D>> mapShapes);

    /**
     * toggles the visibility of road edge identifiers on the display.
     * @param show true to enable id labels, false to hide them.
     */
    void setShowEdgesID(boolean show);
    /**
     * toggles the visibility of vehicle identification labels.
     * @param show true to enable labels, false to hide them.
     */
    void setShowVehicleID(boolean show);
    /**
     * Toggles the visibility of the current route ID for each vehicle.
     * * @param show True to display the route name, false to hide it.
     */
    void setShowRouteID(boolean show);
    /**
     * Sets the control panel reference to allow the renderer to query
     * live simulation data such as vehicle positions, angles, and colors.
     * * @param panel The active ControlPanel instance.
     */
    void setPanel(ControlPanel panel);
    /**
     * Sets the global zoom scale for the visualization.
     * * @param scale The multiplier for coordinate translation (e.g., 1.0 for default).
     */
    void setScale(double scale);
    /**
     * Sets the horizontal translation offset for panning the map view.
     * * @param offsetX The pixel offset to be added to all X-coordinates.
     */
    void setOffsetX(double offsetX);
    /**
     * Sets the vertical translation offset for panning the map view.
     * * @param offsetY The pixel offset to be added to all Y-coordinates.
     */
    void setOffsetY(double offsetY);
}