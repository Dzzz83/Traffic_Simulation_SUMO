package app;

import javafx.collections.ObservableList;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wrapperSUMO.ControlPanel;
import de.tudresden.sumo.objects.SumoPosition2D;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MapDraw3D
{
    private static final Logger LOG = LogManager.getLogger(MapDraw3D.class.getName());

    private Group root3D;
    private SubScene subScene;
    private PerspectiveCamera camera;
    private AmbientLight ambientLight;
    private PointLight pointLight;

    private Group roadGroup = new Group();
    private Group vehicleGroup = new Group();
    private Group lightGroup = new Group();

    // map the vehicle id with its box
    private Map<String, Box> vehicleWithNames = new HashMap<>();
    private Map<String, Box> roadWithNames = new HashMap<>();

    public ControlPanel panel;

    public Map<String, List<SumoPosition2D>> mapShapes;
    List<Box> allRoadBoxes = new ArrayList<>();


    public MapDraw3D(double width, double height)
    {
        root3D = new Group();
        // "true" means the car will get smaller the farther it moves
        camera = new PerspectiveCamera(true);
        // camera's range of sight
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);

        // move the camera up and back
        camera.setTranslateX(70);
        camera.setTranslateY(-900);
        camera.setTranslateZ(-600);

        // rotate the camera to look down
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(-45);

        // initialize subScene
        subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        // set the background color
        subScene.setFill(Color.SKYBLUE);

        createLight();

        // add the road and vehicle groups
        // observablelist detects the new groups and notify javafx to render the new items
        ObservableList<Node> children = root3D.getChildren();
        children.addAll(vehicleGroup, roadGroup, lightGroup);

    }
    public void createLight()
    {
        ambientLight = new AmbientLight(Color.WHITE);

        pointLight = new PointLight(Color.LIGHTYELLOW);
        pointLight.setTranslateY(-500);

        ObservableList<Node> lightList = lightGroup.getChildren();
        lightList.addAll(ambientLight, pointLight);
    }

    public SubScene getSubScene()
    {
        return this.subScene;
    }

    public void clearAll()
    {
        // clear everything
        ObservableList<Node> roads = roadGroup.getChildren();
        roads.clear();

        ObservableList<Node> vehicles = vehicleGroup.getChildren();
        vehicles.clear();

        vehicleWithNames.clear();
    }

    private Box createVehicleBox()
    {
        // width, height, depth
        Box vehicleBox = new Box(4.0, 2.0, 4.0);

        // color the car
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.RED);
        vehicleBox.setMaterial(mat);

        return vehicleBox;
    }

    public void updateVehicles()
    {
        if (panel == null)
        {
            return;
        }

        List<String> vehicleIDs = panel.getVehicleIDs();
        for (String id : vehicleIDs)
        {
            // check if the car exists
            if (!vehicleWithNames.containsKey(id))
            {
                // create the car
                Box vehicleBox = createVehicleBox();
                // add in the hash map
                vehicleWithNames.put(id, vehicleBox);
                // add in the vehicleGroup
                ObservableList<Node> vehicle = vehicleGroup.getChildren();
                vehicle.add(vehicleBox);
            }
            // get the current car
            Box currentCarBox = vehicleWithNames.get(id);
            // get the SUMOPosition2D
            SumoPosition2D pos = panel.getPosition(id);
            // assign SUMO X to the car's X
            currentCarBox.setTranslateX(pos.x);
            // move the car up by 1 meter
            currentCarBox.setTranslateY(-1.1);
            // assign SUMO Y to the car's Z
            currentCarBox.setTranslateZ(pos.y);
        }

        vehicleWithNames.entrySet().removeIf(entry -> {
            // check if the car exists
            boolean check = vehicleIDs.contains(entry.getKey());
            // if not exist
            if (!check)
            {
                // get the car's data
                Box currentCarBox = entry.getValue();
                ObservableList<Node> vehicle = vehicleGroup.getChildren();
                // remove the box data from vehicleGroup
                vehicle.remove(currentCarBox);
                // removeIf(true) --> delete from vehicleWithNames
                return true;
            }
            // removeIf(false) --> don't delete
            return false;
        });
    }

    private Box createRoadBox(double width, double length)
    {
        // width, height, depth
        Box roadBox = new Box(width, 0.1, length);

        // color the road
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.BLACK);
        roadBox.setMaterial(mat);

        return roadBox;
    }

    public void drawRoad()
    {
        allRoadBoxes.clear();
        for (Map.Entry<String, List<SumoPosition2D>> data : mapShapes.entrySet())
        {
            // get the points
            List<SumoPosition2D> points = data.getValue();
            for (int i = 0; i < points.size() - 1; i++)
            {

                SumoPosition2D startPoint = points.get(i);
                SumoPosition2D endPoint = points.get(i+1);

                // calculate length
                double dx = endPoint.x - startPoint.x;
                double dz = endPoint.y - startPoint.y;
                double roadLength = Math.sqrt(dx*dx + dz*dz);

                // calculate midPoint
                double midPointX = (startPoint.x + endPoint.x) / 2;
                double midPointY = (startPoint.y + endPoint.y) / 2;
                SumoPosition2D midPoint = new SumoPosition2D(midPointX, midPointY);

                // calculate the angle of the road(*)
                double angle_rad = Math.atan2(dx, dz);
                double angle_deg = Math.toDegrees(angle_rad);

                // create the roadBox
                Box roadBox = createRoadBox(3.2, roadLength);

                roadBox.setTranslateX(midPoint.x);
                roadBox.setTranslateZ(midPoint.y);

                roadBox.setRotationAxis(Rotate.Y_AXIS);
                roadBox.setRotate(angle_deg);

                allRoadBoxes.add(roadBox);
            }

        }
        ObservableList<Node> roadList = roadGroup.getChildren();
        roadList.addAll(allRoadBoxes);
    }

    public SumoPosition2D getMapCenter()
    {
        try
        {
            if (panel != null)
            {
                // get the coords
                List<SumoPosition2D> mapCenterCoords = panel.getNetBoundary();
                if (mapCenterCoords.size() == 2)
                {
                    // get 2 coords
                    SumoPosition2D bottomLeftCoord = mapCenterCoords.get(0);
                    SumoPosition2D topRightCoord = mapCenterCoords.get(1);

                    // assign variable
                    double x = bottomLeftCoord.x;
                    double z = bottomLeftCoord.y;
                    double x1 = topRightCoord.x;
                    double z1 = topRightCoord.y;

                    // find the center
                    double mapCenterX = (x + x1) / 2;
                    double mapCenterZ = (z + z1) / 2;

                    return new SumoPosition2D(mapCenterX, mapCenterZ);
                }
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the center coordinates of the map");
        }
        return new SumoPosition2D(0, 0);
    }
}
