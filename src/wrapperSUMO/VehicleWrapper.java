package wrapperSUMO;

import de.tudresden.sumo.cmd.Vehicle;
import it.polito.appeal.traci.SumoTraciConnection;

import java.util.List;
import java.util.ArrayList;

public class VehicleWrapper
{
    // initialize the connection
    private final SumoTraciConnection connection;

    // constructor
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
}