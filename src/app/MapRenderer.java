package app;

import java.util.List;
import java.util.Map;
import de.tudresden.sumo.objects.SumoPosition2D;

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
}