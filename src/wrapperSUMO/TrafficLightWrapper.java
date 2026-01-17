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

import de.tudresden.sumo.cmd.Junction;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.objects.SumoLink;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoTLSController;
import it.polito.appeal.traci.SumoTraciConnection;

import java.io.File;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TrafficLightWrapper
{
    private static final Logger LOG = LogManager.getLogger(TrafficLightWrapper.class.getName());
    private class XmlConnectionData {
        String dir;
        int linkIndex;

        XmlConnectionData(String dir, int linkIndex) {
            this.dir = dir;
            this.linkIndex = linkIndex;
        }
    }
    // initialize the connection
    private final SumoTraciConnection connection;
    // Map key is "fromEdge_Lane->toEdge_Lane"
    private Map<String, XmlConnectionData> staticConnectionData = new HashMap<>();
    public boolean isRunning = false;
    // Constructor
    public TrafficLightWrapper(SumoTraciConnection connection)
    {
        this.connection = connection;
    }
    // store phase of each traffic light id
    private Map<String, Integer> phaseTracker = new HashMap<>();
    // get traffic light's IDs
    public List<String> getTrafficLightIDs()
    {
        try
        {
            return (List<String>) connection.do_job_get(Trafficlight.getIDList());
        }
        catch (Exception e)
        {
            LOG.error("Failed to get ID of Traffic Lights");
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
            LOG.error("Failed to get the number of Traffic Lights");
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
            LOG.error("Failed to get the state of Traffic Lights" + trafficLightId);
            e.printStackTrace();
        }
        return "";
    }
    // set automatic mode
    public void setautomaticmode(String trafficLightId)
    {
        try
        {
            connection.do_job_set(Trafficlight.setProgram(trafficLightId, "0"));
        }
        catch (Exception e)
        {
            LOG.error("Failed to set the program of Traffic Lights" + trafficLightId);
        }
    }

    public Map<String, List<TrafficConnectInfo>> get_traffic_connections(String trafficLightId) {
        if (!isRunning) return new HashMap<>();
        try {
            // 1. Cast the result directly to the expected List<SumoLink>
            @SuppressWarnings("unchecked")
            List<SumoLink> links = (List<SumoLink>) connection.do_job_get(Trafficlight.getControlledLinks(trafficLightId));

            Map<String, List<TrafficConnectInfo>> connection_by_edge = new HashMap<>();

            for (SumoLink link : links) {
                String fromlane = link.from;
                String tolane = link.to;
                String fromedge = fromlane.split("_")[0];
                String toedge = tolane.split("_")[0];

                // Retrieve link index/ direction from xml file
                XmlConnectionData data = getConnectionData(fromlane, tolane);

                String direction = (data != null) ? data.dir : "s";
                // Use the accurate linkIndex from the XML
                int link_index = (data != null) ? data.linkIndex : 0;

                TrafficConnectInfo info = new TrafficConnectInfo(fromedge, toedge, direction, link_index, fromlane, tolane);

                connection_by_edge
                        .computeIfAbsent(fromedge, k -> new ArrayList<>())
                        .add(info);
            }
            return connection_by_edge;
        } catch (Exception e) {
            LOG.error("Failed to get the traffic connections " + trafficLightId);
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private XmlConnectionData getConnectionData(String fromLane, String toLane) {
        String key = fromLane + "->" + toLane;
        return staticConnectionData.get(key);
    }
    public void loadConnectionDirections(String netFilePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(netFilePath));

            NodeList connections = doc.getElementsByTagName("connection");

            for (int i = 0; i < connections.getLength(); i++) {
                Element conn = (Element) connections.item(i);

                // We only care about connections controlled by a Traffic Light
                if (!conn.hasAttribute("tl")) continue;

                String from = conn.getAttribute("from");
                String to = conn.getAttribute("to");
                String fromLane = conn.getAttribute("fromLane");
                String toLane = conn.getAttribute("toLane");
                String dir = conn.getAttribute("dir");
                String linkIdxStr = conn.getAttribute("linkIndex");

                if (linkIdxStr != null && !linkIdxStr.isEmpty()) {
                    int linkIdx = Integer.parseInt(linkIdxStr);

                    String key = from + "_" + fromLane + "->" + to + "_" + toLane;
                    // Store both Direction and Index
                    staticConnectionData.put(key, new XmlConnectionData(dir, linkIdx));
                }
            }
            LOG.info("Loaded " + staticConnectionData.size() + " connections from XML.");

        } catch (Exception e) {
            LOG.error("Failed to load connection directions");
            e.printStackTrace();
        }
    }
    // get traffic light pos
    public SumoPosition2D getTrafficLightPosition(String trafficLightId) {
        try {
            return (SumoPosition2D) connection.do_job_get(Junction.getPosition(trafficLightId));
        } catch (Exception e) {
            LOG.error("Could not get position for TL: " + trafficLightId);
            return null;
        }
    }
    public double getRemainingTimeForConnection(String trafficLightId)
    {
        double nextSwitch = getNextSwitchTime(trafficLightId);
        double currentTime = getCurrentTime();
        if (nextSwitch == -1) return 0;
        return Math.max(0, nextSwitch - currentTime);
    }

    public char getStateForConnection(String trafficLightId, int linkIndex) {
        String fullState = getRedYellowGreenState(trafficLightId);

        if (fullState == null || linkIndex >= fullState.length()) {
            return 'r'; // default to red if error
        }

        return fullState.charAt(linkIndex);
    }

    public double getNextSwitchTime(String tlsID) {
        if (!isRunning) return -1.0;
        try {
            return (double) connection.do_job_get(Trafficlight.getNextSwitch(tlsID));
        } catch (Exception e) {
            LOG.error("Failed to get next switch time for " + tlsID);
            e.printStackTrace();
        }
        return -1.0;
    }
    public double getCurrentTime()
    {
        // check if running
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            // use the SUMO command
            return (Double) connection.do_job_get(Simulation.getTime());
        }
        catch (Exception e)
        {
            LOG.error("Failed to get current time");
            e.printStackTrace();
        }
        return 0.0;
    }

    // get the current phase
    public int getCurrentPhaseIndex(String trafficLightId) {
        try {
            return (int) connection.do_job_get(Trafficlight.getPhase(trafficLightId));
        } catch (Exception e) {
            return -1;
        }
    }
    // query the connection belong to the "green state" from a trafficId
    public List<Integer> query_connection(String trafficLightId)
    {
        if (!isRunning)
        {
            LOG.error("The simulation is not running");
            return Collections.emptyList();
        }
        try
        {
            String state = this.getRedYellowGreenState(trafficLightId);
            if (state == null || state.isEmpty())
            {
                LOG.error("Traffic light state is empty for " + trafficLightId);
                return Collections.emptyList();
            }
            List<Integer> green_index = new ArrayList<>();
            int index = 0;
            for (char color: state.toCharArray())
            {
                if (color =='g' || color == 'G')
                {
                    green_index.add(index);
                }
                index++;
            }
            return green_index;
        }
        catch (Exception e) {
            LOG.error("Failed to query connection for " + trafficLightId);
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public Map<String, List<String>> get_green_connection_edge(String trafficLightId) {
        if (!isRunning) return new HashMap<>();

        Map<String, List<String>> green_connection_edge = new HashMap<>();

        // 1. Get Indices
        List<Integer> green_index = this.query_connection(trafficLightId);
        if (green_index == null || green_index.isEmpty()) {
            return green_connection_edge;
        }

        try {
            // Fetch Topology as a Generic List first
            Object response = connection.do_job_get(Trafficlight.getControlledLinks(trafficLightId));

            if (response == null || !(response instanceof List)) {
                return green_connection_edge;
            }

            List<?> allLinks = (List<?>) response;

            // 3. Iterate through the Green Indices
            for (int index : green_index) {
                // Safety check for bounds
                if (index >= 0 && index < allLinks.size()) {

                    // Get the object at this index
                    Object item = allLinks.get(index);

                    if (item == null) continue;

                    // Create a temporary list to unify logic
                    List<SumoLink> linksToProcess = new ArrayList<>();

                    // if a List (Standard behavior)
                    if (item instanceof List) {
                        linksToProcess.addAll((List<SumoLink>) item);
                    }
                    // if it is single SumoLink
                    else if (item instanceof SumoLink) {
                        linksToProcess.add((SumoLink) item);
                    }

                    // 4. Process the links
                    for (SumoLink link : linksToProcess) {
                        String lane_id = link.from;
                        if (lane_id != null && !lane_id.isEmpty()) {

                            // Robust Edge ID extraction
                            String edge_id = lane_id;
                            int lastUnderscore = lane_id.lastIndexOf('_');
                            if (lastUnderscore != -1) {
                                edge_id = lane_id.substring(0, lastUnderscore);
                            }

                            green_connection_edge
                                    .computeIfAbsent(edge_id, k -> new ArrayList<>())
                                    .add(lane_id);
                        }
                    }
                }
            }
            return green_connection_edge;
        } catch (Exception e) {
            LOG.error("Error in get_green_connection_edge for " + trafficLightId);
            e.printStackTrace();
        }
        return green_connection_edge;
    }
    // get the largest number of vehicles among edges
    public int get_largest_no_vehicles(String trafficLightId)
    {
        if (!isRunning) return 0;

        Map<String, List<String>> green_connection_edge = get_green_connection_edge(trafficLightId);

        if (green_connection_edge.isEmpty()) {
            return 0;
        }

        int max_vehicles = 0;

        for (Map.Entry<String, List<String>> entry : green_connection_edge.entrySet())
        {
            List<String> laneid = entry.getValue();
            int current_edge_nov = 0;
            try {
                for (String lane: laneid)
                {
                    // Ensure 'lane' is valid
                    if (lane != null) {
                        int count = (int) connection.do_job_get(Lane.getLastStepVehicleNumber(lane));
                        current_edge_nov += count;
                    }
                }
            }
            catch (Exception e)
            {
                // CHANGED ERROR MESSAGE to identify this specific method
                LOG.error("Error in get_largest_no_vehicles counting lane: " + trafficLightId);
                e.printStackTrace(); // PRINT THE REASON
            }

            if (current_edge_nov > max_vehicles) {
                max_vehicles = current_edge_nov;
            }
        }
        return max_vehicles;
    }

    // set phase duration based on traffic level
    public void update_phase_based_traffic_level(String trafficLightId)
    {
        int max_vehicles = this.get_largest_no_vehicles(trafficLightId);
        double min_dur = 20.0;
        double max_dur = 60.0;
        double timepercar  = 2.5;
        double calculated_duration = min_dur + (max_vehicles * max_dur);
        double new_duration = Math.min(max_dur, calculated_duration);
        try
        {
            connection.do_job_set(Trafficlight.setPhaseDuration(trafficLightId, new_duration));
            LOG.info(String.format(">>> ADAPTIVE %s: %d vehicles. Scaled Duration: %.1fs",
                    trafficLightId, max_vehicles, new_duration));
        }
        catch (Exception e)
        {
            LOG.error("Failed to update phase duration for " + trafficLightId);
        }
    }
    // get all of the traffic light with its phase
    public void checkAndOptimize(String trafficLightId) {
        if (!isRunning || trafficLightId == null) return;

        try {
            // Get current phase from SUMO
            int currentPhase = this.getCurrentPhaseIndex(trafficLightId);

            // Get the last known phase from our Memory Map (default to -1 if not found)
            int lastPhase = phaseTracker.getOrDefault(trafficLightId, -1);

            // Check if phase has changed
            if (currentPhase != lastPhase) {

                // Only optimize if the phase actually involves Green (usually checking state string)
                // (Optional: You can reuse your existing logic for checking 'g' or 'G' state here)
                String state = this.getRedYellowGreenState(trafficLightId);

                if (state != null && (state.contains("G") || state.contains("g"))) {
                    // LOGIC: The phase just changed to GREEN -> RUN OPTIMIZATION
                    this.update_phase_based_traffic_level(trafficLightId);
                }

                // Update the Memory Map with the new phase
                phaseTracker.put(trafficLightId, currentPhase);
            }
        } catch (Exception e) {
            LOG.error("Error optimizing " + trafficLightId);
        }
    }

    public void setPhaseDuration(String trafficLightId, double duration) {
        try {
            // Direct command to SUMO to override the current phase length
            connection.do_job_set(Trafficlight.setPhaseDuration(trafficLightId, duration));
            LOG.info("Manual Override: Set " + trafficLightId + " phase to " + duration + "s");
        } catch (Exception e) {
            LOG.error("Error setting duration for " + trafficLightId, e);
        }
    }
}

