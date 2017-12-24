/*

Presto's Thermostat Controller

Give it a thermostat and one or more motion sensors.
If motion is detected, the thermostat is set to a "present temperature".
A timer is set to automatically turn the thermostat back to a "not present temperature"
after a period of time. Each time motion is detected, the timer is reset.

*/

definition(
    name: "Presto's Thermostat Controller",
    namespace: "presto8",
    author: "me@prestonhunt.com",
    description: "Control thermostat based on presence sensor",
    category: "Green Living",
//    iconUrl: "http://icons.iconarchive.com/icons/icons8/windows-8/512/Science-Temperature-icon.png",
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/f/f1/Bip-1398748.svg",
    iconX2Url: "http://icons.iconarchive.com/icons/icons8/windows-8/512/Science-Temperature-icon.png"
)

section {
    input "thermostat", "capability.thermostat", title: "Thermostat", required: true
    input "sensors", "capability.motionSensor", title: "Motion sensors", multiple: true, required: true
    input "presentTemperature", "decimal", title: "present temperature"
    input "awayTemperature", "decimal", title: "away temperature"
    input "timeoutMinutes", "number", title: "timeout (minutes)"
}

def installed() {}

def updated() {
    log.debug "updated(): ${settings}"
    unsubscribe()
    subscribe(sensors, "motion.active", handler)
    //subscribe(thermostat, "temperature", handler)
    log.debug "current temp=${thermostat.currentHeatingSetpoint}"
}

def set_temp(temp) {
    def curtemp = thermostat.currentHeatingSetpoint
    if (curtemp != temp) {
        log.debug "setting temperature setpoint to: $temp (was: $curtemp)"
        thermostat.setHeatingSetpoint(temp)
    } else {
        log.debug "temperature already at desired value"
    }
}

def handler(evt) {
    log.debug "handler(): setting temperature to present temperature"
    unschedule()
    set_temp(presentTemperature)
    runIn(timeoutMinutes * 60, away)
}

def away() {
    log.debug "away(): setting temperature to away temperature"
    set_temp(awayTemperature)
}
