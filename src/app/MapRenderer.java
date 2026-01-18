package app;

import java.util.List;
import java.util.Map;
import de.tudresden.sumo.objects.SumoPosition2D;

public interface MapRenderer {
    void drawAll();

    void setMapShapes(Map<String, List<SumoPosition2D>> mapShapes);

    void setShowEdgesID(boolean show);
    void setShowVehicleID(boolean show);
}