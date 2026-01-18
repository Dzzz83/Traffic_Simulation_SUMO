package wrapperSUMO;

import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoGeometry;
import it.polito.appeal.traci.SumoTraciConnection;


import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import de.tudresden.sumo.objects.SumoPosition2D;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimulationWrapper
{
    private final SumoTraciConnection connection;
    private static final Logger LOG = LogManager.getLogger(ControlPanel.class.getName());


    // Constructor
    public SimulationWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }

    // get the net boundary
    public List<SumoPosition2D> getNetBoundary()
    {
        try
        {
            SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Simulation.getNetBoundary());
            // check and assign the coordinates to the array list
            if (geometry != null && geometry.coords != null)
            {
                return new ArrayList<>(geometry.coords);
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the lane shape");
        }
        return new ArrayList<>();
    }
}
