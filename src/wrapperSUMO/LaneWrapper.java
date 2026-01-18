package wrapperSUMO;

import de.tudresden.sumo.cmd.Lane;
import it.polito.appeal.traci.SumoTraciConnection;

import java.util.List;
import java.util.ArrayList;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoGeometry;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class LaneWrapper
{
    private static final Logger LOG = LogManager.getLogger(ControlPanel.class.getName());
    private final SumoTraciConnection connection;

    public LaneWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }

    // get lane ids
    public List<String> getLaneIDList()
    {
        try
        {
            List<String> laneIDs = (List<String>) connection.do_job_get(Lane.getIDList());
            return laneIDs;
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the ID list of lanes");
        }
        return new ArrayList<>();
    }

    // get the lane shape
    public List<SumoPosition2D> getLaneShape(String laneId)
    {
        try
        {
            SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));

            List<SumoPosition2D> points = new ArrayList<>();
            // loop through each lane and retrieve all the lane shapes from SUMO
            for (SumoPosition2D p : geometry.coords)
            {
                // convert SumoGeometry to a simple Java List
                points.add(p);
            }
            return points;
        }
        catch (Exception e)
        {
            LOG.error("Failed to get the lane shape");
        }
        return new ArrayList<>();
    }

}
