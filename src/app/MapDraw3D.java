package app;

import javafx.collections.ObservableList;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import wrapperSUMO.ControlPanel;
import de.tudresden.sumo.objects.SumoPosition2D;

public class MapDraw3D
{
    private Group root3D;
    private SubScene subScene;
    private PerspectiveCamera camera;

    private Group roadGroup = new Group();
    private Group vehicleGroup = new Group();

    // map the vehicle id with its box
    private Map<String, Box> vehicleWithNames = new HashMap<>();

    public ControlPanel panel;

    public MapDraw3D(double width, double height)
    {
        root3D = new Group();
        // "true" means the car will get smaller the farther it moves
        camera = new PerspectiveCamera(true);
        // camera's range of sight
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        // move the camera up and back
        camera.setTranslateX(0);
        camera.setTranslateY(-500);
        camera.setTranslateZ(-800);

        // rotate the camera to look down
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(-45);

        // initialize subScene
        subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        // set the background color
        subScene.setFill(Color.SKYBLUE);

        // add the road and vehicle groups
        // observablelist detects the new groups and notify javafx to render the new items
        ObservableList<Node> children = root3D.getChildren();
        children.addAll(vehicleGroup, roadGroup);
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
            currentCarBox.setTranslateY(-1.0);
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
}
