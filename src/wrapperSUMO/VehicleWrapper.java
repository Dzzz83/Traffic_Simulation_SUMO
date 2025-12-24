/*
SUMO commands follow this protocol:

GET Commands:
ReturnType result = (ReturnType) conn.do_job_get(Command.getSomething());
ReturnType result = (ReturnType) conn.do_job_get(Command.getSomething(parameter));

SET Commands:
conn.do_job_set(Command.setSomething(parameter, newValue));
conn.do_job_set(Command.setSomething(parameter1, parameter2, ...));
 */

package wrapperSUMO;

import de.tudresden.sumo.cmd.Vehicle;
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.objects.SumoColor;

import java.util.List;
import java.util.ArrayList;
import de.tudresden.sumo.objects.SumoPosition2D;

public class VehicleWrapper
{
    // initialize the connection
    private final SumoTraciConnection connection;

    // Constructor
    public VehicleWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }

    // getVehicleCount wrapper
    public int getVehicleCount()
    {
        try
        {
            return (Integer) connection.do_job_get(Vehicle.getIDCount());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get number of vehicles");
            e.printStackTrace();
        }
        return 0;
    }

    // getVehicleID wrapper
    public List<String> getVehicleIDs()
    {
        try
        {
            return (List<String>) connection.do_job_get(Vehicle.getIDList());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the list of vehicle IDs");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public String getRouteID(String vehicleId) {
        try {
            return (String) connection.do_job_get(Vehicle.getRouteID(vehicleId));
        } catch (Exception e) {
            System.out.println("Failed to get Route ID for vehicle: " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // get the vehicle's speed
    public double getVehicleSpeed(String vehicleId)
    {
        try
        {
            return (double) connection.do_job_get(Vehicle.getSpeed(vehicleId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the speed of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }

    // get the position of the vehicle
    public SumoPosition2D getPosition(String vehicleId)
    {
        try
        {
            return (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the position of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return new SumoPosition2D(0.0, 0.0);
    }

    // get the ID of the lane
    public String getLaneID(String vehicleId)
    {
        try
        {
            return (String) connection.do_job_get(Vehicle.getLaneID(vehicleId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the laneID of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // get the ID of the road
    public String getRoadID(String vehicleId)
    {
        try
        {
            return (String) connection.do_job_get(Vehicle.getRoadID(vehicleId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the roadId of the vehicle " + vehicleId);
            e.printStackTrace();
        }
        return "";
    }

    // add the vehicle
    public void addVehicle(String vehicleId, String typeId, String routeId, int depart, double position, double speed, byte lane)
    {
        try
        {
            connection.do_job_set(Vehicle.add(vehicleId, typeId, routeId, depart, position, speed, lane));
        }
        catch (Exception e)
        {
            System.out.println("Failed to add the vehicle " + vehicleId);
            e.printStackTrace();
        }
    }

    // tells the vehicle to follow a route that's already defined
    public void setRouteID(String vehicleId, String routeId)
    {
        try
        {
            connection.do_job_set(Vehicle.setRouteID(vehicleId, routeId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the route of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }

    /*
        (byte)0 - Teleport (default) - vehicle disappears gracefully
        (byte)1 - Parking - vehicle parks and is removed
        (byte)2 - Arrived - vehicle reached destination
        (byte)3 - Vaporized - immediate removal
     */
    // remove the vehicle
    public void removeVehicle(String vehicleId, byte reason)
    {
        try
        {
            connection.do_job_set(Vehicle.remove(vehicleId, reason));
        }
        catch (Exception e)
        {
            System.out.println("Failed to remove the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }
    // set the speed of the vehicle
    public void setSpeed(String vehicleId, double speed)
    {
        try
        {
            connection.do_job_set(Vehicle.setSpeed(vehicleId, speed));
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the speed of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
    }
    // get the distance traveled by the vehicle
    public double getDistance(String vehicleId)
    {
        try
        {
            return (Double) connection.do_job_get(Vehicle.getDistance(vehicleId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the distance of the vehicle:  " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }
    // get co2 from the vehicle
    public double getCO2Emission(String vehicleId)
    {
        try
        {
            return (Double) connection.do_job_get(Vehicle.getCO2Emission(vehicleId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the CO2 Emission:  " + vehicleId);
            e.printStackTrace();
        }
        return 0.0;
    }

    // changeLane() function
    public void changeLane(String vehicleId, int laneIndex, int laneDuration)
    {
        try
        {
            connection.do_job_set(Vehicle.changeLane(vehicleId, (byte) laneIndex, laneDuration));
        }
        catch (Exception e)
        {
            System.out.println("Failed to change the lane of the vehicle");
            e.printStackTrace();
        }
    }

    // slowDown() function
    public void slowDown(String vehicleId, double newSpeed, int duration)
    {
        try
        {
            connection.do_job_set(Vehicle.slowDown(vehicleId, newSpeed, duration));
        }
        catch (Exception e)
        {
            System.out.println("Failed to slow down the vehicle");
            e.printStackTrace();
        }
    }

    // setColor() function
    public void setColor(String vehicleId, int r, int g, int b, int a)
    {

        SumoColor color = new SumoColor(r, g, b, a);
        try
        {
            connection.do_job_set(Vehicle.setColor(vehicleId, color));
        }
        catch (Exception e)
        {
            System.out.println("Failed to set the color");
            e.printStackTrace();
        }
    }
    // change the desination of the vehicle
    public void changeTarget(String vehicleId, String edgeId)
    {
        try
        {
            connection.do_job_set(Vehicle.changeTarget(vehicleId, edgeId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to change the target");
            e.printStackTrace();
        }
    }
    // get the angle of the vehicle
    public double getAngle(String vehicleID)
    {
        try
        {
            return (Double) connection.do_job_get(Vehicle.getAngle(vehicleID));
        }
        catch (Exception e)
        {
            System.out.println("Can't get the angle of the vehicle");
            e.printStackTrace();
        }
        return 0.0;
    }





}