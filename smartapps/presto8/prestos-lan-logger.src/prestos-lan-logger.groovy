/**
 *  LAN Event Logger
 *
 *  Copyright 2017 Preston Hunt
 *  Copyright 2015 Brian Keifer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Presto's LAN Logger",
    namespace: "presto8",
    author: "Preston Hunt",
    description: "Log SmartThings events to a LAN server",
    category: "Convenience",
    iconUrl: "http://valinor.net/images/logstash-logo-square.png",
    iconX2Url: "http://valinor.net/images/logstash-logo-square.png",
    iconX3Url: "http://valinor.net/images/logstash-logo-square.png"
)

preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
 	section("Log these switches:") {
    	input "switches", "capability.switch", multiple: true, required: false
    }
 	section("Log these switch levels:") {
    	input "levels", "capability.switchLevel", multiple: true, required: false
    }
	section("Log these motion sensors:") {
    	input "motions", "capability.motionSensor", multiple: true, required: false
    }
	section("Log these temperature sensors:") {
    	input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these humidity sensors:") {
    	input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("Log these contact sensors:") {
    	input "contacts", "capability.contactSensor", multiple: true, required: false
    }
    section("Log these thermostats:") {
    	input "thermostats", "capability.thermostat", multiple: true, required: false
    }
    section("Log these alarms:") {
		input "alarms", "capability.alarm", multiple: true, required: false
	}
    section("Log these indicators:") {
    	input "indicators", "capability.indicator", multiple: true, required: false
    }
    section("Log these CO detectors:") {
    	input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
    }
    section("Log these smoke detectors:") {
    	input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
    }
    section("Log these water detectors:") {
    	input "waterdetectors", "capability.waterSensor", multiple: true, required: false
    }
    section("Log these acceleration sensors:") {
    	input "accelerations", "capability.accelerationSensor", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }

    section ("LAN Event Server") {
        input "host", "text", title: "Hostname/IP"
        input "port", "number", title: "Port"
    }
}

def installed() {}

def updated() {
	log.debug "updated with settings: ${settings}"

	unsubscribe()

	def all_sensors = [alarms, codetectors, contacts, thermostats, indicators, modes,
                       motions, presences, relays, smokedetectors, switches, levels,
                       temperatures, waterdetectors, accelerations, energymeters]
    
  	subscribe_all(all_sensors.flatten() - null, handler)
}

def subscribe_all(sensors, deviceHandler) {
    sensors.each { sen ->
        log.debug "installing $sen"
        sen.capabilities.each { cap ->
            cap.attributes.each { attr ->
                subscribe(sen, attr.name, deviceHandler)
                log.debug "subscribed: $sen ${attr.name}"
            }
        }
    }
}

def httpPostJson(host, port, json) {
  	try {
		def hubAction = new physicalgraph.device.HubAction(
            method: "POST",
            path: "/",
            body: json,
            headers: [
                HOST: "$host:$port", 
                "Content-Type": "application/json"
            ],
        )

        //log.debug hubAction
		sendHubCommand(hubAction)
    } catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

def handler(evt) {
    def json = "{"
    json += "\"date\":\"${evt.date}\","
    json += "\"name\":\"${evt.name}\","
    json += "\"displayName\":\"${evt.displayName}\","
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"value\":\"${evt.value}\","
    json += "\"isStateChange\":\"${evt.isStateChange()}\","
    json += "\"id\":\"${evt.id}\","
    json += "\"description\":\"${evt.description}\","
//    json += "\"descriptionText\":\"${evt.descriptionText}\","
    json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
    json += "\"isoDate\":\"${evt.isoDate}\","
    json += "\"isDigital\":\"${evt.isDigital()}\","
    json += "\"isPhysical\":\"${evt.isPhysical()}\","
    json += "\"location\":\"${evt.location}\","
    json += "\"locationId\":\"${evt.locationId}\","
    json += "\"unit\":\"${evt.unit}\","
    json += "\"source\":\"${evt.source}\","
    json += "\"program\":\"SmartThings\""
    json += "}"
    log.debug("JSON: ${json}")

    //httpPostJson("10.0.0.10", 5000, json)
    httpPostJson(host, port, json)
}
