## Solution instructions

As part of this solution, we have created 2 dashboards that display data from multiple sensors.
The standards for AQI calculations are specific to the region. We use US AQI for simplicity. You may reconfigure the calculations in the Calculated fields.

We will review and describe each solution part below:

#### Public Air Quality Monitoring Dashboard

This dashboard is designed for end-users. It is configured to be "public", meaning the end-user does NOT need to log in to access the dashboard. 
Making the dashboard public is also useful when you plan to embed the page into an external website. You may embed the current dashboard using the code below:

```html
<iframe src="${BASE_URL}${MAIN_DASHBOARD_PUBLIC_URL}" style="position:fixed; inset:0; width:100%; height:100%; border:none;"></iframe>{:copy-code}
```

The dashboard has multiple states:

<div class="img-float" style="max-width:50%;margin: 20px auto;float:right">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-city-state.png" alt="AQI Public Dashboard - City State">
</div>

- **City state** represents the air pollution monitoring of a specific city (in our case Los Angeles) and calculates value based on the maximum AQI received from city sensors. Also, this state contains the following elements:
  - The name of the current city or district;
  - Temperature and humidity;
  - Pollution status according to EPA;
  - Average AQI value based on all sensors in the city and a scale for convenient viewing of pollution;
  - Click on "i" info-icon and a pop-up with the legend of pollutants will appear;
  - A section with general recommendations for sensitive groups of people regarding the current level of pollution;
  - History section of AQI level in the current, weekly and monthly range;
  - Interactive map showing air pollution monitoring stations, the markers of which are color-coded depending on the AQI level;
  - Click on the sensor marker on the map and go to the Sensor state (you can switch to other sensors by clicking on other device markers).

- **Sensor state** represents the current state of the specific sensor. This state contains the following elements:
  - Contains the same elements as the **City state**, but the data is based on the sensor;
  - Section of specific pollutants which include: PM2.5, PM10, NO2, CO, SO2, O3. 
    Click on one of these tiles and a pop-up will appear, which will display a description, general recommendations, as well as its statistics for this pollutant.

<div class="img-float" style="max-width:50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-sensor-state.png" alt="AQI Public Dashboard - Sensor State">
</div>


#### Administration Air Quality Monitoring Dashboard

This dashboard is designed for tenant administrators to perform basic device management tasks, and has multiple states:

-  **Main state** which is intended for monitoring sensors, alarms, etc. The Main state contains:
   - The **Sensors** section. 
    All sensor data is displayed in a table where you can see the following information: “Sensor Label”, “Sensor id”, “Connection”, “Battery level” and “Last AQI”.
    You can also add a new sensor, edit and delete a sensor.
    Click on a specific sensor from the table and go to the **Sensor state**;

   - **Alarms** section.
    All alarm data is displayed in a table where you can see the following information: “Created time”, “Type”, “Sensor id” and “Status”. 
    Click on the “Settings” icon where you can set the alarm rules manually.    
    By default you can configure the values at which alarms will be triggered for such values as Battery Level(in percent) and duration of no connection (in hours);
   - **Interactive map**. After selecting the marker of the sensor on the map, a pop-up will appear with information about it.
    Click on **“Details”** and go to the selected **Sensor state**.

<div class="img-float" style="max-width: 50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-admin-state-1.png" alt="AQI Administration Dashboard - Sensor State">
</div>

- **Sensor state** allows you to view detailed information about the sensor. It contains the following sections:
   - Sensor details that show information about the sensor. Use the **“Edit”** button to edit sensor details;
   - Sensor measures that show next data: “connection” status, “Battery level”, “Last AQI”, “PM2.5”, “PM10”, “NO2”, “CO”, “SO2”, “O3”;
   - Battery level chart;
   - A Map which shows the location of sensor, allows you to move, delete and restore the sensor marker;
   - Sensor alarms table;
   - Connection status chart.

<div class="img-float" style="max-width: 50%;margin: 10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/instruction-admin-state-2.png" alt="AQI Administration Dashboard - Sensor State">
</div>

#### Devices

We have already created five sensors and loaded some demo data for them.

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

**The solution expects that the sensor device will upload all pollution values, temperature, humidity, and battery level. The most simple example of the expected payload is in JSON format:**

```json
{"temperature": 42, "humidity": 73, "pm25": 24.4, "pm10": 30, "no2": 13, "co": 2.8, "so2": 7, "o3": 0.164, "batteryLevel": 77 }{:copy-code}
```

<br>

**To emulate the data upload on behalf of device "Air Quality Sensor 1", one should execute the following command:**

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" ${BASE_URL}/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

#### Alarms
Alarms are generated using <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">Alarm rules</a> configured in the "AQI Sensor" device profile.
The alarm rules configured for AQI sensors are listed below:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

User may turn alarms on and off as well as configure the alarm thresholds via the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Air Quality Monitoring"</a> dashboard using "Edit Sensor" form.

#### Calculated fields

Calculated Fields are used to compute AQI values and derived indicators based on incoming telemetry.

<div class="tb-markdown-view table-wrapper">

${calculated_fields}

</div>

#### Entity Groups

Solution has:
- Asset Group "AQI city" to store all cities that belong to this tenant;
- Device Group "AQI Sensor" to store all devices that belong to city.

### Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

#### Examples

##### How to trigger low battery alarm on Air Quality Sensor 1 (Hollywood)

Let's reproduce the event in which we will configure an alarm that will respond to the specified limit value of the battery level.

Let's take for example the sensor Air Quality Sensor 1 (Hollywood), which currently has a battery level of 43.73%.

<br>
<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-1.png" alt="">
</div>

<br>

In order to adjust the **Battery Level** alarm parameters, click on the “settings” button in the “Alarms” section, after which a pop-up will appear for setting the limit values for alarms.

<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-2.png" alt="">
</div>

<br>

Set Alarm rules to 10% and save by pressing the "Save" button.

<br>

<div class="img-float" style="max-width: fit-content;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-3.png" alt="">
</div>

<br>

Then to emulate the “batteryLevel” data of device "Air Quality Sensor 1" let’s take value - ”9” for example, and then we should execute the following command:


```bash
curl -v -X POST -d "{\"batteryLevel\": 9 }" ${BASE_URL}/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

<br>

Now we can see that the Battery Level of the Hollywood sensor is 9%, this level is below the limit of 10% that we indicated earlier - the alarm has been triggered.


<div class="img-float" style="max-width: 60%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-1-4.png" alt="">
</div>


In this way, we can manually set the threshold for triggering alarms to control the battery level of the sensor, and control through the Administration Dashboard.

##### Report high AQI on Air Quality Sensor 2 (Downtown) using PM2.5


In this example, we will  simulate the sending of a high-level pollutant PM 2.5 by a sensor.


The starting value of PM 2.5 in AQI equivalent is 52, you can see it in the picture below.


<div class="img-float" style="max-width: 60%;margin:10px auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-2-1.png" alt="">
</div>


As an example, let's take the **hazardous** level of PM 2.5 that the sensor can potentially send and it will be equal to **400 μg/m3**.
Then to emulate the “pm25” data of device "Air Quality Sensor 2" we should execute the following command:


```bash
curl -v -X POST -d "{\"pm25\": 400 }" ${BASE_URL}/api/v1/${Air Quality Sensor 2ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```


<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-2-2.png" alt="">
</div>
<div class="img-float" style="max-width: 50%;margin:auto">
<img src="https://img.thingsboard.io/solutions/air_quality_index/use-case-sensor-2-3.png" alt="">
</div>


After the data has been sent, you can see that the dashboard received the value of **PM 2.5 - 400 μg/m3** and calculated it as **AQI - 433**, which is a **hazardous** level of pollution.

### Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

As example, in the context of Air Quality Monitoring solution, edge computing could be useful if you have cities that are scattered throughout the country.
In this case, ThingsBoard Edge can be deployed into every city (remote location) to process data from sensors and other devices, calculation of AQI, enabling real-time analysis and decision-making, such as generating alarms in case pollution level is violated. 
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally. 
Eventually, required data is going to be pushed to the cloud, once network connection is established. 
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Location R1EDGE_DETAILS_URL}" target="_blank">Remote Location R1</a> was created.

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"AQI City"** *ASSET* group;
* **"AQI Sensor"** *DEVICE* group;
* **"Air Quality Monitoring"** *DASHBOARD* group.
* **"Air Quality Monitoring Public"** *DASHBOARD* group.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Location R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant credentials.

#### Push data to device on edge

**"AQI Sensor"** *DEVICE* group was assigned to the edge entity "Remote Location R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Entities -> Devices** page.

To emulate the data upload on behalf of device "Air Quality Sensor 1" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" http://localhost:8080/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"temperature\":  42, \"humidity\":  73, \"pm25\":  24.4, \"pm10\":  30, \"no2\":  13, \"co\":  2.8, \"so2\":  7, \"o3\":  0.164, \"batteryLevel\":  77 }" http://localhost:18080/api/v1/${Air Quality Sensor 1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "Air Quality Sensor 1" on edge, you'll be able to see telemetry update on the cloud for this device as well.
