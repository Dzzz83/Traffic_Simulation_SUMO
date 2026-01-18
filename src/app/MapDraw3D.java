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

/**
 * mapdraw3d provides a three-dimensional visualization of the sumo simulation.
 * it manages a perspective camera, lighting, and translates 2d simulation data
 * into a 3d world using javafx groups and primitives.
 */
public class MapDraw3D implements MapRenderer {
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

    private ControlPanel panel;

    private Map<String, List<SumoPosition2D>> mapShapes;
    List<Box> allRoadBoxes = new ArrayList<>();

    /**
     * Constructs a MapDraw3D instance and initializes the root 3D group.
     * Sets up the hierarchical structure for roads, vehicles, lights, and the camera.
     */
    public MapDraw3D() {
        root3D = new Group();

        // add the road and vehicle groups
        // observablelist detects the new groups and notify javafx to render the new items
        ObservableList<Node> children = root3D.getChildren();
        children.addAll(vehicleGroup, roadGroup, lightGroup, cameraGroup);
    }
    /**
     * Initializes the 3D SubScene with the specified dimensions and rendering settings.
     * Enables depth buffering and anti-aliasing for the 3D environment.
     * @param width The width of the sub-scene in pixels.
     * @param height The height of the sub-scene in pixels.
     */
    public void setSubScene(double width, double height) {
        // initialize subScene
        subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        // set the background color
        subScene.setFill(Color.SKYBLUE);
    }
    /**
     * Configures the perspective camera and its initial orientation.
     * Sets the clipping planes and moves the camera group to a position relative
     * to the map center.
     * @param mapCenterCoord The central coordinate of the SUMO network.
     */
    public void setCamera(SumoPosition2D mapCenterCoord) {
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
    /**
     * initializes the 3d environment by setting up the scene graph, camera, and lighting.
     * it organizes the 3d world into specific groups for roads, vehicles, and lights.
     */
    public void setup() {
        SumoPosition2D mapCenterCoord = getMapCenter();

        // set the camera
        setCamera(mapCenterCoord);

        // set the sub scene
        setSubScene(1200.0, 800);

        // create light at the center
        createLight(mapCenterCoord);
    }
    /**
     * Establishes the lighting environment for the 3D scene.
     * Adds both global ambient light and a specific point light positioned above the map.
     * @param mapCenterCoord The coordinate used to center the point light.
     */
    public void createLight(SumoPosition2D mapCenterCoord) {
        ambientLight = new AmbientLight(Color.WHITE);

        pointLight = new PointLight(Color.LIGHTYELLOW);
        pointLight.setTranslateY(-500);

        ObservableList<Node> lightList = lightGroup.getChildren();
        lightList.addAll(ambientLight, pointLight);

        pointLight.setTranslateX(mapCenterCoord.x);
        pointLight.setTranslateZ(mapCenterCoord.y);
    }

    public SubScene getSubScene() {
        return this.subScene;
    }

    public Group getRoadGroup() {
        return this.roadGroup;
    }
    /**
     * Clears all existing 3D nodes from the road and vehicle groups.
     * This is used to reset the visualization state.
     */
    public void clearAll() {
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

        centralVehicleBox.setTranslateY(-0.7);

        return centralVehicleBox;
    }

    private Box createEngineBox(double width, double height, double depth) {
        Box engineBox = new Box(width, height, depth);
        PhongMaterial mat1 = new PhongMaterial();
        mat1.setDiffuseColor(Color.BLUE);
        engineBox.setMaterial(mat1);

        engineBox.setTranslateZ(-1.75);
        engineBox.setTranslateY(-0.45);

        return engineBox;
    }

    private Box createWindshieldBox(double width, double height, double depth) {
        Box windShieldBox = new Box(width, height, depth);
        PhongMaterial mat2 = new PhongMaterial();
        mat2.setDiffuseColor(Color.BLACK);
        windShieldBox.setMaterial(mat2);

        windShieldBox.setTranslateY(-0.95);
        windShieldBox.setTranslateZ(-1.75);

        return windShieldBox;
    }

    private Group createVehicleTires(double radius, double height) {
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
        leftFrontTire.setTranslateX(-1.1);
        leftFrontTire.setTranslateY(0);
        leftFrontTire.setTranslateZ(-2.0);

        Cylinder rightFrontTire = (Cylinder) carTireGroup.getChildren().get(1);
        rightFrontTire.setTranslateX(1.1);
        rightFrontTire.setTranslateY(0);
        rightFrontTire.setTranslateZ(-2.0);

        Cylinder leftBackTire = (Cylinder) carTireGroup.getChildren().get(2);
        leftBackTire.setTranslateX(-1.1);
        leftBackTire.setTranslateY(0);
        leftBackTire.setTranslateZ(0.5);

        Cylinder rightBackTire = (Cylinder) carTireGroup.getChildren().get(3);
        rightBackTire.setTranslateX(1.1);
        rightBackTire.setTranslateY(0);
        rightBackTire.setTranslateZ(0.5);

        return carTireGroup;
    }

    private Group createVehicle() {
        // width, height, depth
        Box centralVehicleBox = createCentralVehicleBox(2.0, 1.0, 2.0);

        Box engineBox = createEngineBox(2.0, 0.5, 1.5);

        Box windShieldBox = createWindshieldBox(2.0, 0.5, 1.5);

        Group tireGroup =  createVehicleTires(0.5, 0.5);


        Group carGroup = new Group();
        carGroup.getChildren().addAll(tireGroup, centralVehicleBox, engineBox, windShieldBox);
        return carGroup;
    }

    private void rotateVehicle(String vehicleId, Group vehicle) {
        double vehAngle = panel.getVehicleAngle(vehicleId);
        vehicle.setRotationAxis(Rotate.Y_AXIS);
        vehicle.setRotate(vehAngle+180);
    }
    /**
     * Synchronizes 3D vehicle models with the current simulation data.
     * Creates, updates, or removes 3D car models based on the active vehicle list
     * in the simulation.
     */
    public void updateVehicles() {
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
            currentCarBox.setTranslateY(-1.0);
            // assign SUMO Y to the car's Z
            currentCarBox.setTranslateZ(pos.y);

            // rotate the vehicle along the road
            rotateVehicle(id, currentCarBox);
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

    private Box createGrassBox(double width, double length) {
        Box grassBox = new Box(width, 1, length);

        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.rgb(40, 80, 40));
        mat.setSpecularColor(Color.rgb(10, 10, 10));
        grassBox.setMaterial(mat);

        grassBox.setTranslateY(3);
        return grassBox;
    }

    private Box createRoadBox(double width, double length) {
        // width, height, depth
        Box roadBox = new Box(width, 1, length);

        // color the road
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.rgb(40, 40, 40));
        mat.setSpecularColor(Color.rgb(30, 30, 30));
        roadBox.setMaterial(mat);

        return roadBox;
    }
    /**
     * Generates 3D box primitives representing the road network geometry.
     * Calculates the length, position, and rotation of each road segment
     * based on the map's coordinate shapes.
     */
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
                Box roadBox = createRoadBox(4.5, roadLength);

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
    /**
     * Calculates the geographic center point of the simulation map.
     * Retrieves the network boundary and finds the midpoint between the
     * bottom-left and top-right coordinates.
     * @return A SumoPosition2D object representing the map's center.
     */
    public SumoPosition2D getMapCenter() {
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
    /**
     * Adjusts the camera's elevation (Y-axis) in the 3D world.
     * @param isSpacePressed True to fly upward.
     * @param isShiftPressed True to fly downward.
     * @param speed The velocity multiplier for the movement.
     */
    // fly up and fly down method
    public void upDownMovement(boolean isSpacePressed, boolean isShiftPressed, double speed) {
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
            // fly down when "Shift"
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

    /**
     * Converts the camera's current Y-axis rotation from degrees to radians.
     * Used for trigonometric calculations during camera translation.
     * @return The camera angle in radians.
     */
    // get angle and convert to radiance helper function
    public double getRadianAngle() {
        double angle = rotateY.getAngle();
        return Math.toRadians(angle);
    }

    /**
     * Translates the camera horizontally (left/right) relative to its current rotation.
     * Uses trigonometry to ensure movement is aligned with the camera's view direction.
     * @param isAPressed True to move left.
     * @param isDPressed True to move right.
     * @param speed The velocity multiplier for the movement.
     */
    // move left move right method
    public void leftRightMovement(boolean isAPressed, boolean isDPressed, double speed) {
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
    /**
     * Translates the camera forward or backward relative to its current rotation.
     * Uses trigonometry to ensure movement is aligned with the camera's view direction.
     * @param isWPressed True to move forward.
     * @param isSPressed True to move backward.
     * @param speed The velocity multiplier for the movement.
     */
    // move foward and backward method
    public void fowardBackwardMovement(boolean isWPressed, boolean isSPressed, double speed) {
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
    /**
     * updates the camera's rotation based on mouse movement.
     * @param x the change in horizontal mouse position.
     * @param y the change in vertical mouse position.
     * @param sensitivity the multiplier for rotation speed.
     */
    // control camera eye method
        public void controlCameraEye(double x, double y, double sensitivity) {
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
    /**
     * updates the camera position based on current keyboard input.
     * it detects movement keys (wasd, space, shift) and triggers the
     * corresponding translation methods to move the camera in 3d space.
     * @param keyInputSet the set of keys currently pressed by the user.
     */
    public void updateCamera(HashSet<KeyCode> keyInputSet) {
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
    /**
     * retrieves the current translation coordinates of the camera.
     * the coordinates are rounded to the nearest integer for simplified display
     * and stored in a list representing x, y, and z axes.
     * @return a list of doubles containing the rounded camera coordinates.
     */
    // get the camera coordinates
    public List<Double> getCameraCoords() {
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
    /**
     * handles the 3d rendering cycle for each frame.
     * it ensures the road infrastructure is generated once and continuously
     * updates the positions of all vehicle models in the scene.
     */
    @Override
    public void drawAll() {

        if (allRoadBoxes.isEmpty() && mapShapes != null) {
            drawRoad();
        }
        updateVehicles();
    }

    @Override
    public void setMapShapes(Map<String, List<SumoPosition2D>> mapShapes) {
        this.mapShapes = mapShapes;
    }

    @Override
    public void setShowEdgesID(boolean show) {
    }

    @Override
    public void setShowVehicleID(boolean show) {
    }
    @Override
    public void setPanel(ControlPanel panel) {
        this.panel = panel;
    }
    @Override
    public void setScale(double scale) {
    }

    @Override
    public void setOffsetX(double offsetX) {
    }

    @Override
    public void setOffsetY(double offsetY) {
    }

    @Override
    public void setShowRouteID(boolean show){

    }
}
