package app;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfReportManager {

    /**
     * Manages the generation of PDF summary reports for the traffic simulation.
     */
    private static final Logger LOG = LogManager.getLogger(PdfReportManager.class);

    /**
     * Generates a complete PDF report at the specified destination.
     * @param destination The file path where the PDF will be saved.
     * @param history     The list of historical simulation stats to aggregate.
     * @param lineChart   The JavaFX LineChart component for speed analysis.
     * @param barChart    The JavaFX BarChart component for waiting time distribution.
     */
    public void generatePdf(String destination, List<SimulationStats> history, Chart lineChart, Chart barChart) {
        try {
            PdfWriter writer = new PdfWriter(destination);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            addReportHeader(document);
            addSimulationMetrics(document, history);
            document.add(new AreaBreak());
            addVisualAnalysis(document, lineChart, barChart);

            document.close();
            LOG.info("PDF saved successfully to: " + destination);

        } catch (FileNotFoundException e) {
            LOG.error("File error: Could not write to path " + destination, e);
        } catch (IOException e) {
            LOG.error("IO Error during PDF generation", e);
        }
    }

    // Helper methods
    /**
     * Adds the report title and the current generation timestamp to the document.
     */
    private void addReportHeader(Document document) {
        Paragraph title = new Paragraph("Traffic Simulation Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(20);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Paragraph subTitle = new Paragraph("Generated on: " + timestamp)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10);

        document.add(title);
        document.add(subTitle);
        document.add(new Paragraph("\n"));
    }

    /**
     * Calculates aggregate metrics from the history and adds them as a text block.
     *
     * @param document The target PDF document.
     * @param history  The raw simulation data.
     */
    private void addSimulationMetrics(Document document, List<SimulationStats> history) {
        document.add(new Paragraph("Simulation Metrics:").setBold().setFontSize(14));

        if (history.isEmpty()) {
            document.add(new Paragraph("No simulation data available."));
            return;
        }

        SimulationMetrics metrics = calculateMetrics(history);

        document.add(new Paragraph("- Total Duration: " + String.format("%.1f", metrics.duration) + " seconds"));
        document.add(new Paragraph("- Average Speed: " + String.format("%.2f", metrics.avgSpeed) + " km/h"));
        document.add(new Paragraph("- Max Speed Reached: " + String.format("%.2f", metrics.maxSpeed) + " km/h"));
        document.add(new Paragraph("- Total CO2 Emitted: " + String.format("%.2f", metrics.totalCo2) + " g"));
        document.add(new Paragraph("\n"));
    }

    /**
     * Adds the visual chart components to the document.
     */
    private void addVisualAnalysis(Document document, Chart lineChart, Chart barChart) {
        addChartToDocument(document, "Real-Time Speed Analysis", lineChart);
        addChartToDocument(document, "Congestion & Waiting Time Distribution", barChart);
    }

    /**
     * Helper to render a specific chart with a title into the PDF.
     */
    private void addChartToDocument(Document document, String title, Chart chart) {
        document.add(new Paragraph(title).setBold());

        Image chartImage = convertChartToPdfImage(chart);
        if (chartImage != null) {
            document.add(chartImage);
        } else {
            document.add(new Paragraph("[Chart could not be rendered]"));
        }
    }

    /**
     * Aggregates raw history data into a single summary object.
     * @param history The list of per-step stats.
     */
    private SimulationMetrics calculateMetrics(List<SimulationStats> history) {
        double speedSum = 0;
        double totalCo2Mg = 0;
        double maxSpeed = 0;

        for (SimulationStats record : history) {
            speedSum += record.speed;
            totalCo2Mg += (record.co2 * 0.1);
            if (record.speed > maxSpeed) {
                maxSpeed = record.speed;
            }
        }

        double avgSpeed = speedSum / history.size();
        double duration = history.size() * 0.1;
        double totalCo2 = totalCo2Mg / 1000.0;

        return new SimulationMetrics(duration, avgSpeed, maxSpeed, totalCo2);
    }

    /**
     * Converts a JavaFX Chart node into an iText-compatible Image.
     * @param chart The JavaFX chart to capture.
     */
    private Image convertChartToPdfImage(Chart chart) {
        try {
            WritableImage fxImage = chart.snapshot(new SnapshotParameters(), null);
            java.awt.image.BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(awtImage, "png", stream);

            ImageData imageData = ImageDataFactory.create(stream.toByteArray());
            Image pdfImage = new Image(imageData);
            pdfImage.setWidth(UnitValue.createPercentValue(100));

            return pdfImage;

        } catch (IOException e) {
            LOG.error("Failed to convert chart to image", e);
            return null;
        }
    }

    /**
     * Data Transfer Object (DTO) for holding aggregated simulation statistics.
     * Used to pass calculated metrics between the calculation logic and the PDF writing logic.
     */
    private static class SimulationMetrics {
        final double duration;
        final double avgSpeed;
        final double maxSpeed;
        final double totalCo2;

        SimulationMetrics(double duration, double avgSpeed, double maxSpeed, double totalCo2) {
            this.duration = duration;
            this.avgSpeed = avgSpeed;
            this.maxSpeed = maxSpeed;
            this.totalCo2 = totalCo2;
        }
    }
}