package wrapperSUMO;

import de.tudresden.sumo.cmd.Trafficlight;
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoPosition2D;

import java.util.List;
import java.util.ArrayList;

import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.objects.SumoGeometry;
import java.util.Map;
import java.util.HashMap;

public class ControlPanel
{

    // create the connection
    private SumoTraciConnection connection;

    // initialize the wrappers
    private VehicleWrapper vehicleWrapper;
    private TrafficLightWrapper trafficLightWrapper;
    private EdgeWrapper edgeWrapper;
    private RouteWrapper routeWrapper;

    // initialize the boolean value isRunning
    public boolean isRunning = false;

    // default Constructor
    public ControlPanel()
    {
        System.out.println("ControlPanel created - call startSimulation() to begin");
    }

    // create function startSimulation
    public boolean startSimulation()
    {
        try
        {
            // initialize the connection with your specific paths
            connection = new SumoTraciConnection("sumo",
                    "src/map/demo.sumocfg");

            // start the simulation
            connection.runServer();

            // initialize all wrappers
            vehicleWrapper = new VehicleWrapper(connection);
            trafficLightWrapper = new TrafficLightWrapper(connection);
            edgeWrapper = new EdgeWrapper(connection);
            routeWrapper = new RouteWrapper(connection);

            // set the isRunning to true
            isRunning = true;
        }
        catch (Exception e)
        {
            System.out.println("Failed to run the simulation");
            e.printStackTrace();
        }
        return true;
    }

    public void restartSimulation() {
        try {
            if (connection != null) {
                connection.close();
            }
            Thread.sleep(500);
            startSimulation();
            System.out.println("Simulation restarted successfully");
        } catch (Exception e) {
            System.err.println("Failed to restart simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Retrieves the shape (list of X,Y points) for EVERY lane in your map
    public Map<String, List<SumoPosition2D>> getMapShape()
    {
        Map<String, List<SumoPosition2D>> allShapes = new HashMap<>();

        if (!isRunning)
        {
            return allShapes;
        }

        try {
            // get the list of ALL lane IDs in the simulation
            List<String> laneIDs = (List<String>) connection.do_job_get(Lane.getIDList());

            // loop through each lane and retrieve all the lane shapes from SUMO
            for (String laneId : laneIDs) {
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));

                // convert SumoGeometry to a simple Java List
                List<SumoPosition2D> points = new ArrayList<>();
                for (SumoPosition2D p : geometry.coords) {
                    points.add(p);
                }
                allShapes.put(laneId, points);
            }
        } catch (Exception e) {
            System.out.println("Failed to get the shapes of all lanes in map");
            e.printStackTrace();
        }
        return allShapes;
    }

    public void stressTest(int vehicleCount)
    {
        synchronized (connection)
        {
            // check if the simulation is running
            if (!isRunning)
            {
                System.out.println("The simulation is not running");
                return;
            }
            // generate a unique batch id
            String batchID = String.valueOf(System.currentTimeMillis());
            try
            {
                // get all routes
                List<String> routeIDs = routeWrapper.getRouteIDs();
                int currentTime = (int) getCurrentTime();
                if (routeIDs.isEmpty())
                {
                    System.out.println("Can't find any routes on the map");
                    return;
                }

                // declare a routeIndex variable
                int routeIndex = 0;
                for (int i = 0; i < vehicleCount; i++)
                {
                    String vehicleId = "stresstess_" + batchID + "_" + i;
                    // get the current routeID
                    String routeId = routeIDs.get(routeIndex);
                    // add the vehicle in the current time and loop over route id
                    vehicleWrapper.addVehicle(vehicleId, "DEFAULT_VEHTYPE", routeId, currentTime, 0.0, 0.0, (byte)-2);
                    routeIndex = (routeIndex+1) % routeIDs.size();
                }
                System.out.println("Strees Testing successfully");
            }
            catch (Exception e)
            {
                System.out.println("Failed to perform stress test");
                // e.printStackTrace();
            }
        }

    }
    // get the list of all routes
    public List<String> getRouteIDs()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return routeWrapper.getRouteIDs();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get lists of route ids");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // for display routeID next to Vehicle
    public String getVehicleRouteID(String vehicleId) {
        if (!isRunning) {
            return "";
        }
        return vehicleWrapper.getRouteID(vehicleId);
    }

    // function step
    public void step()
    {
        // check if the simulation is running
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            // do timestep
            connection.do_timestep();
            // get current time
            double time = getCurrentTime();
            System.out.println("Step to: " + time);
        }
        catch (Exception e)
        {
            System.out.println("Failed to time step the simulation");
            e.printStackTrace();
        }
    }

    // function getCurrentTime()
    public double getCurrentTime()
    {
        // check if running
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0.0;
        }
        try
        {
            // use the SUMO command
            return (Double) connection.do_job_get(Simulation.getTime());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get current time");
            e.printStackTrace();
        }
        return 0.0;
    }


    public int getVehicleCount()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0;
        }
        try
        {
            return vehicleWrapper.getVehicleCount();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the number of vehicles");
            e.printStackTrace();
        }
        return 0;
    }

    // create function setColor
    public void setColor(String vehicleId, int r, int g, int b, int a)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.setColor(vehicleId, r, g, b, a);
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the color");
            e.printStackTrace();
        }
    }

    // create function changeLane
    public void changeLane(String vehicleId, int laneIndex, int duration)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.changeLane(vehicleId, laneIndex, duration);
        }
        catch (Exception e)
        {
            System.out.println("Failed to change lane for vehicle: " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function slowDown
    public void slowDown(String vehicleId, double newSpeed, int duration)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.slowDown(vehicleId, newSpeed, duration);
        }
        catch (Exception e)
        {
            System.out.println("Failed to slow down vehicle: " + vehicleId);
            e.printStackTrace();
        }
    }


    public List<String> getVehicleIDs()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return vehicleWrapper.getVehicleIDs();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get lists of vehicle ids");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public double getVehicleSpeed(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getVehicleSpeed(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the speed");
            e.printStackTrace();
        }
        return 0.0;
    }

    // create function getPosition
    public SumoPosition2D getPosition(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return new SumoPosition2D(0.0, 0.0);
        }
        try
        {
            return vehicleWrapper.getPosition(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the position of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return new SumoPosition2D(0.0, 0.0);
    }

    // create function getLaneID
    public String getLaneID(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return "";
        }
        try
        {
            return vehicleWrapper.getLaneID(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the laneID of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // create function getRoadID
    public String getRoadID(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return "";
        }
        try
        {
            return vehicleWrapper.getRoadID(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the roadId of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // create function addVehicle
    public void addVehicle(String vehicleId, String typeId, String routeId, int depart, double position, double speed, byte lane)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.addVehicle(vehicleId, typeId, routeId, depart, position, speed, lane);
        }
        catch (Exception e)
        {
            System.out.println("Failed to add the vehicle " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function setRouteID
    public void setRouteID(String vehicleId, String routeId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.setRouteID(vehicleId, routeId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the route of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function removeVehicle
    public void removeVehicle(String vehicleId, byte reason)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.removeVehicle(vehicleId, reason);
        }
        catch (Exception e)
        {
            System.out.println("Failed to remove the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function setSpeed
    public void setSpeed(String vehicleId, double speed)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.setSpeed(vehicleId, speed);
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the speed of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    public int setMaxSpeed(String edgeID, double speed) {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0;
        }
        try
        {
            return edgeWrapper.setMaxSpeed(edgeID, speed);
        }
        catch (Exception e)
        {
            System.out.println("Failed to set max speed");
            e.printStackTrace();
        }
        return 0;
    }

    public double getMeanSpeed(String edgeID) {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0.0;
        }
        try
        {
            return edgeWrapper.getMeanSpeed(edgeID);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get mean speed");
            e.printStackTrace();
        }
        return 0.0;
    }

    public int setGlobalMaxSpeed(double speed) {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0;
        }
        return edgeWrapper.setGlobalMaxSpeed(speed);
    }

    public double getGlobalMeanSpeed() {
        if (!isRunning) {
            System.out.println("The simulation is not running");
            return 0.0;
        }

        try {
            List<String> allVehicles = getVehicleIDs();
            if (allVehicles.isEmpty()) {
                return 0.0;
            }

            double totalSpeed = 0.0;

            for (String vehicleId : allVehicles) {
                totalSpeed += getVehicleSpeed(vehicleId);
            }

            return (totalSpeed / allVehicles.size());
        } catch (Exception e) {
            System.out.println("Failed to calculate global mean speed");
            e.printStackTrace();
        }
        return 0.0;
    }

    // create function getDistance
    public double getDistance(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getDistance(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the distance of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }

    // create function getCo2
    public double getCO2Emission(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getCO2Emission(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the CO2 Emission:  " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }

    public List<String> getTrafficLightIDs()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return trafficLightWrapper.getTrafficLightIDs();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get traffic light IDs");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public int getTrafficLightCount()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0;
        }
        try
        {
            return trafficLightWrapper.getTrafficLightCount();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get number of traffic lights");
            e.printStackTrace();
        }
        return 0;
    }

    public String getRedYellowGreenState(String trafficLightId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return "";
        }
        try
        {
            return trafficLightWrapper.getRedYellowGreenState(trafficLightId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get traffic light state");
            e.printStackTrace();
        }
        return "";
    }

    public int getEdgeCount()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0;
        }
        try
        {
            return edgeWrapper.getEdgeCount();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get number of edges");
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getEdgeIDs()
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return edgeWrapper.getEdgeIDs();
        }
        catch (Exception e)
        {
            System.out.println("Failed to get edge IDs");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public int getLaneNumber(String edgeId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0;
        }
        try
        {
            return edgeWrapper.getLaneNumber(edgeId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get lane number");
            e.printStackTrace();
        }
        return 0;
    }

    public void changeTarget(String vehicleId, String edgeId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.changeTarget(vehicleId, edgeId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to change the destination of that vehicle");
            e.printStackTrace();
        }
    }

    // create function stopSimulation
    public void stopSimulation()
    {
        if (connection == null)
        {
            System.out.println("The simulation is not running");
            return;
        }

        try
        {
            // close the connection
            connection.close();
            isRunning = false;
        }
        catch (Exception e)
        {
            System.out.println("Can't stop the simulation");
            e.printStackTrace();
        }
    }

    // create function getVehicleAngle
    public double getVehicleAngle(String vehicleId)
    {
        if (!isRunning)
        {
            System.out.println("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getAngle(vehicleId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the angle of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }
    // Get the position of traffic light
    public Map<String, List<SumoPosition2D>> get_traffic_light_pos(String trafficLightId)
    {
        if (!isRunning)
        {
            return new HashMap<>();
        }
        try
        {
            Map<String, List<SumoPosition2D>> lane_position = new HashMap<>();
            Map<String, String> edges = this.get_controlled_lanes(trafficLightId);
            for (Map.Entry<String, String> entry : edges.entrySet())
            {
                String edgeid = entry.getKey();
                String laneid = entry.getValue();
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneid));
                List<SumoPosition2D> shape = geometry.coords;
                lane_position.put(edgeid, shape);
            }
            return lane_position;
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the traffic light pos in map");
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    // get the incomming controlled road for traffic light
    public Map<String, String> get_controlled_lanes(String trafficLightId)
    {
        if (!isRunning)
        {
            return new HashMap<>();
        }
        try
        {
            // get controlled lanes
            List<String> controlled_lanes =(List<String>) connection.do_job_get(Trafficlight.getControlledLanes(trafficLightId));
            // get edge
            Map<String, String> edges = new HashMap<>();
            for (String landid : controlled_lanes)
            {
                String edgeid = landid.split("_")[0];
                edges.putIfAbsent(edgeid, landid);
            }
            return edges;
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the controlled lanes in map");
            e.printStackTrace();
        }
        return new HashMap<>();
    }


    // Set traffic light to automatic mode
    public void set_automatic_state(String trafficLightId)
    {
        if (!isRunning)
        {
            return;
        }
        try
        {
            trafficLightWrapper.setautomaticmode(trafficLightId);
        }
        catch (Exception e)
        {
            System.out.println("Failed to set auto traffic light");
        }
        return;
    }

    // Logic to turn ON a specific light (Set program to default "0")
    public void turnOnTrafficLight(String tlsID)
    {
        try {
            connection.do_job_set(Trafficlight.setProgram(tlsID, "0"));
            System.out.println("Traffic Light " + tlsID + " set to ON (Program 0)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Logic to turn OFF a specific light (Set program to "off")
    public void turnOffTrafficLight(String tlsID)
    {
        try {
            connection.do_job_set(Trafficlight.setProgram(tlsID, "off"));
            System.out.println("Traffic Light " + tlsID + " set to OFF");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnOffAllLights() {
        try {
            // 1. Get the list of all Traffic Light IDs
            List<String> tlIDs = (List<String>) connection.do_job_get(Trafficlight.getIDList());

            // 2. Iterate and set program to "off"
            for (String id : tlIDs) {
                connection.do_job_set(Trafficlight.setProgram(id, "off"));
            }
            System.out.println("All traffic lights turned OFF.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBaseStateString(String tlsID) throws Exception
    {
        return (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(tlsID));
    }

    // turn all light to red
    public void turn_all_light_red()
    {
        try
        {
            List<String> tlIDs = (List<String>) connection.do_job_get(Trafficlight.getIDList());

            for (String id : tlIDs)
            {
                String currentState = getBaseStateString(id);
                int length = currentState.length();
                String allRedState = "r".repeat(length);

                connection.do_job_set(Trafficlight.setRedYellowGreenState(id, allRedState));
            }
        }
        catch (Exception e)
        {
            System.err.println("Error forcing all lights to RED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // turn all lights to greeen
    public void turn_all_light_green() {
        try {
            List<String> tlIDs = (List<String>) connection.do_job_get(Trafficlight.getIDList());

            for (String id : tlIDs) {
                String currentState = getBaseStateString(id);
                int length = currentState.length();

                String allGreenState = "g".repeat(length);
                connection.do_job_set(Trafficlight.setRedYellowGreenState(id, allGreenState));
            }
        }
        catch (Exception e)
        {
            System.err.println("Error forcing all lights to GREEN: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void turnOnAllLights() {
        try {
            // 1. Get the list of all Traffic Light IDs
            List<String> tlIDs = (List<String>) connection.do_job_get(Trafficlight.getIDList());

            // 2. Iterate and set program to "0" (default)
            for (String id : tlIDs) {
                connection.do_job_set(Trafficlight.setProgram(id, "0"));
            }
            System.out.println("All traffic lights turned ON.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // create function isRunning()
    public boolean isRunning()
    {
        return isRunning;
    }
}