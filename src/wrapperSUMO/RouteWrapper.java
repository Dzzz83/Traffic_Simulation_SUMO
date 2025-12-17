package wrapperSUMO;

import de.tudresden.sumo.cmd.Route;
import it.polito.appeal.traci.SumoTraciConnection;

import java.util.ArrayList;
import java.util.List;

public class RouteWrapper {
    private SumoTraciConnection conn;

    public RouteWrapper(SumoTraciConnection conn)
    {
        this.conn = conn;
    }
    // get the list of all route ids
    public List<String> getRouteIDs()
    {
        try
        {
            return (List<String>) conn.do_job_get(Route.getIDList());
        }
        catch (Exception e)
        {
            System.out.println("Failed to get the list of all routes");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
