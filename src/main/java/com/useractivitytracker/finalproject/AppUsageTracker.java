package com.useractivitytracker.finalproject;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AppUsageTracker extends Application {

    private final Map<String, Map<String, Long>> appUsageDataByDate = new HashMap<>(); // Date -> (App -> Usage Time)
    private final Map<String, String> appCategories = new HashMap<>();
    private final Map<String, Long> appLimits = new HashMap<>();
    private String currentApp = "";
    private long lastSwitchTime = System.currentTimeMillis();
    private final PieChart pieChart = new PieChart();
    private final Text totalTimeText = new Text();
    private final ListView<String> appListView = new ListView<>();
    private final Label currentAppLabel = new Label("Current App: None"); // Label for current app and time
    private static final String CSV_FILE = "app_usage.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private boolean showingHistory = false; // State to track current view
    private static final String SELF_PROCESS_NAME = "java"; // Process name of this app (java.exe)
    private Button historyButton; // Declare as instance variable
    private Button backButton; // Declare as instance variable
    private Button themeToggleButton; // Button to toggle between dark and light modes
    private boolean isDarkMode = false; // Track the current theme
    private Scene scene; // Store the scene to modify stylesheets
    private final Map<String, String> appColors = new HashMap<>(); // Map to store fixed colors for each app
    private final List<String> colorPalette = Arrays.asList(
            "#FF6F61", "#6B5B95", "#88B04B", "#F7CAC9", "#92A8D1", "#955251", "#B565A7", "#009B77", "#DD4124", "#D65076"
    ); // Predefined color palette

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Configure PieChart
        pieChart.setTitle("Today's Top 3 App Usage");
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(true);
        pieChart.setLabelLineLength(10);
        pieChart.setAnimated(false); // Disable animation to prevent size change effect

        // Initialize PieChart with placeholder data
        pieChart.getData().add(new PieChart.Data("No usage data yet", 1));
        pieChart.getData().get(0).getNode().setStyle("-fx-pie-color: #cccccc;");

        // Configure current app label
        currentAppLabel.setId("current-app-label"); // Set ID for CSS styling
        currentAppLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); // Remove text-fill

        // Load existing data
        loadData();

        // Create UI Components
        Label titleLabel = new Label("Desktop App Usage Tracker");
        titleLabel.setId("title-label"); // Set ID for CSS styling
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;"); // Remove text-fill

        totalTimeText.setId("total-time-text"); // Set ID for CSS styling
        totalTimeText.setStyle("-fx-font-size: 20px;"); // Remove fill

        appListView.setPrefHeight(200);
        ScrollPane scrollPane = new ScrollPane(appListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // History and Back Buttons
        historyButton = new Button("View Last 7 Days History");
        historyButton.setId("history-button"); // Set ID for CSS styling
        historyButton.setOnAction(e -> {
            showingHistory = true;
            historyButton.setVisible(false);
            backButton.setVisible(true);
            currentAppLabel.setVisible(false); // Hide current app label in history mode
            updatePieChart();
            updateAppUsageList();
            pieChart.setTitle("Last 7 Days Top 3 App Usage");
        });

        backButton = new Button("Back to Today");
        backButton.setId("back-button"); // Set ID for CSS styling
        backButton.setOnAction(e -> {
            showingHistory = false;
            historyButton.setVisible(true);
            backButton.setVisible(false);
            currentAppLabel.setVisible(true); // Show current app label in today mode
            updatePieChart();
            updateAppUsageList();
            pieChart.setTitle("Today's Top 3 App Usage");
        });

        // Theme Toggle Button
        themeToggleButton = new Button("Switch to Dark Mode");
        themeToggleButton.setId("theme-toggle-button"); // Set ID for CSS styling
        themeToggleButton.setOnAction(e -> toggleTheme());

        // Button Box with conditional visibility
        HBox buttonBox = new HBox(10, historyButton, backButton, themeToggleButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));
        backButton.setVisible(false); // Initially hidden

        // Layout
        HBox header = new HBox(20, titleLabel, totalTimeText);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15));
        header.setId("header"); // Set ID for CSS styling

        VBox centerBox = new VBox(20, currentAppLabel, pieChart, scrollPane, buttonBox);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(centerBox);

        // Scene and Stage
        scene = new Scene(root, 800, 600);
        java.net.URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl == null) {
            System.err.println("Error: Could not find styles.css in classpath. Check src/main/resources.");
        } else {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("CSS loaded successfully: " + cssUrl.toExternalForm());
        }
        // Set initial theme to light mode
        scene.getRoot().getStyleClass().add("light-mode");
        primaryStage.setScene(scene);
        primaryStage.setTitle("Desktop App Usage Tracker");
        primaryStage.show();

        // Initial updates
        Platform.runLater(() -> {
            updatePieChart(); // Ensure the PieChart updates after the UI is rendered
            updateAppUsageList();
        });

        // Start tracking active applications
        startAppTracking();
        startAutoSave();
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        // Update the root style class to switch themes
        scene.getRoot().getStyleClass().removeAll("light-mode", "dark-mode");
        if (isDarkMode) {
            scene.getRoot().getStyleClass().add("dark-mode");
            themeToggleButton.setText("Switch to Light Mode");
        } else {
            scene.getRoot().getStyleClass().add("light-mode");
            themeToggleButton.setText("Switch to Dark Mode");
        }
    }

    private void startAppTracking() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String activeApp = getActiveWindowTitle();
                // Skip tracking if the active app is this JavaFX app (java.exe)
                if (activeApp.equalsIgnoreCase(SELF_PROCESS_NAME)) {
                    Platform.runLater(() -> currentAppLabel.setText("Current App: None"));
                    return;
                }

                long currentTime = System.currentTimeMillis();
                // Update the time for the current app even if the active app hasn't changed
                if (!currentApp.isEmpty() && !currentApp.equalsIgnoreCase(SELF_PROCESS_NAME)) {
                    String today = LocalDate.now().format(DATE_FORMATTER);
                    Map<String, Long> todayUsage = appUsageDataByDate.computeIfAbsent(today, k -> new HashMap<>());
                    // Add the elapsed time since the last tick
                    long elapsedTime = currentTime - lastSwitchTime;
                    todayUsage.put(currentApp,
                            todayUsage.getOrDefault(currentApp, 0L) + elapsedTime);
                }

                // If the active app has changed, update the current app
                if (!activeApp.equals(currentApp)) {
                    currentApp = activeApp;
                    lastSwitchTime = currentTime;
                    checkAppLimit(activeApp);
                } else {
                    // Update the last switch time for the next iteration
                    lastSwitchTime = currentTime;
                }

                // Update the current app label with the active app's usage time
                if (!currentApp.isEmpty() && !currentApp.equalsIgnoreCase(SELF_PROCESS_NAME)) {
                    String today = LocalDate.now().format(DATE_FORMATTER);
                    Map<String, Long> todayUsage = appUsageDataByDate.getOrDefault(today, new HashMap<>());
                    long usageTime = todayUsage.getOrDefault(currentApp, 0L);
                    long totalSeconds = usageTime / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;
                    String formattedTime = String.format("Current App: %s (%dh %dm %ds)", currentApp, hours, minutes, seconds);
                    Platform.runLater(() -> currentAppLabel.setText(formattedTime));
                }

                // Update the UI if not in history mode
                if (!showingHistory) {
                    updatePieChart();
                    updateAppUsageList();
                }
            }
        }, 0, 1000); // Update every second
    }

    private void updatePieChart() {
        Platform.runLater(() -> {
            pieChart.getData().clear();
            List<PieChart.Data> pieData = new ArrayList<>();
            if (showingHistory) {
                // Aggregate usage over the last 7 days, excluding this app
                Map<String, Long> aggregatedUsage = new HashMap<>();
                LocalDate today = LocalDate.now();
                for (int i = 0; i < 7; i++) {
                    String dateStr = today.minusDays(i).format(DATE_FORMATTER);
                    Map<String, Long> dateUsage = appUsageDataByDate.getOrDefault(dateStr, new HashMap<>());
                    for (Map.Entry<String, Long> entry : dateUsage.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(SELF_PROCESS_NAME)) continue;
                        aggregatedUsage.merge(entry.getKey(), entry.getValue(), Long::sum);
                    }
                }

                List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(aggregatedUsage.entrySet());
                sortedEntries.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

                int count = 0;
                long totalUsageTime = 0;
                for (Map.Entry<String, Long> entry : sortedEntries) {
                    if (count >= 3) break;
                    long totalSeconds = entry.getValue() / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;

                    String formattedTime = String.format("%s (%dh %dm %ds)", entry.getKey(), hours, minutes, seconds);
                    pieData.add(new PieChart.Data(formattedTime, totalSeconds));
                    totalUsageTime += totalSeconds;
                    count++;
                }
                updateTotalTime(totalUsageTime, true);
            } else {
                // Show today's usage, excluding this app
                String today = LocalDate.now().format(DATE_FORMATTER);
                Map<String, Long> todayUsage = appUsageDataByDate.getOrDefault(today, new HashMap<>());

                List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(todayUsage.entrySet());
                sortedEntries.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

                int count = 0;
                long totalUsageTime = 0;
                for (Map.Entry<String, Long> entry : sortedEntries) {
                    if (entry.getKey().equalsIgnoreCase(SELF_PROCESS_NAME)) continue;
                    if (count >= 3) break;
                    long totalSeconds = entry.getValue() / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;

                    String formattedTime = String.format("%s (%dh %dm %ds)", entry.getKey(), hours, minutes, seconds);
                    pieData.add(new PieChart.Data(formattedTime, totalSeconds));
                    totalUsageTime += totalSeconds;
                    count++;
                }
                updateTotalTime(totalUsageTime, false);
            }

            // Always ensure the PieChart has data
            if (pieData.isEmpty()) {
                pieChart.getData().add(new PieChart.Data("No usage data yet", 1));
            } else {
                pieChart.getData().addAll(pieData);
            }

            // Apply fixed colors to each slice based on the app name
            for (PieChart.Data data : pieChart.getData()) {
                if (data.getName().equals("No usage data yet")) {
                    data.getNode().setStyle("-fx-pie-color: #cccccc;"); // Gray color for placeholder
                } else {
                    String appName = data.getName().split(" ")[0]; // Extract the app name (before the time)
                    String color = appColors.computeIfAbsent(appName, k -> {
                        // Assign a color from the palette based on the app name's hash
                        int index = Math.abs(k.hashCode()) % colorPalette.size();
                        return colorPalette.get(index);
                    });
                    data.getNode().setStyle("-fx-pie-color: " + color + ";");
                }
            }
        });
    }

    private void updateTotalTime(long totalSeconds, boolean isHistory) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String formattedTime = isHistory
                ? String.format("Last 7 Days Total: %dh %dm %ds", hours, minutes, seconds)
                : String.format("Today's Total: %dh %dm %ds", hours, minutes, seconds);
        totalTimeText.setText(formattedTime);
    }

    private void checkAppLimit(String appName) {
        if (appName.equalsIgnoreCase(SELF_PROCESS_NAME)) return;
        long limit = appLimits.getOrDefault(appName, Long.MAX_VALUE);
        String today = LocalDate.now().format(DATE_FORMATTER);
        Map<String, Long> todayUsage = appUsageDataByDate.getOrDefault(today, new HashMap<>());
        long usageTime = todayUsage.getOrDefault(appName, 0L);
        if (usageTime > limit) {
            System.out.println(appName + " usage exceeded the time limit for today!");
        }
    }

    private String getActiveWindowTitle() {
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) return "Unknown";

        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid.getValue());
        if (processHandle == null) return "Unknown";

        try {
            char[] buffer = new char[512];
            int charsCopied = Psapi.INSTANCE.GetModuleFileNameExW(
                    processHandle, null, buffer, buffer.length);

            if (charsCopied > 0) {
                String fullPath = Native.toString(buffer).trim();
                String exeName = fullPath.substring(fullPath.lastIndexOf("\\") + 1);
                String appName = exeName.endsWith(".exe") ? exeName.substring(0, exeName.length() - 4) : exeName;
                return appName.substring(0, 1).toUpperCase() + appName.substring(1).toLowerCase();
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(processHandle);
        }
        return "Unknown";
    }

    private void loadData() {
        try {
            if (Files.exists(Paths.get(CSV_FILE))) {
                BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length == 4) {
                        String date = data[0].trim();
                        String appName = data[1].trim();
                        long usageTime = Long.parseLong(data[2].trim());
                        String category = data[3].trim();
                        // Skip loading data for this app
                        if (appName.equalsIgnoreCase(SELF_PROCESS_NAME)) continue;
                        Map<String, Long> dateUsage = appUsageDataByDate.computeIfAbsent(date, k -> new HashMap<>());
                        dateUsage.put(appName, usageTime);
                        appCategories.put(appName, category);
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveData() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE));
            for (Map.Entry<String, Map<String, Long>> dateEntry : appUsageDataByDate.entrySet()) {
                String date = dateEntry.getKey();
                for (Map.Entry<String, Long> appEntry : dateEntry.getValue().entrySet()) {
                    String appName = appEntry.getKey();
                    // Skip saving data for this app
                    if (appName.equalsIgnoreCase(SELF_PROCESS_NAME)) continue;
                    long usageTime = appEntry.getValue();
                    String category = appCategories.getOrDefault(appName, "Uncategorized");
                    writer.write(date + "," + appName + "," + usageTime + "," + category);
                    writer.newLine();
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAppCategory(String appName, String category) {
        if (appName.equalsIgnoreCase(SELF_PROCESS_NAME)) return;
        appCategories.put(appName, category);
    }

    public void setAppUsageLimit(String appName, long limitInSeconds) {
        if (appName.equalsIgnoreCase(SELF_PROCESS_NAME)) return;
        appLimits.put(appName, limitInSeconds * 1000);
    }

    @Override
    public void stop() {
        saveData();
    }

    private void updateAppUsageList() {
        Platform.runLater(() -> {
            appListView.getItems().clear();
            if (showingHistory) {
                // Show detailed usage for each of the last 7 days, excluding this app
                LocalDate today = LocalDate.now();
                for (int i = 0; i < 7; i++) {
                    LocalDate date = today.minusDays(i);
                    String dateStr = date.format(DATE_FORMATTER);
                    Map<String, Long> dateUsage = appUsageDataByDate.getOrDefault(dateStr, new HashMap<>());

                    appListView.getItems().add("--- " + dateStr + " ---");
                    long totalSecondsForDay = 0;
                    for (Map.Entry<String, Long> entry : dateUsage.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(SELF_PROCESS_NAME)) continue;
                        long totalSeconds = entry.getValue() / 1000;
                        totalSecondsForDay += totalSeconds;
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;
                        appListView.getItems().add(String.format("%s: %dh %dm %ds", entry.getKey(), hours, minutes, seconds));
                    }
                    long hours = totalSecondsForDay / 3600;
                    long minutes = (totalSecondsForDay % 3600) / 60;
                    long seconds = totalSecondsForDay % 60;
                    appListView.getItems().add(String.format("Total: %dh %dm %ds", hours, minutes, seconds));
                    appListView.getItems().add(""); // Separator
                }
            } else {
                // Show today's usage, excluding this app
                String today = LocalDate.now().format(DATE_FORMATTER);
                Map<String, Long> todayUsage = appUsageDataByDate.getOrDefault(today, new HashMap<>());

                for (Map.Entry<String, Long> entry : todayUsage.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(SELF_PROCESS_NAME)) continue;
                    long totalSeconds = entry.getValue() / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;
                    appListView.getItems().add(String.format("%s: %dh %dm %ds", entry.getKey(), hours, minutes, seconds));
                }
            }
        });
    }

    private void startAutoSave() {
        Timer saveTimer = new Timer(true);
        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveData();
            }
        }, 0, 5000);
    }
}