package app;

import javafx.collections.ObservableList;
import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.*;

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

    private Rotate rotateY;
    private Rotate rotateX;

    private Group roadGroup = new Group();
    private Group vehicleGroup = new Group();
    private Group lightGroup = new Group();

    // map the vehicle id with its box
    private Map<String, Box> vehicleWithNames = new HashMap<>();
    private Map<String, Box> roadWithNames = new HashMap<>();

    public ControlPanel panel;

    public Map<String, List<SumoPosition2D>> mapShapes;
    List<Box> allRoadBoxes = new ArrayList<>();

    public MapDraw3D()
    {
        root3D = new Group();

        // add the road and vehicle groups
        // observablelist detects the new groups and notify javafx to render the new items
        ObservableList<Node> children = root3D.getChildren();
        children.addAll(vehicleGroup, roadGroup, lightGroup);
    }

    public void setSubScene(double width, double height)
    {
        // initialize subScene
        subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        // set the background color
        subScene.setFill(Color.SKYBLUE);
    }

    public void setCamera(SumoPosition2D mapCenterCoord)
    {

        // "true" means the car will get smaller the farther it moves
        camera = new PerspectiveCamera(true);
        // camera's range of sight
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);

        // move the camera up and back
        camera.setTranslateX(mapCenterCoord.x);
        camera.setTranslateY(-900);
        camera.setTranslateZ(mapCenterCoord.y - 600);

        // rotate the camera to look down
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(-45);
        // (*)
        rotateY = new Rotate(0, Rotate.Y_AXIS);
        camera.getTransforms().add(rotateY);

        // (*)
        rotateX = new Rotate(0, Rotate.X_AXIS);
        camera.getTransforms().add(rotateX);
    }

    public void setup()
    {
        SumoPosition2D mapCenterCoord = getMapCenter();

        // set the camera
        setCamera(mapCenterCoord);

        // set the sub scene
        setSubScene(1200.0, 800);

        // create light at the center
        createLight(mapCenterCoord);
    }

    public void createLight(SumoPosition2D mapCenterCoord)
    {
        ambientLight = new AmbientLight(Color.WHITE);

        pointLight = new PointLight(Color.LIGHTYELLOW);
        pointLight.setTranslateY(-500);

        ObservableList<Node> lightList = lightGroup.getChildren();
        lightList.addAll(ambientLight, pointLight);

        pointLight.setTranslateX(mapCenterCoord.x);
        pointLight.setTranslateZ(mapCenterCoord.y);
    }

    public SubScene getSubScene()
    {
        return this.subScene;
    }

    public Group getRoadGroup()
    {
        return this.roadGroup;
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
        mat.setDiffuseColor(Color.GREEN);
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

                // set the position of the road box
                roadBox.setTranslateX(midPoint.x);
                roadBox.setTranslateZ(midPoint.y);

                // set the rotation axis of the road box
                roadBox.setRotationAxis(Rotate.Y_AXIS);
                roadBox.setRotate(angle_deg);

                // add in the list
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

    // fly up and fly down method
    public void upDownMovement(boolean isSpacePressed, boolean isShiftPressed, double speed)
    {
        // variable to store current Y-Axis
        double currentPosY;

        // variable to store the change in Y-Axis
        double deltaY = 1 * speed;

        try
        {
            // get the current y
            currentPosY = camera.getTranslateY();

            // fly up when "space"
            if (isSpacePressed)
            {
                // subtract deltaY
                camera.setTranslateY(currentPosY - deltaY);
            }
            // fly down when "Ctrl"
            else if (isShiftPressed)
            {
                // add deltaY
                camera.setTranslateY(currentPosY + deltaY);
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to move the camera up and down");
        }
    }

    // get angle and convert to radiance helper function
    public double getRadianAngle()
    {
        double angle = rotateY.getAngle();
        return Math.toRadians(angle);
    }


    // move left move right method
    public void leftRightMovement(boolean isAPressed, boolean isDPressed, double speed)
    {
        // get the camera's angle
        double cameraAngle = getRadianAngle();

        // variable to store distance traveled
        double movingDistance = 1 * speed;

        // variables to store X-Axis and Z-Axis
        double currentPosX;
        double currentPosZ;

        // variables to store changes in X-axis and Y-axis
        // use Trigonometry to find deltaX and deltaZ
        double deltaX = Math.cos(cameraAngle) * movingDistance;
        double deltaZ = Math.sin(cameraAngle) * movingDistance;

        try
        {
            // get the current position
            currentPosX = camera.getTranslateX();
            currentPosZ = camera.getTranslateZ();

            // move to the left if "A"
            if (isAPressed)
            {
                // subtract deltaX
                camera.setTranslateX(currentPosX - deltaX);
                // add deltaY
                camera.setTranslateZ(currentPosZ + deltaZ);
            }
            // move the right if "D"
            else if (isDPressed)
            {
                // add deltaX
                camera.setTranslateX(currentPosX + deltaX);
                // subtract deltaZ
                camera.setTranslateZ(currentPosZ - deltaZ);
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to move the camera left and right");
        }
    }

    // move foward and backward method
    public void fowardBackwardMovement(boolean isWPressed, boolean isSPressed, double speed)
    {
        // get the camera's angle
        double cameraAngle = getRadianAngle();

        // variable to store distance moved
        double movingDistance = 1 * speed;

        // variables to store changes in X-axis and Y-axis
        // use Trigonometry to find deltaX and deltaZ
        double deltaX = Math.sin(cameraAngle) * movingDistance;
        double deltaZ = Math.cos(cameraAngle) * movingDistance;

        // variable to store the current position
        double currentPosZ;
        double currentPosX;

        try
        {
            // find the current position
            currentPosZ = camera.getTranslateZ();
            currentPosX = camera.getTranslateX();

            // move foward if "W"
            if (isWPressed)
            {
                // add deltaZ
                camera.setTranslateZ(currentPosZ + deltaZ);
                // add deltaX
                camera.setTranslateX(currentPosX + deltaX);
            }
            else if (isSPressed)
            {
                // subtract deltaZ
                camera.setTranslateZ(currentPosZ - deltaZ);
                // subtract deltaX
                camera.setTranslateX(currentPosX - deltaX);
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to move the camera left and right");
        }
    }

        // control camera eye method
        public void controlCameraEye(double x, double y, double sensitivity)
        {
            // get the current angle
            double currentAngleX = rotateX.getAngle();
            double currentAngleY = rotateY.getAngle();

            // variables to store the change in mouse movement
            double deltaX = x * sensitivity;
            double deltaY = y * sensitivity;

            // calculate the new angle
            double deltaAngleX = currentAngleX - deltaY;
            double deltaAngleY = currentAngleY - deltaX;

            // set the vertical movement limit
            deltaAngleX = Math.max(deltaAngleX, -90);
            deltaAngleX = Math.min(deltaAngleX, 90);

            // update the angle
            rotateY.setAngle(deltaAngleY);
            rotateX.setAngle(deltaAngleX);
        }
    public void updateCamera(HashSet<KeyCode> keyInputSet)
    {
        double speed = 5.0;

        boolean isWPressed = false;
        if (keyInputSet.contains(KeyCode.W))
        {
            isWPressed = true;
        }
        
        boolean isSPressed = false;
        if (keyInputSet.contains(KeyCode.S))
        {
            isSPressed = true;
        }
        
        boolean isAPressed = false;
        if (keyInputSet.contains(KeyCode.A))
        {
            isAPressed = true;
        }
        
        boolean isDPressed = false;
        if (keyInputSet.contains(KeyCode.D))
        {
            isDPressed = true;
        }
        
        boolean isSpacePressed = false;
        if (keyInputSet.contains(KeyCode.SPACE))
        {
            isSpacePressed = true;
        }
        
        boolean isShiftPressed = false;
        if (keyInputSet.contains(KeyCode.SHIFT))
        {
            isShiftPressed = true;
        }


        upDownMovement(isSpacePressed, isShiftPressed, speed);
        leftRightMovement(isAPressed, isDPressed, speed);
        fowardBackwardMovement(isWPressed, isSPressed, speed);
    }

    // get the camera coordinates
    public List<Double> getCameraCoords()
    {
        try
        {
            // get the coords
            double x = camera.getTranslateX();
            double y = camera.getTranslateY();
            double z = camera.getTranslateZ();

            // round the coords
            x = Math.round(x);
            y = Math.round(y);
            z = Math.round(z);

            // create a list to store them
            List<Double> cameraCoords = new ArrayList<>();

            // add the coords to the list
            cameraCoords.add(x);
            cameraCoords.add(y);
            cameraCoords.add(z);

            return cameraCoords;
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the coordinates of the camera");
        }
        return new ArrayList<>();
    }

}
