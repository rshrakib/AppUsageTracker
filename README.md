Project Name
USER ACTIVITY TRACKER
Project Overview
The Desktop App Usage Tracker is a full-featured, lightweight desktop application built using JavaFX and JNA (Java Native Access), designed to monitor and track active window application usage on a Windows operating system in real-time. The application records which software is being used, for how long and stores this data locally in a CSV file for long-term analysis.
Additionally, it visualizes usage data through interactive Pie Charts, provides last 7 days' usage history and supports a Light/Dark theme switcher for an improved user experience.
This application is primarily intended for:
•	Students, professionals or freelancers who wish to analyze and optimize their digital productivity.
•	Developers looking for a lightweight alternative to paid time-tracking or productivity applications.
Core Purpose
1.	Productivity Tracking: Identify which apps consume most of your time.
2.	Real-time Monitoring: See live updates of foreground applications.
3.	Usage Analytics: Store historical data and visualize it to find patterns.
4.	Offline & Private: No cloud dependency, using local CSV storage.
5.	Clean & Simple UI: Smooth, minimalistic desktop interface using JavaFX.
Technologies Used
Category	Tools/Tech
Programming Language	Java 17+
UI Framework	JavaFX 20+ (FXML, CSS)
Native Access Library	JNA (Java Native Access)
Data Storage	CSV File
Build Tool	Maven
Graphing Component	JavaFX PieChart
Scheduler	Java Timer & TimerTask


External Dependencies
Maven Dependency:
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.13.0</version>
</dependency>
JavaFX SDK:
Must be downloaded and linked with IDE or configured in pom.xml or module settings.
Project Structure
DesktopAppUsageTracker/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── useractivitytracker/
│       │           └── finalproject/
│       │               ├── AppUsageTracker.java
│       │               └── module-info.java
│       └── resources/
│           └── com/
│               └── useractivitytracker/
│                   └── finalproject/
│                       └── styles.css
├── target/
├── .gitignore
├── app_usage.csv
├── jna-jpms-5.16.0.jar
├── mvnw
├── mvnw.cmd
└── pom.xml

Key Features
•	Real-Time Tracking: Monitors the active application every second, recording usage time with millisecond precision. 
•	Data Visualization: 
	Pie Chart: Displays the top three applications by usage time for the current day or the past seven days.
	List View: Shows detailed usage times per application, with daily totals in history mode.
•	Current Application Display: A label shows the currently active application and its total usage time for the day. 
•	History View: Toggle between today’s usage and aggregated data for the last seven days. 
•	Theme Support: Switch between light and dark modes with CSS-based styling. 
•	Data Persistence: Saves usage data to a CSV file every five seconds and on application close. 
•	Self-Exclusion: Excludes its own process (java.exe) from tracking to avoid skewing data. 
•	Consistent Colors: Assigns fixed colors to applications in the pie chart for visual consistency. 
•	Usage Limits: Supports setting time limits for applications (console-based alerts only).
How It Works
Tracking Active Windows
•	Mechanism: 
o	Uses JNA to call Windows APIs: 
	User32.GetForegroundWindow(): Gets the active window handle.
	User32.GetWindowThreadProcessId(): Retrieves the process ID.
	Kernel32.OpenProcess(): Opens a process handle.
	Psapi.GetModuleFileNameExW(): Gets the executable path.
o	Formats the executable name (e.g., notepad.exe → Notepad).
o	Excludes java to avoid self-tracking.
•	Frequency: Every 1 second via a Timer.
•	Accuracy: Millisecond precision for usage time.
Data Persistence
•	Format: CSV (date,appName,usageTime,category).
•	Storage: 
o	In-memory: Map<String, Map<String, Long>>.
o	On-disk: app_usage.csv.
•	Operations: 
o	Load: Reads CSV on startup, populates data structures.
o	Save: Writes to CSV every 5 seconds and on exit.
o	Auto-Save: Ensures data safety using a Timer.
UI Rendering
•	Framework: JavaFX.
•	Components: 
o	PieChart: Top 3 apps, with consistent colors.
o	ListView: Detailed usage, scrollable via ScrollPane.
o	Label: Real-time current app display.
o	Button: Mode switching and theme toggling.
•	Thread Safety: Uses Platform.runLater() for UI updates.

Theme Management
•	Implementation: CSS-based (styles.css).
•	Modes: Light (light-mode) and dark (dark-mode).
•	Toggle: Updates root style class dynamically.
How Users Can Install the Application
Desktop App Usage Tracker monitors how long you use applications on your Windows PC, showing results in a pie chart and list view. It saves data to a CSV file and offers light/dark themes.
Requirements
•	OS: Windows 10/11
•	Java: JRE 17+
•	Disk Space: ~10 MB
Installation Guide
1.	Install Java 17+ SDK, Maven and JavaFX SDK.
2.	Clone the repository:
git clone https://github.com/rshrakib/AppUsageTracker.git
cd AppUsageTracker
3.	Install dependencies:
mvn clean install
4.	Run the application:
mvn javafx:run
5.	Use the Features: 
o	Current App: See the active app and its usage time (e.g., Notepad (0h 5m 30s)).
o	Pie Chart: View top 3 apps for today.
o	List View: Check all apps used today.
o	History: Click View Last 7 Days History to see past usage.
o	Back: Click Back to Today to return.
o	Theme: Click Switch to Dark/Light Mode to change themes.
o	Close: Exit to save data to app_usage.csv.
6.	View Data: 
o	Open app_usage.csv in Excel or a text editor to see usage (e.g., 2025-04-13,Notepad,330000,Text Editor).

Troubleshooting
•	Won’t Start? Ensure Java 17+ and JavaFX are installed. Check console for errors.
•	No Data? Use apps for a few seconds; ensure the app isn’t minimized.
•	Missing CSV? Check writes permissions in the app’s folder.

Notes
•	Privacy: Data stays local in app_usage.csv.
•	Limits: Usage limit alerts are console-only.
•	Support: Check the project repository or contact the developer.
Learning Outcomes
•	Real-time data tracking and visualization
•	Working with native system APIs in Java (via JNA)
•	CSV file manipulation and report generation
•	JavaFX styling and theme implementation
•	Java Timer, multi-threading and UI synchronization
Motivation for Building the Project
The motivation stemmed from recognizing the lack of simple, lightweight, privacy-respecting desktop app usage trackers for Windows users. Many solutions are heavy, expensive or cloud-dependent. This project offers a clean, local, open-source alternative that runs on your machine, stores data locally and visualizes it meaningfully with a modern UI.
