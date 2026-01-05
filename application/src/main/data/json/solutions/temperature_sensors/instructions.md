## Solution instructions

Welcome to your new **Temperature & Humidity** monitoring solution 👋 We have generated the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Temperature & Humidity"</a> dashboard that displays data from multiple sensors. You may use the dashboard to:

* 📍 **Add** and locate sensors on the map;
* 🔔 **Configure** alarm thresholds;
* 📈 **Browse** historical data;
* ⚙️ **Manage** sensor settings.

**Mastering the Dashboard** 🖥️

The dashboard has two states. The **Main** state displays the list of sensors and their map location. 
Click on any row to drill down to the **Sensor Details** state to see history and change settings.
You can always customize this dashboard using our <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">dashboard development guide</a>.

### Devices

We have pre-provisioned two demo sensors for you. You can find their info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

#### ⚡ Test it now

Want to see the dashboard come alive? You can simulate a real device right now.
The solution expects the device to upload `temperature` and `humidity` values in JSON format:


```json
{"temperature":  42, "humidity":  73}{:copy-code}
```

To emulate the data upload on behalf of device "Sensor T1", one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\": 42, \"humidity\": 73}" ${BASE_URL}/api/v1/${Sensor T1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

Go check your dashboard — you should see the values update instantly 🚀

### 🚨 Alarms 

Your solution monitors data based on the <a href="${DOCS_BASE_URL}/user-guide/alarm-rules/" target="_blank">Alarm rules</a> 
configured in the "Temperature Sensor" device profile:

<div class="tb-markdown-view table-wrapper">

${alarm_rules}

</div>

**💡 Tip:** You can enable/disable alarms and configure their thresholds anytime directly from the <a href="${MAIN_DASHBOARD_URL}" target="_blank">dashboard</a> using the "Edit Sensor" button.

### 🔐 Managing Users & Access

We created a sample customer, "Customer D", to demonstrate how you can isolate data for different clients. "Sensor C1" has been explicitly assigned to this customer.

The following users belong to "Customer D" and have read-only access to the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Temperature & Humidity"</a> dashboard. When they log in, they will only see "Sensor C1".

<div class="tb-markdown-view table-wrapper">

${user_list}

</div>

**💡 Note:** You can create more Customers and Users via the administration UI. You can also change the owner of other sensors (like Sensor T1) to "Customer D" to make them visible to these users.

### 📦 Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>

### 📡 Edge computing

**Optionally**, this solution can be extended to use edge computing.

<a href="https://thingsboard.io/products/thingsboard-edge/" target="_blank">ThingsBoard Edge</a> allows bringing data analysis and management to the edge, where the data created.
At the same time ThingsBoard Edge seamlessly synchronizing with the ThingsBoard cloud according to your business needs.

As example, in the context of Temperature & Humidity Sensors solution, edge computing could be useful if you have remote facilities that are located in different parts of town, country or worldwide.
In this case, ThingsBoard Edge can be deployed into every remote facility to process data from temperature and humidity sensors, enabling real-time analysis and decision-making, such as turning on/off heater or adjusting temperature automatically. 
Edge is going to process data in case there is no network connection to the central ThingsBoard server, and thus no data will be lost and required decisions are going to be taken locally. 
Eventually, required data is going to be pushed to the cloud, once network connection is established. 
Configuration of edge computing business logic is centralized in a single place - ThingsBoard server.

In the scope of this solution, new edge entity <a href="${Remote Facility R1EDGE_DETAILS_URL}" target="_blank">Remote Facility R1</a> was added to a customer "Customer D".

Additionally, particular entity groups were already assigned to the edge entity to simplify the edge deployment:

* **"Customer Administrators"** *USER* group of customer "Customer D";
* **"Temperature & Humidity sensors"** *DEVICE* group of customer "Customer D";
* **"Customer dashboards"** *DASHBOARD* group of your tenant.

To install ThingsBoard Edge and connect to the cloud, please navigate to <a href="${Remote Facility R1EDGE_DETAILS_URL}" target="_blank">edge details page</a> and click **Install & Connect instructions** button.

Once the edge is installed and connected to the cloud, you will be able to log in into edge using your tenant or users of customer "Customer D" credentials.

#### 🔄 Push data to device on edge

**"Temperature & Humidity sensors"** *DEVICE* group of customer "Customer D" was assigned to the edge entity "Remote Facility R1".
This means that all devices from this group will be automatically provisioned to the edge.

You can see devices from this group once you log in into edge and navigate to the **Entities -> Devices** page.

To emulate the data upload on behalf of device "Sensor C1" to the edge, one should execute the following command:

```bash
curl -v -X POST -d "{\"temperature\":  43, \"humidity\":  74}" http://localhost:8080/api/v1/${Sensor C1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Or please use next command if you updated edge HTTP 8080 bind port to **18080** during edge installation:

```bash
curl -v -X POST -d "{\"temperature\":  43, \"humidity\":  74}" http://localhost:18080/api/v1/${Sensor C1ACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

Once you'll push data to the device "Sensor C1" on edge, you'll be able to see telemetry update on the cloud for this device as well.
