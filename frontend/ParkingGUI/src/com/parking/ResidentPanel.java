package com.parking;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
// --- END IMPORTS ---

public class ResidentPanel extends BorderPane {

    private BackendConnector connector;
    private ListView<String> userListView;
    private ObservableList<String> userList = FXCollections.observableArrayList();

    private TextField plateField, nameField, billingIdField;
    private ComboBox<String> userTypeCombo;

    public ResidentPanel(BackendConnector connector) {
        this.connector = connector;
        getStyleClass().add("resident-panel"); // Use specific style class
        setPadding(new Insets(20));

        // --- Left Side: Add User Form ---
        VBox formBox = createAddUserForm();
        setLeft(formBox);
        BorderPane.setMargin(formBox, new Insets(0, 20, 0, 0)); // Add margin


        // --- Center: User List ---
        userListView = new ListView<>(userList);
        userListView.setPlaceholder(new Label("No users registered yet. Use the form to add users."));
        setCenter(userListView);
    }

    private VBox createAddUserForm() {
        VBox box = new VBox(15);
        // Padding moved to BorderPane margin

        Label title = new Label("Register/Update User");
        title.getStyleClass().add("control-panel-title");

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);

        plateField = new TextField();
        plateField.setPromptText("MH01AA1111");
        nameField = new TextField();
        nameField.setPromptText("Full Name");
        billingIdField = new TextField();
        billingIdField.setPromptText("Apt 5B / Room 301 / N/A");

        // GUEST(0), RESIDENT(1), HOTEL(2), STAFF(3)
        userTypeCombo = new ComboBox<>(FXCollections.observableArrayList("Guest", "Resident", "Hotel", "Staff"));
        userTypeCombo.setValue("Resident");

        form.add(new Label("Plate:"), 0, 0); form.add(plateField, 1, 0);
        form.add(new Label("Name:"), 0, 1); form.add(nameField, 1, 1);
        form.add(new Label("Type:"), 0, 2); form.add(userTypeCombo, 1, 2);
        form.add(new Label("Billing ID:"), 0, 3); form.add(billingIdField, 1, 3);

        Button addBtn = new Button("Register/Update User");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().add("button-park"); // Re-use park button style
        addBtn.setOnAction(e -> {
            String plate = plateField.getText(); // Let connector handle trim/case
            String name = nameField.getText();
            String billId = billingIdField.getText();
            int typeIndex = userTypeCombo.getSelectionModel().getSelectedIndex();

            // Let connector handle validation
            connector.registerUser(plate, name, typeIndex, billId);

            // Clear fields after attempting registration
            plateField.clear();
            nameField.clear();
            billingIdField.clear();
            userTypeCombo.setValue("Resident"); // Reset combo box

            // Request updated user list after a short delay
             Timeline delay = new Timeline(new KeyFrame(Duration.millis(300), ev -> connector.getUsers()));
             delay.play();
        });

        box.getChildren().addAll(title, form, addBtn);
        return box;
    }

    public void updateUserList(String data) {
        userList.clear();
        if (data == null || data.isEmpty()) return;

        String[] users = data.split(";");
         String[] userTypes = {"Guest", "Resident", "Hotel", "Staff"}; // Match enum order
        for (String user : users) {
             if (user.isEmpty()) continue;
            String[] parts = user.split(",");
            //Format: Name,Plate,BillingID,Type(as int)
            if (parts.length == 4) {
                 try {
                     int typeIndex = Integer.parseInt(parts[3]);
                     String typeStr = (typeIndex >= 0 && typeIndex < userTypes.length) ? userTypes[typeIndex] : "Unknown";
                     // Format: Name (Plate) - Type - ID
                     userList.add(parts[0] + " (" + parts[1] + ") - " + typeStr + " - " + parts[2]);
                 } catch (NumberFormatException e) {
                      System.err.println("Error parsing user type index: " + parts[3]);
                      userList.add(parts[0] + " (" + parts[1] + ") - Error - " + parts[2]);
                 }
            } else {
                 System.err.println("Error parsing user data: " + user);
            }
        }
    }
}