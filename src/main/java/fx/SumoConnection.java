package main.java.fx;
import org.eclipse.sumo.libsumo.Simulation;
import org.eclipse.sumo.libsumo.StringVector;

public class SumoConnection {
    public static void main(String[] args) {
        Simulation.preloadLibraries();
        Simulation.start(new StringVector(new String[] {"sumo", "-c", "C:\\Users\\XUAN NGAN\\IdeaProjects\\Traffic_Simulation_SUMO\\src\\main\\resources\\sumo\\demo.sumocfg"}));
        for (int i = 0; i < 5; i++) {
            Simulation.step();
        }
        Simulation.close();
    }
}