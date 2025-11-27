# Real-time Traffic Simulation Platform

# I. Project Overview
A Java-based traffic simulation platform that connects to SUMO using the TraaS Java API. The main goal of the project is to create a Java program with a graphical user interface (GUI) that shows the traffic lights, vehicles, and road network in the simulation. In addition to visuals, the system allows users to engage by adjusting traffic light phases and inserting new cars into the simulation. Additionally, the platform will allow data-export features and compute and show real-time statistics.


II. Key Files Description
- **`ControlPanel.java`** - Main facade class that orchestrates the simulation
- **`VehicleWrapper.java`** - Handles all vehicle-related TraCI commands
- **`TrafficLightWrapper.java`** - Manages traffic light states and operations  
- **`EdgeWrapper.java`** - Handles road network edges and lane information
- **`demo.sumocfg`** - Main SUMO simulation configuration file
- **`*.net.xml`** - Road network definition (nodes, edges, connections)
- **`*.rou.xml`** - Vehicle routes and traffic definitions

III. Requirements

- **Java**: JDK 25 or higher
- **SUMO**: Version 1.18.0 or higher
- **JavaFX**: SDK 21 or higher

IV. Setup and Run
1. **Download TraaS.jar** from the official SUMO repository: https://eclipse.dev/sumo/
2. **Add to IntelliJ Project:**
- Go to **File** → **Project Structure** → **Libraries**
- Click **+** → **Java** → Select the downloaded `traas.jar`
- Apply changes
3. Run Main.java
  
V. Members
1. Duong Xuan Ngan
2. Nguyen Minh Hieu
3. Vo Thanh Tai
4. Huynh Tuan Phat
5. Lam Quang Thien

