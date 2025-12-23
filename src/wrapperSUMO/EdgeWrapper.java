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

import de.tudresden.sumo.cmd.Edge;
import it.polito.appeal.traci.SumoTraciConnection;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Tai
 */
public class EdgeWrapper {
    // Initialize the connection
    private final SumoTraciConnection connection;

    // Constructor
    public EdgeWrapper(SumoTraciConnection connection) {
        this.connection = connection;
    }

    // Get the amount of edges in the map without counting the internal edges
    public int getEdgeCount() {
        try {
            // Only count the visible edges
            List<String> visibleEdges = getEdgeIDs();
            return visibleEdges.size();
        } catch (Exception e) {
            System.out.println("Failed to get number of edges");
            
        }
        return 0;
    }

    // Get edge ids excluding the internal edges
    public List<String> getEdgeIDs() {
        try {
            // Because the get EdgeIDs also include internal edges, we have to filter them out
            List<String> allIds = (List<String>) connection.do_job_get(Edge.getIDList());
            List<String> filterIds = new ArrayList<>();
            for (String id : allIds) {
                if (!id.startsWith(":")) {
                    filterIds.add(id);
                }
            }
            return filterIds;
        } catch (Exception e) {
            System.out.println("Failed to get the list of edge IDs");
            
        }
        return new ArrayList<>();
    }

    // Get the number of lanes
    public int getLaneNumber(String edgeId) {
        try {
            return (Integer) connection.do_job_get(Edge.getLaneNumber(edgeId));
        } catch (Exception e) {
            System.out.println("Failed to get the number of lanes");
            
        }
        return 0;
    }

    // Basically setting the speed limit for a specific edge
    public int setMaxSpeed(String edgeID, double speed) {
        if (edgeID == null || edgeID.isEmpty()) {
            System.err.println("Error: edgeID cannot be empty.");
            return -1;
        }
        if (speed < 0) // Speed cannot be negative
        {
            System.err.println("Error: speed cannot be negative.");
            return -2;
        }
        try {
            connection.do_job_set(Edge.setMaxSpeed(edgeID, speed));
            return 0;
        } catch (Exception e) {
            System.out.println("Failed to set max speed");
            
            return 1;
        }
    }

    // Get the mean speed of a specific edge
    public double getMeanSpeed(String edgeID) {
        try {
            return (Double) connection.do_job_get(Edge.getLastStepMeanSpeed(edgeID));
        } catch (Exception e) {
            System.out.println("Failed to get mean speed");
            
            return -1.0;
        }
    }

    // Similar to setMaxSpeed but for every edge on the map, so like a speed limit for the whole map
    public int setGlobalMaxSpeed(double speed) {
        if (speed < 0) // Check if the speed is negative
        {
            System.err.println("Error: Speed cannot be negative");
            return -1;
        }
        try
        {
            List<String> allEdges = getEdgeIDs(); // Get the list of all the edges' ID
            for (String edgeID : allEdges)  // Loop through all the edges
            {
                setMaxSpeed(edgeID, speed); // Set the speed limit for every single edge
            }
            return 0;
        } catch (Exception e) {
            System.out.println("Failed to set the global max speed");
            
            return -1;
        }
    }
}