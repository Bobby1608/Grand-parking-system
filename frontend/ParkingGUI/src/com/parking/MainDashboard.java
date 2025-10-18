package com.parking;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainDashboard extends Application {
    private ParkingVisualization parkingVisualization;
    private ControlPanel controlPanel;
    private Label statusMessageLabel;
    private BackendConnector backendConnector;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Grand Parking Lot Management System");
        // Optional: Set an icon for the application window itself
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("assets/logo.png")));

        backendConnector = new BackendConnector(this::updateUI, this::showStatusMessage);

        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");

        // --- NEW HEADER ---
        mainLayout.setTop(createHeader());

        // Center: Parking Visualization
        parkingVisualization = new ParkingVisualization(20);
        mainLayout.setCenter(parkingVisualization);

        // Right: Control Panel
        controlPanel = new ControlPanel(backendConnector);
        mainLayout.setRight(controlPanel);

        // Bottom: Status Bar
        mainLayout.setBottom(createStatusBar());
        
        Scene scene = new Scene(mainLayout, 1200, 700); // Increased height for the header
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> backendConnector.stopBackend());
        
        backendConnector.startBackend();
        backendConnector.getInitialStatus();
    }

    private HBox createHeader() {
        HBox headerPane = new HBox(15);
        headerPane.setAlignment(Pos.CENTER_LEFT);
        headerPane.getStyleClass().add("header-pane");

        // Load the logo
        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("assets/logo.png")));
        logoView.setFitHeight(40);
        logoView.setFitWidth(40);

        // Create the title
        Label titleLabel = new Label("Grand Parking Lot Management");
        titleLabel.getStyleClass().add("header-title");

        headerPane.getChildren().addAll(logoView, titleLabel);
        return headerPane;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.getStyleClass().add("status-bar");
        statusMessageLabel = new Label("System Initialized. Connecting to backend...");
        statusMessageLabel.getStyleClass().add("status-label");
        statusBar.getChildren().add(statusMessageLabel);
        return statusBar;
    }

    public void showStatusMessage(String message, boolean isError) {
        Platform.runLater(() -> {
            statusMessageLabel.setText(message);
            statusMessageLabel.getStyleClass().removeAll("status-label-success", "status-label-error");
            if (isError) {
                statusMessageLabel.getStyleClass().add("status-label-error");
            } else {
                statusMessageLabel.getStyleClass().add("status-label-success");
            }
        });
    }

    public void updateUI(String statusData) {
        Platform.runLater(() -> {
            String[] slotsData = statusData.split(";");
            for (String slotData : slotsData) {
                String[] parts = slotData.split(",");
                if (parts.length == 4) {
                    int slotId = Integer.parseInt(parts[0]);
                    boolean isOccupied = parts[1].equals("1");
                    String plate = parts[2];
                    String type = parts[3];
                    parkingVisualization.updateSlotStatus(slotId, isOccupied, plate, type);
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}