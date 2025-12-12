## Solution instructions

As part of this solution, we have created the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Mine site monitoring"</a> dashboard that displays
data from multiple heavy machines. You may use the dashboard to:

* observe the location of excavators and haul trucks;
* monitor zone-related events and alarms;
* browse individual machine movement and fuel level history;

The main state displays the list of machines, their current positions on the map, and recent alarms related to geofencing and operational conditions.
You may browse a machine’s location history popup by clicking the "Location history" icon located on the right side of the machine row.
You may also drill down to the machine details state by clicking on a table row.
The details state allows you to view machine-specific alarms, fuel history, movement history, and geofence interactions.

You may always customize the <a href="${MAIN_DASHBOARD_URL}" target="_blank">"Mine site monitoring"</a> dashboard using dashboard development <a href="${DOCS_BASE_URL}/user-guide/dashboards/" target="_blank">guide</a>.

### Devices

We have already created two excavators, and two haul trucks and loaded demo telemetry for them. See the device info and credentials below:

<div class="tb-markdown-view table-wrapper">

${device_list_and_credentials}

</div>

The solution expects that machinery devices will upload "latitude", "longitude", "speed",
"fuelLevel", and machine-specific telemetry such as "hydraulicPressure" for excavators and "loadWeight"
for haul trucks.

```json
{"latitude":  37.764702, "longitude":  -122.476071}{:copy-code}
```

To emulate data upload on behalf of device "Haul truck A", execute the following command:

```bash
curl -v -X POST -d "{\"latitude\":  37.764702, \"longitude\":  -122.476071}" ${BASE_URL}/api/v1/${Bus CACCESS_TOKEN}/telemetry --header "Content-Type:application/json"{:copy-code}
```

The example above uses <a href="${DOCS_BASE_URL}/reference/http-api/#telemetry-upload-api" target="_blank">HTTP API</a>.
See <a href="${DOCS_BASE_URL}/getting-started-guides/connectivity/" target="_blank">connecting devices</a> for other connectivity options.

### Alarms

Alarms are generated using X <a href="${DOCS_BASE_URL}/user-guide/device-profiles/#alarm-rules"
target="_blank">Alarm rules</a> in in the Excavator and Haul truck device profiles <a href="/profiles/deviceProfiles"
target="_blank">device profile</a>.

The template includes:

* Restricted zone breach — machine enters a flood-risk zone
* Max stay duration — machine remains too long in a loading, unloading, or restricted zone
* Low fuel level
* Machine-specific alarms, such as:
* High hydraulic pressure (excavator)
* Overloaded (haul truck)

### Solution entities

As part of this solution, the following entities were created:

<div class="tb-markdown-view table-wrapper">

${all_entities}

</div>
