package com.parking;

import java.util.Random;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ControlPanel extends VBox {

    private BackendConnector connector;

    private TitledPane entryGatePane, exitGatePane, findCarPane, detailsPane;

    private TextField entryPlateField, entryNameField;
    private ComboBox<String> entryTypeCombo;
    private Spinner<Integer> durationSpinner;
    private CheckBox valetCheck;

    private TextField exitPlateField;
    private TextField findPlateField;
    private VBox waitingQueueBox;
    private Label queueBike, queueCarGuest, queueCarHotel, queueCarResident, queueTruck, queueStaff;

    private Label detailPlate, detailName, detailTime;

    public ControlPanel(BackendConnector connector) {
        this.connector = connector;
        getStyleClass().add("control-panel");
        setPrefWidth(380); // Wider panel

        detailsPane = new TitledPane("Slot Details", createDetailsSection());
        entryGatePane = new TitledPane("Entry Gate", createEntryGate());
        exitGatePane = new TitledPane("Exit & Validation", createExitGate());
        findCarPane = new TitledPane("Find My Car", createFindCar());

        waitingQueueBox = createWaitingQueueSection();

        // Make panes collapsible
        detailsPane.setCollapsible(true);
        entryGatePane.setCollapsible(true);
        exitGatePane.setCollapsible(true);
        findCarPane.setCollapsible(true);
        entryGatePane.setExpanded(true); // Start with Entry expanded

        getChildren().addAll(detailsPane, entryGatePane, exitGatePane, findCarPane, waitingQueueBox);
    }

    private VBox createDetailsSection() {
        VBox box = new VBox(10);
        detailPlate = new Label("Plate: -");
        detailName = new Label("Name: -");
        detailTime = new Label("Entry: -");

        detailPlate.getStyleClass().add("detail-label");
        detailName.getStyleClass().add("detail-label");
        detailTime.getStyleClass().add("detail-label");

        box.getChildren().addAll(detailPlate, detailName, detailTime);
        return box;
    }

    private VBox createEntryGate() {
        VBox box = new VBox(15);

        Button lprButton = new Button("Simulate LPR Scan");
        lprButton.setMaxWidth(Double.MAX_VALUE);
        lprButton.setOnAction(e -> entryPlateField.setText(generateRandomPlate()));

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        entryPlateField = new TextField();
        entryPlateField.setPromptText("Plate Number (e.g., MH01AA1111)");
        entryNameField = new TextField();
        entryNameField.setPromptText("Driver Name (Optional for Guests)");

        entryTypeCombo = new ComboBox<>(FXCollections.observableArrayList("BIKE", "CAR", "TRUCK"));
        entryTypeCombo.setValue("CAR");

        durationSpinner = new Spinner<>(1, 24, 1);
        durationSpinner.setEditable(true); // Allow typing duration

        valetCheck = new CheckBox("Valet Service (\u20B9" + String.format("%.0f", 150.0) + ")"); // Use constant

        form.add(new Label("Plate:"), 0, 0); form.add(entryPlateField, 1, 0);
        form.add(new Label("Name:"), 0, 1); form.add(entryNameField, 1, 1);
        form.add(new Label("Type:"), 0, 2); form.add(entryTypeCombo, 1, 2);
        form.add(new Label("Duration (Guests):"), 0, 3); form.add(durationSpinner, 1, 3);
        form.add(valetCheck, 1, 4);

        Button parkBtn = createIconButton("Park Vehicle", "assets/park_icon.png");
        parkBtn.getStyleClass().add("button-park");
        parkBtn.setOnAction(e -> {
            connector.parkVehicle(
                entryPlateField.getText(), // Let connector handle trimming/case
                entryTypeCombo.getValue(),
                entryNameField.getText(),
                durationSpinner.getValue(),
                valetCheck.isSelected() ? 1 : 0
            );
            // Clear only on success? Maybe not, keep UI responsive.
            // entryPlateField.clear();
            // entryNameField.clear();
        });

        box.getChildren().addAll(lprButton, form, parkBtn);
        return box;
    }

    private VBox createExitGate() {
        VBox box = new VBox(15);

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        exitPlateField = new TextField();
        exitPlateField.setPromptText("Enter Plate Number");
        form.add(new Label("Plate:"), 0, 0); form.add(exitPlateField, 1, 0);

        Button validateBtn = createIconButton("Apply Restaurant Validation", "assets/validate.png");
        validateBtn.setOnAction(e -> connector.applyValidation(exitPlateField.getText()));

        Button removeBtn = createIconButton("Calculate Fee / Process Exit", "assets/remove_icon.png");
        removeBtn.getStyleClass().add("button-remove");
        removeBtn.setOnAction(e -> connector.removeVehicle(exitPlateField.getText()));

        box.getChildren().addAll(form, validateBtn, removeBtn);
        return box;
    }

    private VBox createFindCar() {
        VBox box = new VBox(15);
        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        findPlateField = new TextField();
        findPlateField.setPromptText("Enter Plate Number");
        form.add(new Label("Plate:"), 0, 0); form.add(findPlateField, 1, 0);

        Button findBtn = createIconButton("Find My Car", "assets/search.png");
        findBtn.setOnAction(e -> connector.findCar(findPlateField.getText()));

        box.getChildren().addAll(form, findBtn);
        return box;
    }

    private VBox createWaitingQueueSection() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10, 0, 0, 0));
        Label title = new Label("Waiting Queues");
        title.getStyleClass().add("control-panel-title");
        queueBike = new Label("Bikes: 0");
        queueCarGuest = new Label("Guests (Car): 0");
        queueCarHotel = new Label("Hotel (Car): 0");
        queueCarResident = new Label("Residents (Car): 0");
        queueTruck = new Label("Trucks: 0");
        queueStaff = new Label("Staff: 0");
        box.getChildren().addAll(title, queueBike, queueCarGuest, queueCarHotel, queueCarResident, queueTruck, queueStaff);
        return box;
    }

    // --- Public Methods ---
    public void setPlate(String plate) {
        if (plate != null && !plate.equals("N/A")) {
             if (exitPlateField != null) exitPlateField.setText(plate);
             if (findPlateField != null) findPlateField.setText(plate);
        }
    }

    public void displayDetails(String data) {
         if (data == null) { clearDetails(); return; }
        String[] parts = data.split(",");
        if(parts.length == 3) {
            if (detailPlate != null) detailPlate.setText("Plate: " + parts[0]);
            if (detailName != null) detailName.setText("Name: " + parts[1]);
            if (detailTime != null) detailTime.setText("Entry: " + parts[2]);
        } else {
             System.err.println("Malformed details data: " + data);
             clearDetails();
        }
    }

    public void clearDetails() {
        if (detailPlate != null) detailPlate.setText("Plate: -");
        if (detailName != null) detailName.setText("Name: -");
        if (detailTime != null) detailTime.setText("Entry: -");
    }

    public void updateWaitingQueues(String queueData) {
        // Reset all labels safely
        if (queueBike != null) queueBike.setText("Bikes: 0");
        if (queueCarGuest != null) queueCarGuest.setText("Guests (Car): 0");
        if (queueCarHotel != null) queueCarHotel.setText("Hotel (Car): 0");
        if (queueCarResident != null) queueCarResident.setText("Residents (Car): 0");
        if (queueTruck != null) queueTruck.setText("Trucks: 0");
        if (queueStaff != null) queueStaff.setText("Staff: 0");

        if (queueData == null || queueData.isEmpty()) return;

        String[] queues = queueData.split(";");
        for (String q : queues) {
             if (q.isEmpty()) continue;
            String[] parts = q.split(":");
            if (parts.length == 2) {
                String type = parts[0];
                String count = parts[1];
                if (type.equals("BIKE") && queueBike != null) queueBike.setText("Bikes: " + count);
                else if (type.equals("CAR_GUEST") && queueCarGuest != null) queueCarGuest.setText("Guests (Car): " + count);
                else if (type.equals("CAR_HOTEL") && queueCarHotel != null) queueCarHotel.setText("Hotel (Car): " + count);
                else if (type.equals("CAR_RESIDENT") && queueCarResident != null) queueCarResident.setText("Residents (Car): " + count);
                else if (type.equals("TRUCK") && queueTruck != null) queueTruck.setText("Trucks: " + count);
                else if (type.equals("STAFF") && queueStaff != null) queueStaff.setText("Staff: " + count);
            } else {
                 System.err.println("Malformed queue data: " + q);
            }
        }
    }

    // --- Helpers ---
    private Button createIconButton(String text, String iconPath) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        try {
            Image iconImage = new Image(getClass().getResourceAsStream(iconPath));
            ImageView icon = new ImageView(iconImage);
            icon.setFitHeight(16);
            icon.setFitWidth(16);
            btn.setGraphic(icon);
            btn.setContentDisplay(ContentDisplay.LEFT);
            btn.setGraphicTextGap(10); // Add gap between icon and text
        } catch (Exception e) {
            System.err.println("Could not load icon: " + iconPath + " - " + e.getMessage());
        }
        return btn;
    }

    private String generateRandomPlate() {
        Random r = new Random();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return String.format("MH%02d%c%c%04d",
            r.nextInt(20) + 1, // MH01 to MH20
            letters.charAt(r.nextInt(26)),
            letters.charAt(r.nextInt(26)),
            r.nextInt(10000));
    }
}