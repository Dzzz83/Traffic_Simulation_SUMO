package app;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

abstract class VehicleRenderer {
    public void draw(GraphicsContext gc, double x, double y, double angle, double length, double width, Color sumoColor) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angle);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);

        //Draw Body
        drawBody(gc, length, width, sumoColor);

        //Draw Headlights
        drawHeadlights(gc, length, width);
        gc.restore();
    }

    protected abstract void drawBody(GraphicsContext gc, double length, double width, Color color);

    protected void drawHeadlights(GraphicsContext gc, double length, double width) {
        gc.setFill(Color.WHITE);
        double lightSize = width / 4;
        gc.fillOval(-width / 2 + 1, -length / 2, lightSize, lightSize);
        gc.fillOval(width / 2 - 1 - lightSize, -length / 2, lightSize, lightSize);
    }
}

// Class for "DEFAULT_VEHTYPE"
class CarRenderer extends VehicleRenderer {
    @Override
    protected void drawBody(GraphicsContext gc, double length, double width, Color color) {
        gc.setFill(color);

        gc.fillRoundRect(-width / 2, -length / 2, width, length, 3, 3);
        gc.strokeRoundRect(-width / 2, -length / 2, width, length, 3, 3);

        // Windshield
        gc.setFill(Color.color(0, 0, 0, 0.5));
        gc.fillRoundRect(-width / 2 + 1, -length / 2 + 2, width - 2, length / 4, 2, 2);
    }
}

// Class for "Delivery"
class DeliveryRenderer extends VehicleRenderer {
    @Override
    protected void drawBody(GraphicsContext gc, double length, double width, Color color) {
        gc.setFill(color);

        // Boxy Truck Shape
        gc.fillRect(-width / 2, -length / 2, width, length);
        gc.strokeRect(-width / 2, -length / 2, width, length);

        // Cargo lines (X shape on top)
        gc.setStroke(Color.DARKGRAY);
        gc.strokeLine(-width / 2, -length/4, width / 2, length/2);
        gc.strokeLine(width / 2, -length/4, -width / 2, length/2);

        // Windshield
        gc.setFill(Color.GRAY);
        gc.fillRect(-width / 2 + 1, -length / 2 + 1, width - 2, length / 6);
    }
}

// Class for "DEFAULT_TAXITYPE"
class TaxiRenderer extends VehicleRenderer {
    @Override
    protected void drawBody(GraphicsContext gc, double length, double width, Color color) {
        gc.setFill(Color.GOLD);

        // Body
        gc.fillRoundRect(-width / 2, -length / 2, width, length, 4, 4);
        gc.strokeRoundRect(-width / 2, -length / 2, width, length, 4, 4);

        // Windshield
        gc.setFill(Color.BLACK);
        gc.fillRoundRect(-width / 2 + 1, -length / 2 + 2, width - 2, length / 4, 2, 2);

        // Define sign dimensions
        double signWidth = width * 0.85;
        double signHeight = length * 0.35;
        double signYPosition = 0;

        //Draw the Sign Background
        gc.setFill(Color.BLACK);
        gc.fillRoundRect(-signWidth / 2, signYPosition, signWidth, signHeight, 3, 3);

        //Draw the Text "TAXI"
        gc.setFill(Color.YELLOW);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        //Bold Serif font
        gc.setFont(Font.font("Serif", FontWeight.BOLD, signHeight * 0.55));

        //Draw text centered
        gc.fillText("TAXI", 0, signYPosition + (signHeight / 2));
    }
}

// Class for "Evehicle"
class EvehicleRenderer extends VehicleRenderer {
    @Override
    protected void drawBody(GraphicsContext gc, double length, double width, Color color) {
        // Electric Green Glow
        gc.setLineWidth(1.5);
        gc.setFill(color);

        // Futuristic smoothed shape
        gc.fillRoundRect(-width / 2, -length / 2, width, length, 8, 8);
        gc.strokeRoundRect(-width / 2, -length / 2, width, length, 8, 8);

        // Blue-tinted Windshield
        gc.setFill(Color.CYAN);
        gc.fillRoundRect(-width / 2 + 1, -length / 2 + 2, width - 2, length / 4, 2, 2);
    }
}