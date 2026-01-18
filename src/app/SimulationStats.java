package app;

/**
 * An immutable data structure representing the state of the simulation at a specific time step.
 * <p>
 * This class acts as a Data Transfer Object (DTO) to store snapshots of simulation metrics
 * (Speed, CO2, Density) for later use in reporting and charting.
 * </p>
 */
public class SimulationStats {
    public final double time;
    public final double speed;
    public final double co2;
    public final double density;

    /**
     * Constructs a new simulation snapshot.
     *
     * @param time    The current time in seconds.
     * @param speed   The average speed (km/h).
     * @param co2     The CO2 emission (g/s). Note: Input should already be converted to grams.
     * @param density The congestion density percentage.
     */
    public SimulationStats(double time, double speed, double co2, double density) {
        this.time = time;
        this.speed = speed;
        this.co2 = co2;
        this.density = density;
    }
}