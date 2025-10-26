package com.parking;

import java.util.Comparator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class AnalyticsPanel extends BorderPane {

    private BarChart<String, Number> peakHoursChart;
    private PieChart occupancyChart;
    private Label revenueLabel;

    private ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

    public AnalyticsPanel() {
        getStyleClass().add("analytics-panel"); // Use specific style class
        setPadding(new Insets(20));

        // --- Revenue Box ---
        VBox revenueBox = new VBox(10);
        revenueBox.setStyle("-fx-background-color: rgba(44, 62, 80, 0.7); -fx-padding: 20; -fx-background-radius: 10;");
        Label revenueTitle = new Label("Total Revenue Today");
        revenueTitle.getStyleClass().add("control-panel-title");
        revenueLabel = new Label("\u20B90.00");
        revenueLabel.getStyleClass().add("revenue-label");
        revenueBox.getChildren().addAll(revenueTitle, revenueLabel);
        setTop(revenueBox);
        BorderPane.setMargin(revenueBox, new Insets(0, 0, 20, 0)); // Add margin below

        // --- Peak Hours Chart ---
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Hour of Day");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Vehicles Entered");
        yAxis.setTickUnit(1); // Ensure integer ticks
        yAxis.setMinorTickVisible(false);

        peakHoursChart = new BarChart<>(xAxis, yAxis);
        peakHoursChart.setTitle("Peak Hour Analysis");
        peakHoursChart.setLegendVisible(false);
        setCenter(peakHoursChart);
        BorderPane.setMargin(peakHoursChart, new Insets(0, 10, 0, 0)); // Add margin right

        // --- Occupancy Chart ---
        occupancyChart = new PieChart(pieChartData);
        occupancyChart.setTitle("Live Occupancy by User Type");
        setRight(occupancyChart);
    }

    public void updateData(String data) {
         if (data == null || data.isEmpty()) {
             System.err.println("Received empty analytics data.");
             return;
         }
        String[] parts = data.split("\\|");
        if (parts.length < 3) {
             System.err.println("Analytics data format error. Expected 3 parts, got " + parts.length + ". Data: " + data);
             return; // Not enough data
        }

        String peakData = parts[0];
        String pieData = parts[1];
        String revenue = parts[2];

        // 1. Update Peak Hours Chart
        peakHoursChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Entries"); // Add name for clarity if legend is enabled later
        if (!peakData.isEmpty()) {
            ObservableList<XYChart.Data<String, Number>> dataList = FXCollections.observableArrayList();
            for (String part : peakData.split(";")) {
                if (part.isEmpty()) continue;
                String[] pair = part.split(",");
                if (pair.length == 2) {
                    try {
                        int hour24 = Integer.parseInt(pair[0]);
                        int count = Integer.parseInt(pair[1]);

                        String hourLabel;
                        if (hour24 == 0) hourLabel = "12 AM";
                        else if (hour24 == 12) hourLabel = "12 PM";
                        else if (hour24 > 12) hourLabel = (hour24 - 12) + " PM";
                        else hourLabel = hour24 + " AM";

                        dataList.add(new XYChart.Data<>(hourLabel, count));
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing peak hour data: " + part + " - " + e.getMessage());
                    }
                } else {
                     System.err.println("Malformed peak hour pair: " + part);
                }
            }
             // Sort data by hour for better chart display
            dataList.sort(Comparator.comparingInt(d -> {
                try {
                    String label = d.getXValue();
                    int hour = Integer.parseInt(label.split(" ")[0]);
                    if (label.contains("PM") && hour != 12) hour += 12;
                    if (label.contains("AM") && hour == 12) hour = 0; // Midnight case
                    return hour;
                } catch (Exception e) { return 24; } // Put errors at the end
            }));
            series.setData(dataList);
        }
        peakHoursChart.getData().add(series);

        // 2. Update Pie Chart
        pieChartData.clear();
        // GUEST(0), RESIDENT(1), HOTEL(2), STAFF(3)
        String[] userTypes = {"Guest", "Resident", "Hotel", "Staff"};
        int totalOccupancy = 0;
        if (!pieData.isEmpty()) {
            for (String part : pieData.split(";")) {
                 if (part.isEmpty()) continue;
                String[] pair = part.split(",");
                if (pair.length == 2) {
                    try {
                        int typeIndex = Integer.parseInt(pair[0]);
                        int count = Integer.parseInt(pair[1]);
                        if (typeIndex >= 0 && typeIndex < userTypes.length && count > 0) {
                             pieChartData.add(new PieChart.Data(userTypes[typeIndex] + " (" + count + ")", count));
                             totalOccupancy += count;
                        }
                    } catch (NumberFormatException e) {
                         System.err.println("Error parsing pie chart data: " + part + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Malformed pie data pair: " + part);
                }
            }
        }
        occupancyChart.setTitle("Live Occupancy by User Type (Total: " + totalOccupancy + ")");


        // 3. Update Revenue
        try {
            revenueLabel.setText("\u20B9" + String.format("%.2f", Double.parseDouble(revenue)));
        } catch (NumberFormatException e) {
             System.err.println("Error parsing revenue data: " + revenue + " - " + e.getMessage());
             revenueLabel.setText("\u20B9?.??");
        }
    }
}