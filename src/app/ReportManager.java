package app;

import java.io.File;
import java.io.PrintWriter; // To write into the file
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ReportManager  {
    private static final Logger LOG = LogManager.getLogger(ReportManager.class);

    public void saveToCSV(String filePath, List<SimulationStats> dataHistory) throws TrafficSimulationException {
        File file = new File(filePath);

        try (PrintWriter writer = new PrintWriter(file)) // Try-with-Resources
        {
            writer.println("Time(s),AvgSpeed(km/h),CO2(g/s),Density(%)");
            for (SimulationStats stat : dataHistory) {
                writer.printf("%.1f,%.2f,%.2f,%.1f%n",
                        stat.time,
                        stat.speed,
                        stat.co2,
                        stat.density
                );
            }
            LOG.info("Report saved successfully to: " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new TrafficSimulationException("Could not create file: " + e.getMessage());
        }

    }
}