/*
1. Vehicles
2. Traffic Lights
3. Map/Routes
4. Statistics
 */

package wrapperSUMO;

import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.objects.*;
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import javafx.scene.paint.Color;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Route;

import java.util.*;


import de.tudresden.sumo.cmd.Lane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ControlPanel
{
    private static final Logger LOG = LogManager.getLogger(ControlPanel.class.getName());
    // create the connection
    private SumoTraciConnection connection;

    // initialize the wrappers
    private VehicleWrapper vehicleWrapper;
    private TrafficLightWrapper trafficLightWrapper;
    private TrafficConnectInfo trafficConnectInfo;
    private EdgeWrapper edgeWrapper;
    private RouteWrapper routeWrapper;
    private LaneWrapper laneWrapper;
    private SimulationWrapper simulationWrapper;

    // initialize the boolean value isRunning
    public boolean isRunning = false;

    // default Constructor
    public ControlPanel()
    {
        LOG.info("ControlPanel created - call startSimulation() to begin");
    }

    // create function startSimulation
    public boolean startSimulation()
    {
        try
        {
            // initialize the connection with specific paths
            connection = new SumoTraciConnection("sumo",
                    "src/SumoConfig/demo.sumocfg");

            // start the simulation
            connection.runServer();

            // initialize all wrappers
            vehicleWrapper = new VehicleWrapper(connection);
            trafficLightWrapper = new TrafficLightWrapper(connection);
            edgeWrapper = new EdgeWrapper(connection);
            routeWrapper = new RouteWrapper(connection);
            laneWrapper = new LaneWrapper(connection);
            simulationWrapper = new SimulationWrapper(connection);

            // set the isRunning to true
            isRunning = true;
        }
        catch (Exception e)
        {
            LOG.error("Failed to run the simulation");
        }
        return true;
    }

    // create function stopSimulation
    public void stopSimulation()
    {
        if (connection == null)
        {
            LOG.error("The simulation is not running");
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
            LOG.error("Can't stop the simulation");
        }
    }

    // restartSimulation function
    public void restartSimulation() {
        try {
            // check if a connection exists and close it
            if (connection != null) {
                connection.close();
            }
            // pause for 0.5 seconds to let the OS fully release the network port and resources
            Thread.sleep(500);
            // call the startSimulation method to start a brand-new simulation
            startSimulation();
            // log the success message
            LOG.info("Simulation restarted successfully");
        } catch (Exception e) {
            // if the restart fails, log the error and print details for debugging
            LOG.error("Failed to restart simulation: " + e.getMessage());
            
        }
    }

    // function step
    public void step()
    {
        // check if the simulation is running
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            // do timestep
            connection.do_timestep();
            // get current time
            double time = getCurrentTime();
            LOG.info("Step to: " + time);
        }
        catch (Exception e)
        {
            LOG.error("Failed to time step the simulation");

        }
    }

    // create function isRunning()
    public boolean isRunning()
    {
        return isRunning;
    }

    public SumoTraciConnection getConnection() {
        return this.connection;
    }

    // ------------------------------------------
    // VEHICLE METHODS
    // ------------------------------------------

    // create function addVehicle
    public void addVehicle(String vehicleId, String typeId, String routeId, int depart, double position, double speed, byte lane)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.addVehicle(vehicleId, typeId, routeId, depart, position, speed, lane);
        }
        catch (Exception e)
        {
            LOG.error("Failed to add the vehicle " + vehicleId);
            
        }
    }

    // create function removeVehicle
    public void removeVehicle(String vehicleId, byte reason)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.removeVehicle(vehicleId, reason);
        }
        catch (Exception e)
        {
            LOG.error("Failed to remove the vehicle:  " + vehicleId);
            
        }
    }
    // stress test method
    public void stressTest(int vehicleCount)
    {
        synchronized (connection)
        {
            // check if the simulation is running
            if (!isRunning)
            {
                LOG.error("The simulation is not running");
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
                    LOG.error("Can't find any routes on the map");
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
                LOG.info("Strees Testing successfully");
            }
            catch (Exception e)
            {
                LOG.error("Failed to perform stress test");
                // 
            }
        }

    }

    // create function setColor
    public void setColor(String vehicleId, int r, int g, int b, int a)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.setColor(vehicleId, r, g, b, a);
        }
        catch (Exception e)
        {
            LOG.error("Failed to set the color");
            
        }
    }

    // gets the current color of a vehicle from SUMO and converts it for JavaFX
    public Color getVehicleColor(String id) {
        // if the simulation isn't running, return a default yellow color
        if (!isRunning) return Color.YELLOW;
        try {
            // use TraCI to request the raw color data (SumoColor object)
            // from the SUMO backend for this specific vehicle ID.
            SumoColor sc = (SumoColor) connection.do_job_get(Vehicle.getColor(id));

            // Java bytes are "signed" (-128 to 127), but colors
            // are "unsigned" (0 to 255). Using '& 0xFF' to convert
            // potential negative values into the correct positive integers
            int r = sc.r & 0xFF;
            int g = sc.g & 0xFF;
            int b = sc.b & 0xFF;
            // convert the Alpha (transparency) channel from a 0-255 byte
            // to a 0.0-1.0 double, which is what the JavaFX Color.rgb method requires
            double a = (sc.a & 0xFF) / 255.0;

            // create and return the final JavaFX Color object to the Controller
            return Color.rgb(r, g, b, a);
        } catch (Exception e) {
            // if the vehicle ID doesn't exist yet or the connection drops,
            // log the error and return Yellow to prevent the GUI from crashing
            System.err.println("TraCI Color Error for " + id + ": " + e.getMessage());
            return Color.YELLOW;
        }
    }

    // create function changeLane
    public void changeLane(String vehicleId, int laneIndex, int duration)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.changeLane(vehicleId, laneIndex, duration);
        }
        catch (Exception e)
        {
            LOG.error("Failed to change lane for vehicle: " + vehicleId);
            
        }
    }

    // create function slowDown
    public void slowDown(String vehicleId, double newSpeed, int duration)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.slowDown(vehicleId, newSpeed, duration);
        }
        catch (Exception e)
        {
            LOG.error("Failed to slow down vehicle: " + vehicleId);
            
        }
    }

    public void changeTarget(String vehicleId, String edgeId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.changeTarget(vehicleId, edgeId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to change the destination of that vehicle");
            
        }
    }

    // create function setRouteID
    public void setRouteID(String vehicleId, String routeId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.setRouteID(vehicleId, routeId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to set the route of the vehicle:  " + vehicleId);
            
        }
    }

    // create function setSpeed
    public void setSpeed(String vehicleId, double speed)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return;
        }
        try
        {
            vehicleWrapper.setSpeed(vehicleId, speed);
        }
        catch (Exception e)
        {
            LOG.error("Failed to set the speed of the vehicle:  " + vehicleId);
            
        }
    }

    public List<String> getVehicleIDs()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return vehicleWrapper.getVehicleIDs();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get lists of vehicle ids");
            
        }
        return new ArrayList<>();
    }

    public String getVehicleTypeID(String vehicleID) {
        if (!isRunning) {
            return "DEFAULT_VEHTYPE";
        }
        try {
            //Delegate to the wrapper
            return vehicleWrapper.getVehicleTypeID(vehicleID);
        } catch (Exception e) {
            System.err.println("Error getting type for " + vehicleID);
            return "DEFAULT_VEHTYPE";
        }
    }

    public int getVehicleCount()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0;
        }
        try
        {
            return vehicleWrapper.getVehicleCount();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the number of vehicles");
            
        }
        return 0;
    }

    public double getVehicleSpeed(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getVehicleSpeed(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the speed");
            
        }
        return 0.0;
    }

    // create function getPosition
    public SumoPosition2D getPosition(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new SumoPosition2D(0.0, 0.0);
        }
        try
        {
            return vehicleWrapper.getPosition(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the position of the vehicle " + vehicleId);
            
        }
        return new SumoPosition2D(0.0, 0.0);
    }

    // create function getVehicleAngle
    public double getVehicleAngle(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getAngle(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the angle of the vehicle " + vehicleId);
            
        }
        return 0.0;
    }

    // create function getLaneID
    public String getLaneID(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return "";
        }
        try
        {
            return vehicleWrapper.getLaneID(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the laneID of the vehicle " + vehicleId);
            
        }
        return "";
    }

    // create function getRoadID
    public String getRoadID(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return "";
        }
        try
        {
            return vehicleWrapper.getRoadID(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the roadId of the vehicle " + vehicleId);
            
        }
        return "";
    }

    // for display routeID next to Vehicle
    public String getVehicleRouteID(String vehicleId) {
        if (!isRunning) {
            return "";
        }
        return vehicleWrapper.getRouteID(vehicleId);
    }

    // create function getDistance
    public double getDistance(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getDistance(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the distance of the vehicle:  " + vehicleId);
            
        }
        return 0.0;
    }

    // create function getCo2
    public double getCO2Emission(String vehicleId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            return vehicleWrapper.getCO2Emission(vehicleId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the CO2 Emission:  " + vehicleId);
            
        }
        return 0.0;
    }

    // -----------------------------------------
    // TRAFFIC LIGHT METHODS
    // ------------------------------------------

    public TrafficLightWrapper getTrafficLightWrapper() {
        return this.trafficLightWrapper;
    }

    public List<String> getTrafficLightIDs()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return trafficLightWrapper.getTrafficLightIDs();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get traffic light IDs");
            
        }
        return new ArrayList<>();
    }

    public int getTrafficLightCount()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0;
        }
        try
        {
            return trafficLightWrapper.getTrafficLightCount();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get number of traffic lights");
            
        }
        return 0;
    }

    // get the switching time between two signals
    public double getNextSwitchTime(String tlsID) {
        if (!isRunning) return -1.0;
        try
        {
            return (double) connection.do_job_get(Trafficlight.getPhaseDuration(tlsID));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get next switch time for " + tlsID);
            
        }
        return -1.0;
    }

    public String getRedYellowGreenState(String trafficLightId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return "";
        }
        try
        {
            return trafficLightWrapper.getRedYellowGreenState(trafficLightId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get traffic light state");
            
        }
        return "";
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
            LOG.error("Failed to get the traffic light pos in map");
            
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
            LOG.error("Failed to get the controlled lanes in map");
            
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
            LOG.error("Failed to set auto traffic light");
        }
        return;
    }

    // Logic to turn ON a specific light (Set program to default "0")
    public void turnOnTrafficLight(String tlsID)
    {
        try {
            connection.do_job_set(Trafficlight.setProgram(tlsID, "0"));
            LOG.error("Traffic Light " + tlsID + " set to ON (Program 0)");
        }
        catch (Exception e)
        {
            LOG.error("Failed to turn on the traffic lights");
        }
    }

    // Logic to turn OFF a specific light (Set program to "off")
    public void turnOffTrafficLight(String tlsID)
    {
        try {
            connection.do_job_set(Trafficlight.setProgram(tlsID, "off"));
            LOG.error("Traffic Light " + tlsID + " set to OFF");
        } catch (Exception e) {
            
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
            LOG.error("All traffic lights turned OFF.");
        } catch (Exception e) {
            
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
            LOG.error("All traffic lights turned ON.");
        } catch (Exception e) {
            
        }
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
            
        }
    }

    private String getBaseStateString(String tlsID) throws Exception
    {
        return (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(tlsID));
    }

    // ------------------------------------------
    // MAP, ROUTES & EDGES
    // -------------------------------------------

    // get the lane id list
    public List<String> getLaneIDList()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return laneWrapper.getLaneIDList();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get lists of lane ids");
        }
        return new ArrayList<>();
    }

    // get the lane shape
    public List<SumoPosition2D> getLaneShape(String laneId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return laneWrapper.getLaneShape(laneId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get lists of lane shapes");
        }
        return new ArrayList<>();
    }

    public Map<String, List<SumoPosition2D>> getMapShape()
    {
        Map<String, List<SumoPosition2D>> allShapes = new HashMap<>();

        if (!isRunning)
        {
            return allShapes;
        }

        try {
            // get the list of ALL lane IDs in the simulation
            List<String> laneIDs = laneWrapper.getLaneIDList();

            // loop through each lane and retrieve all the lane shapes from SUMO
            for (String laneId : laneIDs) {
                List<SumoPosition2D> points = laneWrapper.getLaneShape(laneId);
                allShapes.put(laneId, points);
            }
        } catch (Exception e) {
            LOG.error("Failed to get the shapes of all lanes in map");
            e.printStackTrace();
        }
        return allShapes;
    }
    // get the list of all routes
    public List<String> getRouteIDs()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return routeWrapper.getRouteIDs();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get lists of route ids");
            
        }
        return new ArrayList<>();
    }

    public void addRoute(String routeId, List<String> edgeIds) {
        if (!isRunning && edgeIds.isEmpty()) return;
        try {
            SumoStringList edgeList = new SumoStringList();
            for  (String edge : edgeIds) {
                edgeList.add(edge);
            }
            connection.do_job_set(Route.add(routeId, edgeList));
        } catch (Exception e) {
            LOG.error("Failed to add route " + routeId);
        }
    }

    // Get valid outgoing edges connected to a specific edge
    public List<String> getValidEdges(String edgeId) {
        if (!isRunning) return new ArrayList<>();
        Set<String> connectedEdges = new HashSet<>();

        try {
            String baseEdgeId = edgeId;
            if (edgeId.contains("_") && !edgeId.startsWith(":")) {
                int lastUnderscore = edgeId.lastIndexOf('_');
                String suffix = edgeId.substring(lastUnderscore + 1);
                if (suffix.matches("\\d+")) {
                    baseEdgeId = edgeId.substring(0, lastUnderscore);
                }
            }

            int laneCount = edgeWrapper.getLaneNumber(baseEdgeId);

            for (int i = 0; i < laneCount; i++) {
                String laneId = baseEdgeId + "_" + i;

                try {
                    SumoLinkList links = (SumoLinkList) connection.do_job_get(Lane.getLinks(laneId));

                    for (SumoLink link : links) {
                        String nextLane = link.notInternalLane;
                        int lastUnderscore = nextLane.lastIndexOf('_');
                        if (lastUnderscore != -1) {
                            String nextEdge =  nextLane.substring(0, lastUnderscore);
                            connectedEdges.add(nextEdge);
                        }
                    }
                }
                catch (Exception e) {
                    // ignore, keep going on the next lanes.
                }
            }
        }
        catch (Exception e) {
            LOG.error("Failed to get valid edges for " + edgeId + ": " + e.getMessage());
        }
        return new ArrayList<>(connectedEdges);
    }

    public int getEdgeCount()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0;
        }
        try
        {
            return edgeWrapper.getEdgeCount();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get number of edges");
            
        }
        return 0;
    }

    public List<String> getEdgeIDs()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return edgeWrapper.getEdgeIDs();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get edge IDs");
            
        }
        return new ArrayList<>();
    }

    public int getLaneNumber(String edgeId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0;
        }
        try
        {
            return edgeWrapper.getLaneNumber(edgeId);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get lane number");
            
        }
        return 0;
    }

    public int setMaxSpeed(String edgeID, double speed) {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0;
        }
        try
        {
            return edgeWrapper.setMaxSpeed(edgeID, speed);
        }
        catch (Exception e)
        {
            LOG.error("Failed to set max speed");
            
        }
        return 0;
    }

    public double getMeanSpeed(String edgeID) {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            return edgeWrapper.getMeanSpeed(edgeID);
        }
        catch (Exception e)
        {
            LOG.error("Failed to get mean speed");
            
        }
        return 0.0;
    }

    public int setGlobalMaxSpeed(double speed) {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0;
        }
        return edgeWrapper.setGlobalMaxSpeed(speed);
    }

    // --------------------------------------------
    // STATISTICS & GLOBAL
    // --------------------------------------------

    // function getCurrentTime()
    public double getCurrentTime()
    {
        // check if running
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            // use the SUMO command
            return (Double) connection.do_job_get(Simulation.getTime());
        }
        catch (Exception e)
        {
            LOG.error("Failed to get current time");
            
        }
        return 0.0;
    }

    public double getGlobalMeanSpeed() {
        if (!isRunning) {
            LOG.error("The simulation is not running");
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
            LOG.error("Failed to calculate global mean speed");
            
        }
        return 0.0;
    }

    // Get the CO2 of every vehicle on the map
    public double getTotalCO2() {
        if (!isRunning) return 0.0;

        double totalCO2 = 0.0;
        try {
            List<String> ids = getVehicleIDs(); // Get all the vehicle IDs
            for (String id : ids) // Loop through and get all the CO2 emission by every vehicle
            {
                totalCO2 += vehicleWrapper.getCO2Emission(id);
            }
        } catch (Exception e) {
            System.out.println("Error calculating CO2");
        }
        return totalCO2;
    }

    public double getCongestionPercentage() {
        if (!isRunning) return 0.0;

        try {
            List<String> ids = getVehicleIDs();
            if (ids.isEmpty()) return 0.0;

            int stoppedCars = 0;
            for (String id : ids) {
                // If speed is less than 0.1 m/s, the car can be viewed as stopped
                if (vehicleWrapper.getVehicleSpeed(id) < 0.1) {
                    stoppedCars++;
                }
            }
            // Return percentage (0.0 to 100.0)
            return ((double) stoppedCars / ids.size()) * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ------------------------------------------
    // SIMULATION
    // -------------------------------------------

    // get the map boundary's coordinates
    public List<SumoPosition2D> getNetBoundary()
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return new ArrayList<>();
        }
        try
        {
            return simulationWrapper.getNetBoundary();
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the map boundary");
        }
        return new ArrayList<>();
    }

}