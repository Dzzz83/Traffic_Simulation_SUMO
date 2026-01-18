package app;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExportTask implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ExportTask.class);

    // Define the data
    private final ReportManager reportManager;
    private final List<SimulationStats> dataToSave;
    private final String filename;

    // Constructor
    public ExportTask(ReportManager reportManager, List<SimulationStats> dataToSave, String filename) {
        this.reportManager = reportManager;
        this.dataToSave = dataToSave;
        this.filename = filename;
    }

    @Override
    public void run() {
        LOG.info("Starting background export task...");
        try {
            reportManager.saveToCSV(filename, dataToSave);
        } catch (TrafficSimulationException e) {
            LOG.error("Export Task Failed", e);
            LOG.error("Error saving file: " + e.getMessage());
        }
    }
}