## Solution instructions

Welcome to your new **Swimming Pool SCADA System** solution 👋
This template provides a comprehensive monitoring and control environment tailored for swimming pool facilities, utilizing Modbus communication and IoT Gateway integration.

### 🐳 Step 1: Install Docker Compose 

Follow the instructions in the official [Docker Compose installation guide](https://docs.docker.com/compose/install/) to install Docker Compose on your system.

### 🏊 Step 2: Launch the Modbus Pool Emulator

To simulate a comprehensive swimming pool system, this Docker command launches a Modbus pool emulator containing 14 separate devices that function as a unified system and communicate via Modbus. 
Execute the following command in your terminal: 

```bash
docker run --pull always --rm -d --name tb-modbus-pool-emulator -p 5021-5034:5021-5034 thingsboard/tb-modbus-pool-emulator:1.0-stable && docker logs -f tb-modbus-pool-emulator{:copy-code}
```

### 🚀 Step 3: Launch the IoT Gateway

Create a `docker-compose.yml` file with the necessary configurations:

```bash 
${DOCKER_CONFIG}
{:copy-code}
```

Use Docker Compose to pull and run the IoT Gateway:

```bash
docker compose up{:copy-code}
```

### Interacting with the Swimming Pool SCADA System

<br>

The <a href="${MAIN_DASHBOARD_URL}" target="_blank">Swimming Pool SCADA system</a> dashboard is designed to visualize and interact with the data from multiple pool components.

**1. Real-time Monitoring**

View sensor data and their real-time states to ensure optimal operation.

* **Water Quality:** Monitor water levels and pH filtration segments.
* **Temperature:** Track outdoor and pool temperatures to maintain comfort levels.

**2. Historical Analysis**

Analyze historical data to identify trends and optimize energy usage.

* **Energy Management:** Review power consumption history for heat pumps and filtration systems.
* **Temperature Trends:** Correlate outdoor weather with pool temperature changes.

**3. Remote Control**

Control system components remotely directly from the dashboard.

* **Actuation:** Control valves, heating systems, and motor pumps.
* **Operational Modes:** Switch between different filtration and heating modes.

**💡 Tip:** For further customization of the dashboard, refer to the <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### 🔌 Devices

The solution automatically configures the IoT gateway and creates an asset and 14 devices representing the physical components of the pool.

* **Sensors:** Includes Water Level, Temperature (Outdoor/Pool), Flow Rate, Vibration, and Pressure sensors.
* **Actuators:** Includes Valves (Intake/Drain/Weir) and Switches for managing filtration and heat pump states.

#### 📡 Connectivity

This solution utilizes the IoT Gateway and Modbus protocol to communicate with the physical devices. 
For real-time monitoring of device data received from Modbus servers, you can access the <a href="${GATEWAYS_URL}" target="_blank">Gateways</a> page to view the status and data of connected devices.

### 🚨 Alarms

Alarms are configured to monitor equipment health and operational thresholds. For example, the solution tracks vibration, rotation speed, and power consumption to detect potential failures in pumps and filters.

Below is the complete list of <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">alarm rules</a> configured for this solution:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

### 🧮 Calculated fields

Solution utilizes <a href="${DOCS_BASE_URL}/user-guide/calculated-fields/" target="_blank">calculated fields</a> to derive the real-time flow status of different pipe segments based on valve positions and pump states.

**Flow Logic:** Scripts like Filter Segment Flowing or Heat Segment Flowing determine if water is currently moving through specific sections of the infrastructure.

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>
