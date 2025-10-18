package com.parking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BackendConnector {
    private Process backendProcess;
    private BufferedWriter processWriter;
    private BufferedReader processReader;
    
    // Callback to update the main UI grid
    private final Consumer<String> uiUpdater;
    // Callback to show messages in the status bar
    private final BiConsumer<String, Boolean> statusNotifier;

    public BackendConnector(Consumer<String> uiUpdater, BiConsumer<String, Boolean> statusNotifier) {
        this.uiUpdater = uiUpdater;
        this.statusNotifier = statusNotifier;
    }

    /**
     * Starts the C++ backend executable in a separate process.
     */
    public void startBackend() {
        try {
            // Using an absolute path is the most reliable way to find the executable
            String backendPath = "C:\\Users\\Bhuban Wakode\\Documents\\Projects\\grand-parking-system\\backend\\parking_backend.exe";
            
            // For a more flexible approach, you can use a relative path, but it can be less reliable
            // String backendPath = new File("../backend/parking_backend.exe").getCanonicalPath();
            
            ProcessBuilder pb = new ProcessBuilder(backendPath);
            pb.redirectErrorStream(true); // Combine error and input streams
            backendProcess = pb.start();

            processWriter = new BufferedWriter(new OutputStreamWriter(backendProcess.getOutputStream()));
            processReader = new BufferedReader(new InputStreamReader(backendProcess.getInputStream()));

            // Listen for responses from the backend in a separate thread to keep the UI responsive
            new Thread(() -> {
                try {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        handleBackendResponse(line);
                    }
                } catch (Exception e) {
                    // This happens if the backend process is terminated
                    statusNotifier.accept("Connection to backend lost.", true);
                }
            }).start();
            
            statusNotifier.accept("Backend connected successfully.", false);

        } catch (Exception e) {
            statusNotifier.accept("FATAL ERROR: Failed to start backend. Check the path and antivirus settings.", true);
        }
    }

    /**
     * Parses messages received from the C++ backend and acts accordingly.
     * @param response The raw string received from the backend.
     */
    private void handleBackendResponse(String response) {
        String[] parts = response.split(",", 2);
        String type = parts[0];
        String message = parts.length > 1 ? parts[1] : "";

        if ("SUCCESS".equals(type)) {
            statusNotifier.accept(message, false); // Show success message
            sendCommand("STATUS"); // Always refresh the UI after a successful action
        } else if ("ERROR".equals(type)) {
            statusNotifier.accept("Backend Error: " + message, true); // Show error message
        } else if ("STATUS".equals(type)) {
            uiUpdater.accept(message); // Update the parking grid visualization
        }
    }

    /**
     * Sends a command string to the running C++ backend process.
     * @param command The command to send (e.g., "PARK,MYCAR,CAR").
     */
    private void sendCommand(String command) {
        if (backendProcess == null || !backendProcess.isAlive()) {
            statusNotifier.accept("Backend is not running. Please restart the application.", true);
            return;
        }
        try {
            processWriter.write(command + "\n");
            processWriter.flush(); // Ensures the command is sent immediately
        } catch (Exception e) {
            statusNotifier.accept("Failed to send command to backend: " + e.getMessage(), true);
        }
    }
    
    public void getInitialStatus() {
        sendCommand("STATUS");
    }

    public void parkVehicle(String plate, String type) {
        sendCommand("PARK," + plate + "," + type);
    }
    
    public void removeVehicle(String plate) {
        sendCommand("REMOVE," + plate);
    }
    
    /**
     * Forcefully terminates the backend process when the application is closed.
     */
    public void stopBackend() {
        if (backendProcess != null) {
            backendProcess.destroyForcibly();
        }
    }
}