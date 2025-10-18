package com.parking;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ControlPanel extends VBox {

    public ControlPanel(BackendConnector connector) {
        getStyleClass().add("control-panel");
        setPrefWidth(350);
        getChildren().addAll(createParkSection(connector), createRemoveSection(connector));
    }

    private VBox createParkSection(BackendConnector connector) {
        VBox section = new VBox(15);
        Label title = new Label("Park Vehicle");
        title.getStyleClass().add("control-panel-title");

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        
        TextField plateField = new TextField();
        plateField.setPromptText("MH01AB1234");
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("CAR", "BIKE", "TRUCK");
        typeCombo.setValue("CAR");

        form.add(new Label("License Plate:"), 0, 0);
        form.add(plateField, 1, 0);
        form.add(new Label("Vehicle Type:"), 0, 1);
        form.add(typeCombo, 1, 1);

        Button parkBtn = new Button("Park Vehicle");
        parkBtn.setMaxWidth(Double.MAX_VALUE);
        parkBtn.getStyleClass().add("button-park");
        
        // --- ADD ICON TO BUTTON ---
        ImageView parkIcon = new ImageView(new Image(getClass().getResourceAsStream("assets/park_icon.png")));
        parkIcon.setFitHeight(16);
        parkIcon.setFitWidth(16);
        parkBtn.setGraphic(parkIcon);
        parkBtn.setContentDisplay(ContentDisplay.LEFT);
        
        parkBtn.setOnAction(e -> {
            String plate = plateField.getText().trim().toUpperCase();
            String type = typeCombo.getValue();
            if (!plate.isEmpty()) {
                connector.parkVehicle(plate, type);
                plateField.clear();
            }
        });

        section.getChildren().addAll(title, form, parkBtn);
        return section;
    }
    
    private VBox createRemoveSection(BackendConnector connector) {
        VBox section = new VBox(15);
        Label title = new Label("Remove Vehicle");
        title.getStyleClass().add("control-panel-title");

        TextField removePlateField = new TextField();
        removePlateField.setPromptText("Enter License Plate");

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        
        form.add(new Label("License Plate:"), 0, 0);
        form.add(removePlateField, 1, 0);
        
        Button removeBtn = new Button("Remove Vehicle"); // Shortened text
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.getStyleClass().add("button-remove");
        
        // --- ADD ICON TO BUTTON ---
        ImageView removeIcon = new ImageView(new Image(getClass().getResourceAsStream("assets/remove_icon.png")));
        removeIcon.setFitHeight(16);
        removeIcon.setFitWidth(16);
        removeBtn.setGraphic(removeIcon);
        removeBtn.setContentDisplay(ContentDisplay.LEFT);

        removeBtn.setOnAction(e -> {
            String plate = removePlateField.getText().trim().toUpperCase();
            if (!plate.isEmpty()) {
                connector.removeVehicle(plate); 
                removePlateField.clear();
            }
        });

        section.getChildren().addAll(title, form, removeBtn);
        return section;
    }
}