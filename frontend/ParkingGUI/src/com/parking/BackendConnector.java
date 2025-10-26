package com.parking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;

/**
 * BackendConnector: manages communication with backend EXE and keeps
 * waiting-queues for items that couldn't be parked immediately.
 *
 * Exposes small API for MainDashboard to read/assign/poll queues.
 */
public class BackendConnector {
    private Process backendProcess;
    private BufferedWriter processWriter;
    private BufferedReader processReader;

    private MainDashboard mainApp;
    private BiConsumer<String, Boolean> statusNotifier;

    // Absolute path tuned earlier
    private static final String BACKEND_PATH =
        "C:\\Users\\Bhuban Wakode\\Documents\\Projects\\grand-parking-system\\backend\\parking_backend.exe";

    // ----- Waiting queues (frontend-side) -----
    public static class WaitingVehicle {
        public final String plate;
        public final String name;
        public final String type; // "BIKE" or "CAR" (or "CAR_GUEST"/etc)

        public WaitingVehicle(String plate, String name, String type) {
            this.plate = plate;
            this.name = (name == null || name.trim().isEmpty()) ? "Guest" : name.trim();
            this.type = type;
        }
    }

    private final Queue<WaitingVehicle> bikeQueue = new LinkedList<>();
    private final Queue<WaitingVehicle> carQueue  = new LinkedList<>();

    public BackendConnector(MainDashboard app, BiConsumer<String, Boolean> statusNotifier) {
        this.mainApp = app;
        this.statusNotifier = statusNotifier;
    }

    public void startBackend() {
        try {
            File backendFile = new File(BACKEND_PATH);
            if (!backendFile.exists()) {
                statusNotifier.accept(
                    "Backend not found at: " + BACKEND_PATH + 
                    ". Please verify path or antivirus restrictions.", true);
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(BACKEND_PATH);
            pb.redirectErrorStream(true);
            backendProcess = pb.start();

            processWriter = new BufferedWriter(new OutputStreamWriter(backendProcess.getOutputStream()));
            processReader = new BufferedReader(new InputStreamReader(backendProcess.getInputStream()));

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        System.out.println("RAW RCV <- " + line);
                        handleBackendResponse(line);
                    }
                } catch (Exception e) {
                    if (backendProcess != null && backendProcess.isAlive()) {
                        statusNotifier.accept("Lost backend connection: " + e.getMessage(), true);
                    }
                } finally {
                    System.out.println("Backend stopped or closed. Reader thread finished.");
                }
            });

            readerThread.setDaemon(true);
            readerThread.start();

            statusNotifier.accept("Backend connected successfully.", false);
        } catch (Exception e) {
            statusNotifier.accept(
                "FATAL: Could not start backend! Error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Handle incoming backend responses.
     *
     * We also parse messages that indicate waiting-queue placement such as:
     * "SUCCESS,Lot full for CAR_GUEST. Vehicle MH01XX1111 added to waiting queue."
     */
    private void handleBackendResponse(String response) {
        Platform.runLater(() -> {
            try {
                String[] parts = response.split(",", 2);
                String type = parts[0];
                String message = parts.length > 1 ? parts[1].trim() : "";

                if ("SUCCESS".equals(type)) {
                    // If backend tells us the vehicle was added to waiting queue, parse it
                    parseWaitingQueueMessage(message);

                    // Many SUCCESS messages carry either parking info or generic messages
                    statusNotifier.accept(message, false);

                    // Also request a fresh STATUS so UI gets the latest occupancy
                    sendCommand("STATUS");
                } else if ("ERROR".equals(type)) {
                    statusNotifier.accept("Backend Error: " + message, true);
                } else if ("STATUS".equals(type)) {
                    // forward raw status payload to UI updater
                    mainApp.updateUI(message);
                } else if ("ANALYTICS".equals(type)) {
                    // MainDashboard does not define updateAnalytics(String); instead,
                    // forward analytics payload as a status so the UI can surface it,
                    // or implement updateAnalytics in MainDashboard and revert this change.
                    statusNotifier.accept("ANALYTICS: " + message, false);
                } else if ("USERS".equals(type)) {
                    // MainDashboard may not implement updateUserList(String); forward as status
                    // so the UI can decide how to present the user list payload.
                    statusNotifier.accept("USERS: " + message, false);
                } else if ("DETAILS".equals(type) || "DETAIL".equals(type)) {
                    mainApp.showSlotDetails(message);
                } else {
                    // Unknown responses - forward as status so user sees them
                    statusNotifier.accept(response, false);
                }
            } catch (Exception e) {
                System.err.println("Error handling backend response: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // parse messages indicating waiting queue addition
    // Example matched texts:
    //  "Lot full for CAR_GUEST. Vehicle MH01XX1111 added to waiting queue."
    //  "Lot full for BIKE. Vehicle MH01XX1111 added to waiting queue."
    private void parseWaitingQueueMessage(String msg) {
        try {
            Pattern p = Pattern.compile("Lot full for\\s*(\\w+)[\\.]?\\s*Vehicle\\s*(\\S+)\\s*added", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(msg);
            if (m.find()) {
                String constraint = m.group(1).toUpperCase(); // e.g., CAR_GUEST or BIKE
                String plate = m.group(2).toUpperCase();

                // Map constraint to simplified type for frontend-queues
                String simplifiedType = mapConstraintToType(constraint);

                // We don't know the name from backend message; use "Guest" placeholder
                WaitingVehicle w = new WaitingVehicle(plate, "Guest", simplifiedType);
                if ("BIKE".equals(simplifiedType)) {
                    synchronized (bikeQueue) { bikeQueue.add(w); }
                } else {
                    synchronized (carQueue) { carQueue.add(w); }
                }

                // Tell UI to refresh waiting queue table
                mainApp.updateWaitingQueuesUI(); // new method in MainDashboard
            }
        } catch (Exception ignored) {}
    }

    private String mapConstraintToType(String constraint) {
        // Keep it simple: BIKE vs CAR
        if (constraint.contains("BIKE")) return "BIKE";
        return "CAR";
    }

    // ---------- Queue API for UI ----------

    public List<WaitingVehicle> getBikeQueueSnapshot() {
        synchronized (bikeQueue) {
            return new ArrayList<>(bikeQueue);
        }
    }

    public List<WaitingVehicle> getCarQueueSnapshot() {
        synchronized (carQueue) {
            return new ArrayList<>(carQueue);
        }
    }

    public WaitingVehicle peekNextForType(String type) {
        if ("BIKE".equals(type)) {
            synchronized (bikeQueue) { return bikeQueue.peek(); }
        } else {
            synchronized (carQueue) { return carQueue.peek(); }
        }
    }

    public WaitingVehicle pollNextForType(String type) {
        if ("BIKE".equals(type)) {
            synchronized (bikeQueue) { 
                WaitingVehicle v = bikeQueue.poll();
                mainApp.updateWaitingQueuesUI();
                return v;
            }
        } else {
            synchronized (carQueue) { 
                WaitingVehicle v = carQueue.poll();
                mainApp.updateWaitingQueuesUI();
                return v;
            }
        }
    }

    /** Forcefully add a waiting vehicle into queue (front-end action) */
    public void enqueueWaitingVehicle(String type, String plate, String name) {
        WaitingVehicle w = new WaitingVehicle(plate, name, type);
        if ("BIKE".equals(type)) {
            synchronized (bikeQueue) { bikeQueue.add(w); }
        } else {
            synchronized (carQueue) { carQueue.add(w); }
        }
        mainApp.updateWaitingQueuesUI();
    }

    // ---------- Commands ----------

    private void sendCommand(String command) {
        if (backendProcess == null || !backendProcess.isAlive()) {
            statusNotifier.accept("Backend offline. Command dropped: " + command, true);
            return;
        }

        try {
            System.out.println("SEND -> " + command);
            processWriter.write(command + "\n");
            processWriter.flush();
        } catch (Exception e) {
            statusNotifier.accept("Failed to send: " + e.getMessage(), true);
        }
    }

    public void getInitialStatus() { sendCommand("STATUS"); }
    public void getAnalytics() { sendCommand("GET_ANALYTICS"); }
    public void getUsers() { sendCommand("GET_USERS"); }
    public void getSlotDetails(int slotId) { sendCommand("GET_DETAILS," + slotId); }

    public void parkVehicle(String plate, String type, String name, int duration, int valet) {
        if (plate == null || plate.trim().isEmpty()) {
            statusNotifier.accept("Plate cannot be empty.", true); return;
        }
        sendCommand("PARK," + plate.trim().toUpperCase() + "," + type + "," + name.trim() + "," + duration + "," + valet);
    }

    public void removeVehicle(String plate) {
        if (plate == null || plate.trim().isEmpty()) {
            statusNotifier.accept("Plate cannot be empty.", true); return;
        }
        sendCommand("REMOVE," + plate.trim().toUpperCase());
    }

    public void applyValidation(String plate) {
        if (plate == null || plate.trim().isEmpty()) {
            statusNotifier.accept("Plate cannot be empty.", true); return;
        }
        sendCommand("VALIDATE," + plate.trim().toUpperCase());
    }

    public void findCar(String plate) {
        if (plate == null || plate.trim().isEmpty()) {
            statusNotifier.accept("Plate cannot be empty.", true); return;
        }
        sendCommand("FIND," + plate.trim().toUpperCase());
    }

    public void registerUser(String plate, String name, int type, String billingId) {
        if (plate == null || plate.trim().isEmpty() ||
            name == null || name.trim().isEmpty() ||
            billingId == null || billingId.trim().isEmpty()) {
            statusNotifier.accept("All fields required!", true);
            return;
        }
        sendCommand("REGISTER," + plate.trim().toUpperCase() + "," + name.trim() + "," + type + "," + billingId.trim());
    }

    public void stopBackend() {
        try {
            if (processWriter != null) processWriter.close();
            if (processReader != null) processReader.close();
            if (backendProcess != null && backendProcess.isAlive()) {
                backendProcess.destroyForcibly();
                System.out.println("Backend terminated.");
            }
        } catch (Exception ignored) {}
    }
}
