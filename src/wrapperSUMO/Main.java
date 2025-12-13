package wrapperSUMO;
import de.tudresden.sumo.cmd.Trafficlight;

import java.util.List;

public class Main
{
    public static void main(String[] args) {
        // create the Control Panel instance
        ControlPanel panel = new ControlPanel();

        // start the simulation
        System.out.println("Starting SUMO Connection Demo");
        panel.startSimulation();

        // check isRunning
        boolean isRunning = panel.isRunning();
        System.out.println("Simulation running: " + isRunning);

        // list traffic lights
        int trafficLightCount = panel.getTrafficLightCount();
        System.out.println("Number of traffic lights: " + trafficLightCount);

        List<String> trafficLightIDs = panel.getTrafficLightIDs();
        System.out.println("Traffic light IDs: " + trafficLightIDs);

        // Edges methods
        int edgeCount = panel.getEdgeCount();
        System.out.println("Number of edges: " + edgeCount);

        List<String> getEdgeIDs = panel.getEdgeIDs();
        System.out.println("Edge IDs: " + getEdgeIDs);

        // step 3 times and show traffic light states
        for (int i = 0; i < 500000000; i++)
        {
            System.out.println("\n--- Step " + (i+1) + " ---");
            panel.step();

            // show traffic light states after each step
            System.out.println("Traffic light states:");
            for (String tlId : trafficLightIDs) {
                String state = panel.getRedYellowGreenState(tlId);
                System.out.println("  " + tlId + ": " + state);
            }
            double globalMeanSpeed = panel.getGlobalMeanSpeed();
            System.out.println("Average speed: " + globalMeanSpeed);
        }

        // stop the simulation
        panel.stopSimulation();
        System.out.println("Simulation stopped: " + !panel.isRunning());
    }
}