package wrapperSUMO;
import java.sql.SQLOutput;
import java.util.List;
import de.tudresden.sumo.objects.SumoPosition2D;
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

        // adding new car
        String new_car = "myPorse";
        // fixed lane to -2
        panel.addVehicle(new_car, "DEFAULT_VEHTYPE", "r_1", 0, 0.0, 10, (byte)-2);
        System.out.println("New vehicle added: " + new_car);


        // step 20 times
        int total_step = 100;
        for (int i = 0; i < total_step; i++)
        {
            System.out.println("\n--- Step " + (i+1) + "/" + total_step);
            panel.step();
//            if (!trafficLightIDs.isEmpty())
//            {
//                // show traffic light states after each step
//                System.out.println("Traffic light states:");
//                for (String tlId : trafficLightIDs) {
//                    String state = panel.getRedYellowGreenState(tlId);
//                    System.out.println("  " + tlId + ": " + state);
//                }
//                System.out.println();
//            }

            if (i == 5)
            {
                panel.setColor(new_car, 0, 0, 255, 255);
                System.out.println("The new car's color has been changed");
                System.out.println("The speed of the " + new_car + " is (before): " + panel.getVehicleSpeed(new_car));
                panel.setSpeed(new_car, 10);
                System.out.println("The lane of the " + new_car + " is (before): " + panel.getLaneID(new_car));
                panel.changeLane(new_car, 1, 3000);
            }
            if (i == 6)
            {
                System.out.println("The speed of the " + new_car + " is (after): " + panel.getVehicleSpeed(new_car));
                System.out.println("The lane of the " + new_car + " is (after): " + panel.getLaneID(new_car));
                panel.slowDown(new_car, 5, 2);

            }
            if (i == 7)
            {
                System.out.println("The speed of the " + new_car + " is (after): " + panel.getVehicleSpeed(new_car));
                System.out.println("Current Road of the new vehicle is: " + panel.getRoadID(new_car));

            }

            // check if the new car exists in the simulation
            if (panel.getVehicleIDs().contains(new_car))
            {
                // get the speed of the car
                double speed = panel.getVehicleSpeed(new_car);
                // get the position of the car
                SumoPosition2D pos = panel.getPosition(new_car);
                System.out.println("The new car's speed is " + speed);
                System.out.println("The position of the new car is " + pos);
            }
            if (i == 50)
            {
                System.out.println("Current Road of the new vehicle is: " + panel.getRoadID(new_car));
                // change the destination of the new car
                panel.changeTarget(new_car, "E4");
                System.out.println("Successfully changed the target");
            }
            if (i == 80)
            {
                System.out.println("Current Road of the new vehicle is: " + panel.getRoadID(new_car));
            }
        }
        // check if the new car exists
        if (panel.getVehicleIDs().contains(new_car))
        {
            // get the lane and the road
            String laneID = panel.getLaneID(new_car);
            String roadID = panel.getRoadID(new_car);
            System.out.println("The new car's lane is " + laneID);
            System.out.println("The new car's positon is " + roadID);

            // change the speed
            double newSpeed = 15;
            panel.setSpeed(new_car, newSpeed);

            // get the CO2 emission
            double co2 = panel.getCO2Emission(new_car);
            System.out.println("The new car's co2 emision is: " + co2);

            // get the distance
            double dis = panel.getDistance(new_car);
            System.out.println("The new car's distance is: " + dis);


            panel.removeVehicle(new_car, (byte)0);
            System.out.println("The new vehicle has been removed");
        }
        else
        {
            System.out.println("The new car is not in the simulation");
        }

        // stop the simulation
        panel.stopSimulation();
        System.out.println("Simulation stopped: " + !panel.isRunning());
    }
}