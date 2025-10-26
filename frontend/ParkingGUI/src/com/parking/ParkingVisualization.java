package com.parking;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

class ParkingSlotView extends Group {
    private Rectangle background;
    private Text slotIdText;
    private Text plateText;

    private String plate = "N/A";
    private int floor;
    public boolean isOccupied = false;
    private FadeTransition flashAnimation;

    public ParkingSlotView(int id, int floor, double x, double y, double width, double height) {
        this.floor = floor;

        background = new Rectangle(x, y, width, height);
        background.getStyleClass().add("parking-slot");

        slotIdText = new Text(x + 10, y + 25, "Slot " + id);
        slotIdText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        plateText = new Text(x + 10, y + 60, "EMPTY");
        plateText.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        plateText.setWrappingWidth(width - 20);

        getChildren().addAll(background, slotIdText, plateText);

        updateStatus(false, false, false, "N/A");

        // Initialize highlight animation safely
        flashAnimation = new FadeTransition(Duration.millis(400), background);
        flashAnimation.setFromValue(1.0);
        flashAnimation.setToValue(0.2);
        flashAnimation.setCycleCount(Timeline.INDEFINITE);
        flashAnimation.setAutoReverse(true);
        flashAnimation.setInterpolator(Interpolator.EASE_BOTH);
    }

    public String getPlate() { return plate; }
    public int getFloor() { return floor; }

    public void updateStatus(boolean occupied, boolean reserved, boolean overstay, String plate) {
        this.isOccupied = occupied;
        this.plate = plate;

        stopHighlight();

        background.getStyleClass().removeAll(
                "parking-slot-available", "parking-slot-occupied",
                "parking-slot-reserved", "parking-slot-overstay");
        slotIdText.getStyleClass().removeAll(
                "slot-text-available", "slot-text-occupied",
                "slot-text-reserved");
        plateText.getStyleClass().removeAll(
                "slot-text-available", "slot-text-occupied",
                "slot-text-reserved");

        if (overstay) {
            background.getStyleClass().add("parking-slot-overstay");
            plateText.setText(plate);
            slotIdText.getStyleClass().add("slot-text-occupied");
            plateText.getStyleClass().add("slot-text-occupied");
            startHighlight();
        } else if (occupied) {
            background.getStyleClass().add("parking-slot-occupied");
            plateText.setText(plate);
            slotIdText.getStyleClass().add("slot-text-occupied");
            plateText.getStyleClass().add("slot-text-occupied");
        } else if (reserved) {
            background.getStyleClass().add("parking-slot-reserved");
            plateText.setText(plate);
            slotIdText.getStyleClass().add("slot-text-reserved");
            plateText.getStyleClass().add("slot-text-reserved");
        } else {
            background.getStyleClass().add("parking-slot-available");
            plateText.setText("AVAILABLE");
            slotIdText.getStyleClass().add("slot-text-available");
            plateText.getStyleClass().add("slot-text-available");
        }
    }

    // Find My Car Temporary Flash
    public void highlight() {
        FadeTransition ft = new FadeTransition(Duration.millis(350), background);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(8);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void startHighlight() {
        if (flashAnimation != null) {
            flashAnimation.play();
        }
    }

    private void stopHighlight() {
        if (flashAnimation != null) {
            flashAnimation.stop();
            background.setOpacity(1.0);
        }
    }
}
