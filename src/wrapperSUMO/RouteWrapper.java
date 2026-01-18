package wrapperSUMO;

import de.tudresden.sumo.cmd.Route;
import it.polito.appeal.traci.SumoTraciConnection;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RouteWrapper {
    private SumoTraciConnection conn;
    private static final Logger LOG = LogManager.getLogger(ControlPanel.class.getName());

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
            LOG.error("Failed to get the list of all routes");
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
