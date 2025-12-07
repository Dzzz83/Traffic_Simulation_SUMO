/*
SUMO commands follow this protocol:

GET Commands:
ReturnType result = (ReturnType) conn.do_job_get(Command.getSomething());
ReturnType result = (ReturnType) conn.do_job_get(Command.getSomething(parameter));

SET Commands:
conn.do_job_set(Command.setSomething(parameter, newValue));
conn.do_job_set(Command.setSomething(parameter1, parameter2, ...));
 */

import de.tudresden.sumo.cmd.Edge;
import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Tai
 */
public class EdgeWrapper
{
    // initialize the connection
    private final SumoTraciConnection connection;

    // Constructor
    public EdgeWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }

    // Get the number of edges
    public int getEdgeCount()
    {
        try
        {
            return (Integer) connection.do_job_get(Edge.getIDCount());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get number of edges");
            e.printStackTrace();
        }
        return 0;
    }

    // Get edge IDs
    public List<String> getEdgeIDs()
    {
        try
        {
            return (List<String>) connection.do_job_get(Edge.getIDList());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the list of edge IDs");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // Get the lane number
    public int getLaneNumber(String EdgeId)
    {
        try
        {
            return (Integer) connection.do_job_get(Edge.getLaneNumber(EdgeId));
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the lane number");
            e.printStackTrace();
        }
        return 0;
    }

}