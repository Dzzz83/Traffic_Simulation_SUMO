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

import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.objects.SumoLink;
import de.tudresden.sumo.objects.SumoTLSController;
import it.polito.appeal.traci.SumoTraciConnection;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class TrafficLightWrapper
{
    private class XmlConnectionData {
        String dir;
        int linkIndex;

        XmlConnectionData(String dir, int linkIndex) {
            this.dir = dir;
            this.linkIndex = linkIndex;
        }
    }
    private static final Logger logger = LogManager.getLogger(TrafficLightWrapper.class);
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

    // get traffic light's IDs
    public List<String> getTrafficLightIDs()
    {
        logger.debug("Attempting to retrieve all traffic light IDs.");
        try
        {
            List<String> ids = (List<String>) connection.do_job_get(Trafficlight.getIDList());
            logger.debug("Retrieved {} traffic lights.", ids != null ? ids.size() : 0);
            return ids;
        }
        catch (Exception e)
        {
            // Log the error, the context, and the stack trace (e)
            logger.error("Failed to get ID list of Traffic Lights. Returning empty list.", e);
        }
        return new ArrayList<>();
    }

    // get the number of traffic lights
    public int getTrafficLightCount()
    {
        try
        {
            int count = (int) connection.do_job_get(Trafficlight.getIDCount());
            logger.info("Total Traffic Light count retrieved: {}", count);
            return count;
        }
        catch (Exception e)
        {
            logger.error("Failed to get the number of Traffic Lights. Returning 0.", e);
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
            logger.error("Failed to get state for Traffic Light ID: {}. Returning empty string.", trafficLightId, e);
        }
        return "";
    }
    // set automatic mode
    public void setautomaticmode(String trafficLightId)
    {
        logger.info("Setting automatic mode (program '0') for Traffic Light: {}", trafficLightId);
        try
        {
            connection.do_job_set(Trafficlight.setProgram(trafficLightId, "0"));
        }
        catch (Exception e)
        {
            logger.error("Failed to set automatic mode for Traffic Light ID: {}.", trafficLightId, e);
        }
    }

    public Map<String, List<TrafficConnectInfo>> get_traffic_connections(String trafficLightId) {
        if (!isRunning)
        {
            logger.warn("Attempted to get traffic connections for {} but simulation is not running.", trafficLightId);
            return new HashMap<>();
        }
        try {
            // 1. Cast the result directly to the expected List<SumoLink>
            @SuppressWarnings("unchecked")
            List<SumoLink> links = (List<SumoLink>) connection.do_job_get(Trafficlight.getControlledLinks(trafficLightId));

            Map<String, List<TrafficConnectInfo>> connection_by_edge = new HashMap<>();

            // 2. The standard for-each loop will now work on the List<SumoLink>
            for (SumoLink link : links) {
                String fromlane = link.from;
                String tolane = link.to;
                String fromedge = fromlane.split("_")[0];
                String toedge = tolane.split("_")[0];

                // Retrieve link index/ direction from xml file
                XmlConnectionData data = getConnectionData(fromlane, tolane);

                String direction = (data != null) ? data.dir : "s";
                // Use the accurate linkIndex from the XML
                int accurateLinkIndex = (data != null) ? data.linkIndex : 0;

                TrafficConnectInfo info = new TrafficConnectInfo(fromedge, toedge, direction, accurateLinkIndex, fromlane, tolane);

                connection_by_edge
                        .computeIfAbsent(fromedge, k -> new ArrayList<>())
                        .add(info);
            }
            return connection_by_edge;
        } catch (Exception e) {
            logger.error("Failed to get the traffic connections " + trafficLightId);
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private XmlConnectionData getConnectionData(String fromLane, String toLane) {
        String fromEdge = fromLane.split("_")[0];
        String toEdge = toLane.split("_")[0];
        String fromLaneNum = fromLane.split("_")[1];
        String toLaneNum = toLane.split("_")[1];

        String key = fromEdge + "_" + fromLaneNum + "->" + toEdge + "_" + toLaneNum;
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
            logger.error("Loaded " + staticConnectionData.size() + " connections from XML.");

        } catch (Exception e) {
            logger.error("Failed to load connection directions");
            e.printStackTrace();
        }
    }
    public double getRemainingTimeForConnection(String trafficLightId) {
        // Note: Connections usually switch together, so we query the TLS ID
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
    public double getRemainingTimeForConnection(String trafficLightId, int linkIndex) {
        double nextSwitch = getNextSwitchTime(trafficLightId);
        double currentTime = getCurrentTime();
        return Math.max(0, nextSwitch - currentTime);
    }
    public double getNextSwitchTime(String tlsID) {
        if (!isRunning) return -1.0;
        try {
            return (double) connection.do_job_get(Trafficlight.getNextSwitch(tlsID));
        } catch (Exception e) {
            logger.error("Failed to get next switch time for " + tlsID);
            e.printStackTrace();
        }
        return -1.0;
    }
    public double getCurrentTime()
    {
        // check if running
        if (!isRunning)
        {
            logger.error("The simulation is not running");
            return 0.0;
        }
        try
        {
            // use the SUMO command
            return (Double) connection.do_job_get(Simulation.getTime());
        }
        catch (Exception e)
        {
            logger.error("Failed to get current time");
            e.printStackTrace();
        }
        return 0.0;
    }
}


