package app;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A background task responsible for exporting simulation data to a file.
 * <p>
 * This class implements Runnable so that file I/O operations
 * can be executed on a separate thread, preventing the main UI from freezing.
 * </p>
 */
public class ExportTask implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ExportTask.class);

    // Define the data
    private final ReportManager reportManager;
    private final List<SimulationStats> dataToSave;
    private final String filename;

    /**
     * Constructs a new export task.
     *
     * @param reportManager The service responsible for the actual file writing logic.
     * @param dataToSave    The snapshot of simulation history to be written.
     * @param filename      The target file path (e.g., "TrafficReport.csv").
     */
    public ExportTask(ReportManager reportManager, List<SimulationStats> dataToSave, String filename) {
        this.reportManager = reportManager;
        this.dataToSave = dataToSave;
        this.filename = filename;
    }

    /**
     * Executes the export process.
     */
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