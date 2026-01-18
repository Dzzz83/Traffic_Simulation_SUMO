# Real-time Traffic Simulation Platform

## I. Project Overview
A Java-based traffic simulation platform that connects to SUMO using the TraaS Java API. The main goal of the project is to create a Java program with a graphical user interface (GUI) that shows the traffic lights, vehicles, and road network in the simulation. In addition to visuals, the system allows users to engage by adjusting traffic light phases and inserting new cars into the simulation. Additionally, the platform allows data-export features and computes real-time statistics.

## II. Key Files Description
- **`ControlPanel.java`** - Main facade class that orchestrates the simulation backend.
- **`Controller.java`** - Handles UI logic, user input, SumoConfig drawing, and animation loops (JavaFX).
- **`ui_design.java` / `ControlPanel.fxml`** - Defines the graphical layout and elements.
- **`VehicleWrapper.java`** - Handles all vehicle-related TraCI commands.
- **`TrafficLightWrapper.java`** - Manages traffic light states and operations.
- **`EdgeWrapper.java`** - Handles road network edges and lane information.
- **`demo.sumocfg`** - Main SUMO simulation configuration file.
- **`*.net.xml`** - Road network definition (nodes, edges, connections).
- **`*.rou.xml`** - Vehicle routes and traffic definitions.

## III. Features (Milestone 2)
- **Interactive Map Visualization**: Real-time rendering of roads, lanes, and vehicles using JavaFX Canvas.
- **Traffic Light Control**:
    - **Manual Mode**: User can toggle lights and adjust phase duration via sliders.
    - **Auto Mode**: Automated traffic light logic.
- **Vehicle Injection**: Spawn individual vehicles into the simulation via the "Add Vehicle" button.
- **Navigation Controls**: Zoom (Scroll) and Pan (Drag) to navigate the SumoConfig.
- **Stress Testing**: A dedicated feature to spawn 500+ vehicles to test system stability.
- **Real-time Statistics**: Dashboard displaying vehicle count, average speed, and congestion density.
- **Restart Capability**: Reset the simulation and SumoConfig to their initial state without restarting the application.

## IV. Requirements
- **Java**: JDK 25 or higher
- **SUMO**: Version 1.18.0 or higher
- **JavaFX**: SDK 21 or higher

## V. Setup and Run
1. **Download TraaS.jar** from the official SUMO repository: https://eclipse.dev/sumo/
2. **Add to IntelliJ Project:**
   - Go to **File** → **Project Structure** → **Libraries**
   - Click **+** → **Java** → Select the downloaded `traas.jar`
   - Apply changes
3. Run `Main.java`

## VI. User Guide Draft
1. **Start Simulation**: Click the `Start` button in the top left to begin the SUMO connection.
2. **Navigation**:
   - **Zoom**: Use the mouse scroll wheel.
   - **Pan**: Click and drag the SumoConfig to move around.
   - **Toggle Views**: Use the sidebar buttons to show/hide Edge IDs, Vehicle IDs, or Route IDs.
3. **Traffic Control**:
   - Select a junction (e.g., "Junction_1") from the dropdown menu.
   - Use the **Sliders** to adjust Red/Green light duration.
   - Toggle **Auto Mode** to switch between manual and automatic control.
4. **Spawning Cars**:
   - Click **Add Vehicle** to spawn a single car on a random route.
   - Click **Stress Test** to spawn 500 cars immediately to test performance.
5. **Restart**: Click the **Restart** button to clear the SumoConfig and reset the simulation.

## VII. Team Members & Roles
1. **Duong Xuan Ngan** - Developer, GUI Designer
   - *Responsibilities:* Traffic Light Logic & Visualization.
2. **Nguyen Minh Hieu** - Lead Developer, Lead UI/UX Designer
   - *Responsibilities:*
        + Map Creation
        + Improve Map & Control Panel Design & Visualization
        + Stress Test 2 
        + Restart Button 
        + Improve Inject Vehicles features
        + Vehicle Types Drawing
3. **Vo Thanh Tai** - Developer, Data Analyst, UI/UX Designer
   - *Responsibilities:*
        + Data-Related Features
        + Tilted Panes
        + Improve Control Panel Design
        + maxSpeed feature
4. **Huynh Tuan Phat** - Developer, UI/UX Designer
   - *Responsibilities:*
        + Improve Map & Control Panel Design & Visualization    
        + Stress Test 1
        + Improve Inject Vehicles features
        + 3D Map Rendering
        + Vehicle Types Drawing"
5. **Lam Quang Thien** - Developer, UI/UX Designer
   - *Responsibilities:*
        + Map & Control Panel Design & Visualization
        + Slide Bars
        + Toggle View IDs features
        +  Logging infos & errors
        +   Inject Vehicles features
        +    Filter Vehicle Types
        +    User Guide
