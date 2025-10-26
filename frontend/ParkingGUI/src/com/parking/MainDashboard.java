package com.parking;

// --- Necessary Imports ---
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * MainDashboard - primary JavaFX application.
 * Contains waiting queue UI and integration with BackendConnector queue APIs.
 */
public class MainDashboard extends Application {

    // --- Member Variables ---
    private TabPane mainTabPane;
    private TabPane parkingFloorTabPane; // For the floor tabs
    private AnalyticsPanel analyticsPanel;
    private ResidentPanel residentPanel;

    private Map<Integer, Pane> floorPanes = new HashMap<>();
    private Map<Integer, Tab> floorTabs = new HashMap<>(); // Store tabs for lookup
    private Map<Integer, Integer> floorSlotCount = new HashMap<>();
    private Map<Integer, ParkingSlotUI> allSlots = new HashMap<>(); // Master map of all slots

    private ControlPanel controlPanel;
    private Label statusMessageLabel;
    private BackendConnector backendConnector;
    private Label timeLabel;

    // Waiting queue UI
    private TableView<WaitingRow> waitingTable;
    private Label waitingSummaryLabel;

    // Prevent duplicate suggestion popups for same slot
    private final Map<Integer, AtomicBoolean> suggestionShownForSlot = new HashMap<>();

    // --- Default Constructor (Needed for JavaFX) ---
    public MainDashboard() {}

    // --- Start Method (Main UI Setup) ---
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Smart Complex Mobility Hub");
        try {
            // Set application icon (optional)
            Image icon = new Image(getClass().getResourceAsStream("assets/logo.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) { System.err.println("Error loading application icon: " + e.getMessage()); }

        // Initialize the backend connector, passing 'this' (MainDashboard instance)
        backendConnector = new BackendConnector(this, this::showStatusMessage);
        backendConnector.startBackend();

        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");

        // Setup UI sections
        mainLayout.setTop(createHeader());

        // --- Main TabPane Setup ---
        mainTabPane = new TabPane();

        // 1. Parking Tab
        Tab parkingTab = new Tab("Parking Lot");
        parkingTab.setClosable(false);
        parkingFloorTabPane = new TabPane();
        parkingFloorTabPane.getStyleClass().add("floor-tab-pane");
        parkingTab.setContent(parkingFloorTabPane);

        // 2. Analytics Tab
        Tab analyticsTab = new Tab("Analytics");
        analyticsTab.setClosable(false);
        analyticsPanel = new AnalyticsPanel(); // Create instance
        analyticsTab.setContent(analyticsPanel);
        // Add listener to refresh data when tab is selected
        analyticsTab.setOnSelectionChanged(e -> {
            if (analyticsTab.isSelected() && backendConnector != null) backendConnector.getAnalytics();
        });

        // 3. User Management Tab
        Tab userTab = new Tab("User Management");
        userTab.setClosable(false);
        residentPanel = new ResidentPanel(backendConnector); // Create instance
        userTab.setContent(residentPanel);
        // Add listener to refresh data when tab is selected
        userTab.setOnSelectionChanged(e -> {
            if (userTab.isSelected() && backendConnector != null) backendConnector.getUsers();
        });

        // Add tabs to the main pane
        mainTabPane.getTabs().addAll(parkingTab, analyticsTab, userTab);
        mainLayout.setCenter(mainTabPane);

        // --- Control Panel (right) ---
        controlPanel = new ControlPanel(backendConnector);
        VBox rightPane = new VBox(12);
        rightPane.setPadding(new Insets(12));
        rightPane.getChildren().addAll(controlPanel, createWaitingSection());

        // Make right pane scrollable so it never pushes under main content
        ScrollPane rightPaneScroll = new ScrollPane(rightPane);
        rightPaneScroll.setFitToWidth(true);
        rightPaneScroll.setFitToHeight(true);
        rightPaneScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rightPaneScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightPaneScroll.setPrefViewportHeight(700);
        rightPaneScroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        mainLayout.setRight(rightPaneScroll);

        // --- Status Bar ---
        mainLayout.setBottom(createStatusBar());

        // --- Scene Setup ---
        Scene scene = new Scene(mainLayout, 1350, 850);
        try {
            // load CSS from resources
            String css = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Error loading CSS: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Kick off periodic STATUS polling
        Timeline statusTicker = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (backendConnector != null) backendConnector.getInitialStatus();
        }));
        statusTicker.setCycleCount(Animation.INDEFINITE);
        statusTicker.play();

        // Request initial data
        backendConnector.getInitialStatus();
        backendConnector.getAnalytics();
        backendConnector.getUsers();
    }

    // --- Header UI ---
    private HBox createHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header-pane");
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView logo = null;
        try {
            Image img = new Image(getClass().getResourceAsStream("assets/logo.png"));
            logo = new ImageView(img);
            logo.setFitHeight(48);
            logo.setFitWidth(48);
        } catch (Exception ignored) {}

        Label title = new Label(" Smart Complex Mobility Hub");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timeLabel = new Label();
        timeLabel.getStyleClass().add("time-label");
        updateTime(); // initial

        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), ev -> updateTime()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();

        if (logo != null) header.getChildren().addAll(logo, title, spacer, timeLabel);
        else header.getChildren().addAll(title, spacer, timeLabel);
        return header;
    }

    private void updateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy hh:mm:ss a");
        timeLabel.setText(LocalDateTime.now().format(dtf));
    }

    // --- Status Bar ---
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(8, 16, 8, 16));
        statusBar.setSpacing(12);

        statusMessageLabel = new Label("Ready");
        statusMessageLabel.getStyleClass().add("status-label");

        statusBar.getChildren().add(statusMessageLabel);
        return statusBar;
    }

    // Small helper for notifications
    public void showStatusMessage(String message, boolean isError) {
        Platform.runLater(() -> {
            statusMessageLabel.setText(message);
            statusMessageLabel.getStyleClass().removeAll("status-label-success", "status-label-error");
            statusMessageLabel.getStyleClass().add(isError ? "status-label-error" : "status-label-success");
        });
    }

    // ------------------------
    // Waiting Queue UI Section
    // ------------------------
    private VBox createWaitingSection() {
        VBox box = new VBox(10);
        box.getStyleClass().add("waiting-section");
        box.setPadding(new Insets(8));

        Label header = new Label("Waiting Queues");
        header.getStyleClass().add("control-panel-title");

        waitingSummaryLabel = new Label("No waiting vehicles");
        waitingSummaryLabel.setWrapText(true);

        waitingTable = new TableView<>();
        waitingTable.setPlaceholder(new Label("No vehicles waiting"));

        TableColumn<WaitingRow, String> plateCol = new TableColumn<>("Plate");
        plateCol.setCellValueFactory(new PropertyValueFactory<>("plate"));
        plateCol.setPrefWidth(90);

        TableColumn<WaitingRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(140);

        TableColumn<WaitingRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);

        TableColumn<WaitingRow, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Assign");

            {
                btn.setOnAction(e -> {
                    WaitingRow row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        assignFromQueue(row.getType(), row.getPlate(), row.getName());
                    }
                });
                btn.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        actionCol.setPrefWidth(90);

        waitingTable.getColumns().addAll(plateCol, nameCol, typeCol, actionCol);
        waitingTable.setPrefHeight(240);

        box.getChildren().addAll(header, waitingSummaryLabel, waitingTable);
        return box;
    }

    /**
     * Called by BackendConnector when internal queue snapshots changed.
     * This method refreshes the waitingTable content.
     */
    public void updateWaitingQueuesUI() {
        Platform.runLater(() -> {
            List<BackendConnector.WaitingVehicle> bikes = backendConnector.getBikeQueueSnapshot();
            List<BackendConnector.WaitingVehicle> cars  = backendConnector.getCarQueueSnapshot();

            // build row items: label type = BIKE/CAR
            List<WaitingRow> rows = bikes.stream()
                    .map(w -> new WaitingRow(w.plate, w.name, w.type))
                    .collect(Collectors.toList());
            rows.addAll(cars.stream().map(w -> new WaitingRow(w.plate, w.name, w.type)).collect(Collectors.toList()));

            waitingTable.getItems().setAll(rows);

            int total = rows.size();
            if (total == 0) waitingSummaryLabel.setText("No waiting vehicles");
            else {
                long bcount = bikes.size();
                long ccount = cars.size();
                waitingSummaryLabel.setText("Total waiting: " + total + " (Bikes: " + bcount + ", Cars: " + ccount + ")");
            }
        });
    }

    // Data-holding POJO for TableView
    public static class WaitingRow {
        private final String plate;
        private final String name;
        private final String type;

        public WaitingRow(String plate, String name, String type) {
            this.plate = plate;
            this.name = name;
            this.type = type;
        }
        public String getPlate() { return plate; }
        public String getName() { return name; }
        public String getType() { return type; }
    }

    // ---- When operator clicks "Assign" in Waiting Table or popup chooses Assign Now
    private void assignFromQueue(String type, String plate, String name) {
        // For the auto-assignment we use default duration 1 and valet 0. Operator can edit after assignment if needed.
        // Poll the queue on backendConnector to remove the vehicle we've assigned (if it exists)
        // In some edge cases, the vehicle may have been removed already; this is safe.
        BackendConnector.WaitingVehicle front = backendConnector.peekNextForType(type);
        if (front == null) {
            showStatusMessage("No waiting vehicle for " + type + " found.", true);
            updateWaitingQueuesUI();
            return;
        }

        // If the peeked plate matches the requested plate, poll. Otherwise, we try to remove specifically by plate:
        BackendConnector.WaitingVehicle polled = null;
        if (front.plate.equalsIgnoreCase(plate)) {
            polled = backendConnector.pollNextForType(type);
        } else {
            // fallback: poll until matching plate found (rare, not ideal for large queues)
            // We'll dequeue into a temp list and re-enqueue non-matching vehicles.
            // Simpler approach: tell user to pick the top of the queue instead.
            showStatusMessage("Assign must be top-most waiting vehicle. Please assign oldest waiting vehicle first.", true);
            return;
        }

        if (polled != null) {
            showStatusMessage("Assigning " + polled.plate + " to next free slot...", false);
            // Send PARK command with defaults (duration=1, valet=0)
            backendConnector.parkVehicle(polled.plate, type, polled.name, 1, 0);
            // UI will refresh once backend sends a STATUS update
        } else {
            showStatusMessage("Could not remove from waiting queue.", true);
        }
    }

    // ------------------------
    // Status updates from backend
    // ------------------------
    public void updateUI(String statusData) {
        Platform.runLater(() -> {
            try {
                String[] parts = statusData.split("\\|", -1);
                String slotsInfo = parts[0];
                String queueInfo = parts.length > 1 ? parts[1] : "";

                // --- Process Slot Data ---
                String[] slotsData = slotsInfo.split(";");
                for (String slotData : slotsData) {
                    if (slotData.isEmpty()) continue;
                    String[] slotParts = slotData.split(",");
                    // Expected Format: slotId,isOccupied,isReserved,isOverstay,plate,floor
                    if (slotParts.length == 6) {
                        int slotId = Integer.parseInt(slotParts[0]);
                        boolean isOccupied = slotParts[1].equals("1");
                        boolean isReserved = slotParts[2].equals("1");
                        boolean isOverstay = slotParts[3].equals("1");
                        String plate = slotParts[4];
                        int floor = Integer.parseInt(slotParts[5]);

                        // Dynamically create floor tab if it doesn't exist
                        if (!floorTabs.containsKey(floor)) {
                             String floorName = "Floor " + floor; // Determine name later if needed
                            Tab tab = new Tab(floorName);
                            tab.setUserData(floor); // Store floor number
                            tab.setClosable(false);
                            Pane pane = new Pane();
                            pane.getStyleClass().add("parking-visualization-pane");
                            tab.setContent(pane);

                            parkingFloorTabPane.getTabs().add(tab);
                            // Sort tabs by floor number after adding
                            parkingFloorTabPane.getTabs().sort(Comparator.comparingInt(t -> (int) t.getUserData()));

                            floorPanes.put(floor, pane);
                            floorTabs.put(floor, tab); // Store the tab reference
                            floorSlotCount.put(floor, 0); // Initialize slot count for this floor
                        }

                        Pane currentFloorPane = floorPanes.get(floor);
                        if (currentFloorPane == null) continue; // Should not happen

                        // Dynamically create slot UI if it doesn't exist
                        if (!allSlots.containsKey(slotId)) {
                            int slotIndex = floorSlotCount.getOrDefault(floor, 0); // Get current count
                            int slotsPerRow = 10; // Layout: 10 slots per row
                            int row = slotIndex / slotsPerRow;
                            int col = slotIndex % slotsPerRow;
                            double slotWidth = 80;
                            double slotHeight = 120;
                            double xGap = 15;
                            double yGap = 20;
                            double x = 20 + col * (slotWidth + xGap);
                            double y = 50 + row * (slotHeight + yGap);

                            ParkingSlotUI slotUI = new ParkingSlotUI(slotId, floor, x, y, slotWidth, slotHeight);
                            // Add click listener
                            slotUI.setOnMouseClicked(e -> {
                                if (controlPanel != null) controlPanel.setPlate(slotUI.getPlate());
                                if (slotUI.isOccupied) { // Use the public field
                                    backendConnector.getSlotDetails(slotId);
                                } else {
                                    if (controlPanel != null) controlPanel.clearDetails();
                                }
                            });

                            allSlots.put(slotId, slotUI);
                            currentFloorPane.getChildren().add(slotUI);
                            floorSlotCount.put(floor, slotIndex + 1); // Increment count
                        }

                        // Update the visual status of the slot
                        if (allSlots.containsKey(slotId)) {
                            allSlots.get(slotId).updateStatus(isOccupied, isReserved, isOverstay, plate);
                        }

                        // If slot is free (not occupied), check if there's a waiting vehicle of matching type
                        if (!isOccupied) {
                            // Determine slot constraint type by current UI setup. We only support BIKE or CAR here.
                            // We derive type by looking at plate pattern or slot distribution? Simpler: use slotId -> floor mapping heuristic
                            // For robustness, we'll attempt to check top of bikeQueue/carQueue and suggest if available.
                            checkAndSuggestAssign(slotId);
                        }
                    } else {
                         // Log malformed data
                         System.err.println("Malformed slot data received: " + slotData);
                    }
                }
                // Update waiting queue display
                updateWaitingQueuesUI();
            } catch (Exception e) {
                 // Log any errors during UI update
                 System.err.println("Error during UI update: " + e.getMessage());
                 e.printStackTrace();
            }
        });
    }

    /**
     * When a free slot is seen, we check the top of queues and propose assignment.
     * This uses a simple heuristic:
     * - If bikeQueue has vehicles and bike slots exist -> propose to assign a bike
     * - Else if carQueue has vehicles -> propose assign car
     *
     * We guard against popping multiple popups for the same slot using suggestionShownForSlot map.
     */
    private void checkAndSuggestAssign(int freeSlotId) {
        if (suggestionShownForSlot.containsKey(freeSlotId) && suggestionShownForSlot.get(freeSlotId).get()) {
            return; // Already suggested for this slot
        }
        BackendConnector.WaitingVehicle bikeTop = backendConnector.peekNextForType("BIKE");
        BackendConnector.WaitingVehicle carTop  = backendConnector.peekNextForType("CAR");

        // If both exist, prefer BIKE if free slots typically for bikes. To be safe, show combined choices.
        if (bikeTop != null) {
            // Suggest assign bikeTop to this freeSlot
            suggestionShownForSlot.putIfAbsent(freeSlotId, new AtomicBoolean(false));
            showAssignPopupForSlot(freeSlotId, "BIKE", bikeTop);
        } else if (carTop != null) {
            suggestionShownForSlot.putIfAbsent(freeSlotId, new AtomicBoolean(false));
            showAssignPopupForSlot(freeSlotId, "CAR", carTop);
        }
    }

    private void showAssignPopupForSlot(int slotId, String type, BackendConnector.WaitingVehicle candidate) {
        // mark suggested so we don't flood
        suggestionShownForSlot.get(slotId).set(true);

        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Assign Waiting Vehicle");
            a.setHeaderText("Slot available (ID: " + slotId + ")");
            a.setContentText("Assign " + candidate.plate + " (" + candidate.name + ", " + type + ") to Slot " + slotId + "?");
            ButtonType assignNow = new ButtonType("Assign Now");
            ButtonType later = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(assignNow, later);

            a.showAndWait().ifPresent(btn -> {
                if (btn == assignNow) {
                    // Poll and assign
                    BackendConnector.WaitingVehicle polled = backendConnector.pollNextForType(type);
                    if (polled != null) {
                        // Use defaults duration=1, valet=0
                        backendConnector.parkVehicle(polled.plate, type, polled.name, 1, 0);
                        showStatusMessage("Assigned " + polled.plate + " to Slot " + slotId, false);
                    } else {
                        showStatusMessage("Vehicle not available in queue anymore.", true);
                    }
                } else {
                    // operator postponed
                    showStatusMessage("Assignment postponed", false);
                }
            });
        });
    }

    // Show detailed slot info panel
    public void showSlotDetails(String data) {
        if (controlPanel != null) {
            Platform.runLater(() -> controlPanel.displayDetails(data));
        }
    }

    public void highlightSlot(String slotIdStr, String floorStr) {
        Platform.runLater(() -> {
            try {
                int slotId = Integer.parseInt(slotIdStr);
                int floor = Integer.parseInt(floorStr);
                if (allSlots.containsKey(slotId)) {
                    ParkingSlotUI slot = allSlots.get(slotId);
                    // Select the correct floor tab
                    if (floorTabs.containsKey(floor)) {
                         mainTabPane.getSelectionModel().select(0); // Select the main Parking Lot tab first
                         parkingFloorTabPane.getSelectionModel().select(floorTabs.get(floor));
                    }
                    slot.highlight(); // Make the slot flash
                }
            } catch (Exception e) {
                showStatusMessage("Could not highlight slot: " + e.getMessage(), true);
            }
        });
    }

    // --- Main Method (Application Entry Point) ---
    public static void main(String[] args) {
        launch(args);
    }
} // --- End of MainDashboard Class ---
