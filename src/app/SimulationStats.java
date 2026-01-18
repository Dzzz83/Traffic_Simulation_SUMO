package app;

// The object of this class is here to hold the data for each second of the simulation
public class SimulationStats {
    public final double time;
    public final double speed;
    public final double co2;
    public final double density;

    public SimulationStats(double time, double speed, double co2, double density) {
        this.time = time;
        this.speed = speed;
        this.co2 = co2;
        this.density = density;
    }
}