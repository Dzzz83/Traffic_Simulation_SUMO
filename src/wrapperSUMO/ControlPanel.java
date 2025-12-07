package wrapperSUMO;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoPosition2D;

import java.util.List;
import java.util.ArrayList;

public class ControlPanel {

    // create the connection
    private SumoTraciConnection connection;

    // initialize the wrappers
    private VehicleWrapper vehicleWrapper;
    private TrafficLightWrapper trafficLightWrapper;
    private EdgeWrapper edgeWrapper;

    // initialize the boolean value isRunning
    private boolean isRunning = false;

    // default Constructor
    public ControlPanel()
    {
        System.out.println("ControlPanel created - call startSimulation() to begin");
    }

    // create function startSimulation
    public void startSimulation()
    {
        try {
            // initialize the connection with your specific paths
            connection = new SumoTraciConnection("C:\\Program Files (x86)\\Eclipse\\Sumo\\bin\\sumo-gui.exe",
                    "src/wrapperSUMO/sumo/demo.sumocfg");

            // start the simulation
            connection.runServer();

            // initialize ALL wrappers (Merged from File 1 so TL and Edge work)
            vehicleWrapper = new VehicleWrapper(connection);
            trafficLightWrapper = new TrafficLightWrapper(connection);
            edgeWrapper = new EdgeWrapper(connection);

            // set the isRunning to true
            isRunning = true;
        }
        catch (Exception e)
        {
            System.out.println("Failed to run the simulation");
            e.printStackTrace();
        }
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

    // ==========================================
    //       VEHICLE WRAPPER METHODS
    // ==========================================

    public int getVehicleCount()
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0; }
        try {
            return vehicleWrapper.getVehicleCount();
        } catch (Exception e) {
            System.out.println("Failed to get the number of vehicles");
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getVehicleIDs()
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return new ArrayList<>(); }
        try {
            return vehicleWrapper.getVehicleIDs();
        } catch (Exception e) {
            System.out.println("Failed to get lists of vehicle ids");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public double getVehicleSpeed(String vehicleId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0.0; }
        try {
            return vehicleWrapper.getVehicleSpeed(vehicleId);
        } catch (Exception e) {
            System.out.println("Failed to get the speed");
            e.printStackTrace();
        }
        return 0.0;
    }

    // --- Methods from the second file (Advanced Vehicle Control) ---

    // create function getPosition
    public SumoPosition2D getPosition(String vehicleId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return new SumoPosition2D(0.0, 0.0); }
        try {
            return vehicleWrapper.getPosition(vehicleId);
        } catch (Exception e) {
            System.out.println("Failed to get the position of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return new SumoPosition2D(0.0, 0.0);
    }

    // create function getLaneID
    public String getLaneID(String vehicleId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return ""; }
        try {
            return vehicleWrapper.getLaneID(vehicleId);
        } catch (Exception e) {
            System.out.println("Failed to get the laneID of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // create function getRoadID
    public String getRoadID(String vehicleId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return ""; }
        try {
            return vehicleWrapper.getRoadID(vehicleId);
        } catch (Exception e) {
            System.out.println("Failed to get the roadId of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // create function addVehicle
    public void addVehicle(String vehicleId, String typeId, String routeId, int depart, double position, double speed, byte lane)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return; }
        try {
            vehicleWrapper.addVehicle(vehicleId, typeId, routeId, depart, position, speed, lane);
        } catch (Exception e) {
            System.out.println("Failed to add the vehicle " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function setRouteID
    public void setRouteID(String vehicleId, String routeId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return; }
        try {
            vehicleWrapper.setRouteID(vehicleId, routeId);
        } catch (Exception e) {
            System.out.println("Failed to set the route of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function removeVehicle
    public void removeVehicle(String vehicleId, byte reason)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return; }
        try {
            vehicleWrapper.removeVehicle(vehicleId, reason);
        } catch (Exception e) {
            System.out.println("Failed to remove the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function setSpeed
    public void setSpeed(String vehicleId, double speed)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return; }
        try {
            vehicleWrapper.setSpeed(vehicleId, speed);
        } catch (Exception e) {
            System.out.println("Failed to set the speed of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    // create function getDistance
    public double getDistance(String vehicleId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0.0; }
        try {
            return vehicleWrapper.getDistance(vehicleId);
        } catch (Exception e) {
            System.out.println("Failed to get the distance of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }

    // create function getCo2
    public double getCO2Emission(String vehicleId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0.0; }
        try {
            return vehicleWrapper.getCO2Emission(vehicleId);
        } catch (Exception e) {
            System.out.println("Failed to get the CO2 Emission:  " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }

    // ==========================================
    //       TRAFFIC LIGHT WRAPPER METHODS
    // ==========================================

    public List<String> getTrafficLightIDs()
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return new ArrayList<>(); }
        try {
            return trafficLightWrapper.getTrafficLightIDs();
        } catch (Exception e) {
            System.out.println("Failed to get traffic light IDs");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public int getTrafficLightCount()
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0; }
        try {
            return trafficLightWrapper.getTrafficLightCount();
        } catch (Exception e) {
            System.out.println("Failed to get number of traffic lights");
            e.printStackTrace();
        }
        return 0;
    }

    public String getRedYellowGreenState(String trafficLightId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return ""; }
        try {
            return trafficLightWrapper.getRedYellowGreenState(trafficLightId);
        } catch (Exception e) {
            System.out.println("Failed to get traffic light state");
            e.printStackTrace();
        }
        return "";
    }

    // ==========================================
    //          EDGE WRAPPER METHODS
    // ==========================================

    public int getEdgeCount()
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0; }
        try {
            return edgeWrapper.getEdgeCount();
        } catch (Exception e) {
            System.out.println("Failed to get number of edges");
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getEdgeIDs()
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return new ArrayList<>(); }
        try {
            return edgeWrapper.getEdgeIDs();
        } catch (Exception e) {
            System.out.println("Failed to get edge IDs");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public int getLaneNumber(String edgeId)
    {
        if (!isRunning) { System.out.println("The simulation is not running"); return 0; }
        try {
            return edgeWrapper.getLaneNumber(edgeId);
        } catch (Exception e) {
            System.out.println("Failed to get lane number");
            e.printStackTrace();
        }
        return 0;
    }

    // ==========================================
    //          SYSTEM CONTROL METHODS
    // ==========================================

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

    // create function isRunning()
    public boolean isRunning()
    {
        return isRunning;
    }
}