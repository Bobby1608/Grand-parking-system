package com.parking;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ParkingVisualization extends Pane {
    private Map<Integer, ParkingSlotUI> slotVisuals = new HashMap<>();

    public ParkingVisualization(int totalSlots) {
        getStyleClass().add("parking-visualization-pane");
        initializeLayout(totalSlots);
    }

    private void initializeLayout(int totalSlots) {
        int slotsPerRow = 10;
        double startX = 20, startY = 50;
        double slotWidth = 80, slotHeight = 120;
        double xGap = 15, yGap = 20;
        
        for (int i = 1; i <= totalSlots; i++) {
            int row = (i - 1) / slotsPerRow;
            int col = (i - 1) % slotsPerRow;
            double x = startX + col * (slotWidth + xGap);
            double y = startY + row * (slotHeight + yGap);
            ParkingSlotUI slot = new ParkingSlotUI(i, x, y, slotWidth, slotHeight);
            slotVisuals.put(i, slot);
            getChildren().add(slot);
        }
    }

    public void updateSlotStatus(int slotId, boolean occupied, String plateNumber, String vehicleType) {
        ParkingSlotUI slot = slotVisuals.get(slotId);
        if (slot != null) {
            slot.updateStatus(occupied, plateNumber, vehicleType);
        }
    }
}

class ParkingSlotUI extends Group {
    private Rectangle background;
    private Text slotIdText;
    private Text plateText;
    private Text typeText;

    public ParkingSlotUI(int id, double x, double y, double width, double height) {
        background = new Rectangle(x, y, width, height);
        background.getStyleClass().add("parking-slot");

        slotIdText = new Text(x + width / 2 - 20, y + 25, "Slot " + id);
        slotIdText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        plateText = new Text(x + 10, y + 60, "EMPTY");
        plateText.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        
        typeText = new Text(x + 10, y + 90, "");
        typeText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));

        getChildren().addAll(background, slotIdText, plateText, typeText);
        updateStatus(false, "", "");
    }

    public void updateStatus(boolean occupied, String plate, String type) {
        // Remove old styles
        background.getStyleClass().removeAll("parking-slot-available", "parking-slot-occupied");
        slotIdText.getStyleClass().removeAll("slot-text-available", "slot-text-occupied");
        
        if (occupied) {
            background.getStyleClass().add("parking-slot-occupied");
            plateText.setText(plate);
            typeText.setText(type);
            slotIdText.getStyleClass().add("slot-text-occupied");
            plateText.getStyleClass().add("slot-text-occupied");
            typeText.getStyleClass().add("slot-text-occupied");
        } else {
            background.getStyleClass().add("parking-slot-available");
            plateText.setText("AVAILABLE");
            typeText.setText("");
            slotIdText.getStyleClass().add("slot-text-available");
            plateText.getStyleClass().add("slot-text-available");
            typeText.getStyleClass().add("slot-text-available");
        }
    }
}