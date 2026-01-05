package app;

import javafx.collections.ObservableList;
import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
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
    private Group cameraGroup = new Group();

    // map the vehicle id with its box
    private Map<String, Group> vehicleWithNames = new HashMap<>();
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
        children.addAll(vehicleGroup, roadGroup, lightGroup, cameraGroup);
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
        cameraGroup.setTranslateX(mapCenterCoord.x);
        cameraGroup.setTranslateY(-100);
        cameraGroup.setTranslateZ(mapCenterCoord.y - 600);

        cameraGroup.getChildren().add(camera);
        // (*)
        rotateY = new Rotate(0, Rotate.Y_AXIS);
        cameraGroup.getTransforms().add(rotateY);

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

    private Box createCentralVehicleBox(double width, double height, double depth) {
        // width, height, depth
        Box centralVehicleBox = new Box(width, height, depth);

        // color the car
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.RED);
        centralVehicleBox.setMaterial(mat);

        centralVehicleBox.setTranslateY(-2);

        return centralVehicleBox;
    }

    private Box createEngineBox(double width, double height, double depth)
    {
        Box engineBox = new Box(width, height, depth);
        PhongMaterial mat1 = new PhongMaterial();
        mat1.setDiffuseColor(Color.BLUE);
        engineBox.setMaterial(mat1);

        engineBox.setTranslateZ(-3);
        engineBox.setTranslateY(-1);

        return engineBox;
    }

    private Box createWindshieldBox(double width, double height, double depth)
    {
        Box windShieldBox = new Box(width, height, depth);
        PhongMaterial mat2 = new PhongMaterial();
        mat2.setDiffuseColor(Color.BLACK);
        windShieldBox.setMaterial(mat2);

        windShieldBox.setTranslateY(-2);
        windShieldBox.setTranslateZ(-3);

        return windShieldBox;
    }

    private Group createVehicleTires(double radius, double height)
    {
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.BLACK);

        Group carTireGroup = new Group();

        for (int i = 0; i < 4; i++)
        {
            Cylinder carTire = new Cylinder(radius, height);
            carTire.setRotationAxis(Rotate.Z_AXIS);
            carTire.setRotate(90);
            carTire.setMaterial(mat);
            carTireGroup.getChildren().add(carTire);
        }

        Cylinder leftFrontTire = (Cylinder) carTireGroup.getChildren().get(0);
        leftFrontTire.setTranslateX(-2);
        leftFrontTire.setTranslateY(2.5);
        leftFrontTire.setTranslateZ(-2);

        Cylinder rightFrontTire = (Cylinder) carTireGroup.getChildren().get(1);
        rightFrontTire.setTranslateX(2);
        rightFrontTire.setTranslateY(2.5);
        rightFrontTire.setTranslateZ(-2);

        Cylinder leftBackTire = (Cylinder) carTireGroup.getChildren().get(2);
        leftBackTire.setTranslateX(-2);
        leftBackTire.setTranslateY(2.5);
        leftBackTire.setTranslateZ(2);

        Cylinder rightBackTire = (Cylinder) carTireGroup.getChildren().get(3);
        rightBackTire.setTranslateX(2);
        rightBackTire.setTranslateY(2.5);
        rightBackTire.setTranslateZ(2);

        return carTireGroup;
    }

    private Group createVehicle()
    {
        // width, height, depth
        Box centralVehicleBox = createCentralVehicleBox(4.0, 3.0, 4.0);

        Box engineBox = createEngineBox(4.0, 1.0, 3.0);

        Box windShieldBox = createWindshieldBox(4.0, 1.0, 2.0);

        Group tireGroup =  createVehicleTires(0.5, 1);


        Group carGroup = new Group();
        carGroup.getChildren().addAll(tireGroup, centralVehicleBox, engineBox, windShieldBox);
        return carGroup;
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
                    Group aCar = createVehicle();
                    // add in the hash map
                    vehicleWithNames.put(id, aCar);
                    // add in the vehicleGroup
                    ObservableList<Node> vehicle = vehicleGroup.getChildren();
                    vehicle.add(aCar);
                }
                // get the current car
                Group currentCarBox = vehicleWithNames.get(id);
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
                    Group currentCarBox = entry.getValue();
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

    private Box createGrassBox(double width, double length)
    {
        Box grassBox = new Box(width, 1, length);

        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.rgb(40, 80, 40));
        mat.setSpecularColor(Color.rgb(10, 10, 10));
        grassBox.setMaterial(mat);

        grassBox.setTranslateY(3);
        return grassBox;
    }

    private Box createRoadBox(double width, double length)
    {
        // width, height, depth
        Box roadBox = new Box(width, 1, length);

        // color the road
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.rgb(40, 40, 40));
        mat.setSpecularColor(Color.rgb(30, 30, 30));
        roadBox.setMaterial(mat);

        return roadBox;
    }

        public void drawRoad()
        {
            allRoadBoxes.clear();
            Box grassBox = createGrassBox(2000, 2000);
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
            roadList.add(grassBox);
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
            currentPosY = cameraGroup.getTranslateY();

            // fly up when "space"
            if (isSpacePressed)
            {
                // subtract deltaY
                cameraGroup.setTranslateY(currentPosY - deltaY);
            }
            // fly down when "Ctrl"
            else if (isShiftPressed)
            {
                // add deltaY
                cameraGroup.setTranslateY(currentPosY + deltaY);
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
            currentPosX = cameraGroup.getTranslateX();
            currentPosZ = cameraGroup.getTranslateZ();

            // move to the left if "A"
            if (isAPressed)
            {
                // subtract deltaX
                cameraGroup.setTranslateX(currentPosX - deltaX);
                // add deltaY
                cameraGroup.setTranslateZ(currentPosZ + deltaZ);
            }
            // move the right if "D"
            else if (isDPressed)
            {
                // add deltaX
                cameraGroup.setTranslateX(currentPosX + deltaX);
                // subtract deltaZ
                cameraGroup.setTranslateZ(currentPosZ - deltaZ);
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
            currentPosZ = cameraGroup.getTranslateZ();
            currentPosX = cameraGroup.getTranslateX();

            // move foward if "W"
            if (isWPressed)
            {
                // add deltaZ
                cameraGroup.setTranslateZ(currentPosZ + deltaZ);
                // add deltaX
                cameraGroup.setTranslateX(currentPosX + deltaX);
            }
            else if (isSPressed)
            {
                // subtract deltaZ
                cameraGroup.setTranslateZ(currentPosZ - deltaZ);
                // subtract deltaX
                cameraGroup.setTranslateX(currentPosX - deltaX);
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
            double x = cameraGroup.getTranslateX();
            double y = cameraGroup.getTranslateY();
            double z = cameraGroup.getTranslateZ();

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
