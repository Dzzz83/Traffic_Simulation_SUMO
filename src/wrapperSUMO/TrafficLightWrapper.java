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

import de.tudresden.sumo.cmd.Trafficlight;
import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.ArrayList;

public class TrafficLightWrapper
{
    // initialize the connection
    private final SumoTraciConnection connection;

    // Constructor
    public TrafficLightWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }

    // get traffic light's IDs
    public List<String> getTrafficLightIDs()
    {
        try
        {
            return (List<String>) connection.do_job_get(Trafficlight.getIDList());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get ID of Traffic Lights");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // get the number of traffic lights
    public int getTrafficLightCount()
    {
        try
        {
            return (int) connection.do_job_get(Trafficlight.getIDCount());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the number of Traffic Lights");
            e.printStackTrace();
        }
        return 0;
    }

    // get the traffic light's state
    public String getRedYellowGreenState(String trafficLightId)
    {
        try
        {
            return (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(trafficLightId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the state of Traffic Lights" + trafficLightId);
            e.printStackTrace();
        }
        return "";
    }

}