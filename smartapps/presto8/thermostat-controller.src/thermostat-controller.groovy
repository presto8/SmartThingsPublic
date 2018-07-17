/*

Presto's Thermostat Controller

Give it a thermostat and one or more motion sensors.
If motion is detected, the thermostat is set to "auto" mode.
A timer is set to automatically turn the thermostat back to "off" after a period of time. Each time motion is detected, the timer is reset.
The temperature is set to a day temperature and a night temperature at the indicated times.

Desired behavior:
- At night, temperature should not go outside specified range
- If nobody is present, should be turned off
- If user overrides temperature set point, preserve user's intent

Test cases:
1. Time after 10pm, no hotter than 76
2.

New operation:

- Don't change mode ever, use temperature set point
- Don't have day and night temperatures; use presence detection to set temperature
- Have summer and winter temperatures
- If inactive, save current temperature and set to awayTemperature
- If active, restore previous temperature

Implementation
- Register callbacks to save last motion time
- Schedule every 15 minutes a check, if motion time has expired at that time, then take appropriate action
*/

definition(
    name: "Presto's Thermostat Controller",
    namespace: "presto8",
    author: "me@prestonhunt.com",
    description: "Control thermostat based on presence sensor",
    category: "Green Living",
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/f/f1/Bip-1398748.svg",
    iconX2Url: "http://icons.iconarchive.com/icons/icons8/windows-8/512/Science-Temperature-icon.png"
)

section {
    input "thermostat", "capability.thermostat", title: "Thermostat", required: true
    input "sensors", "capability.motionSensor", title: "Motion sensors", multiple: true, required: true
    //input "dayHour", "number", title: "Day starts at (hour)"
    //input "nightHour", "number", title: "Night starts at (hour)"
    input "timeoutMinutes", "number", title: "timeout (minutes)"
}

def installed() {}

def updated() {
    state.lastMotion = new Date();

    log.debug "updated(): ${settings}"
    unsubscribe()
    subscribe(sensors, "motion.active", motionActiveHandler)
    //subscribe(thermostat, "temperature", handler)

    runEvery15Minutes(periodic);

    //def hoursTemps = temperatures.split(',')
    //log.debug hoursTemps
    //schedule("0 0 6 * * ?", setDayTemperature)
    //schedule("0 0 10 * * ?", setNightTemperature)
}

def periodic() {
    log.debug "entering periodic"
    def minutes = 30
    if (new Date().getTime() - state.lastMotion.getTime() > minutes*60*1000) {
        log.debug "no motion in 30 minutes, time to set away action"
    }
}

def setTemp(temp) {
    def curtemp = thermostat.currentHeatingSetpoint
    if (curtemp != temp) {
        log.debug "setting temperature setpoint to: $temp (was: $curtemp)"
        thermostat.setHeatingSetpoint(temp)
    } else {
        log.debug "temperature already at desired value"
    }
}

def setMode(mode) {
    def cur = thermostat.currentThermostatMode
    if (cur != mode) {
        log.debug "thermostat currently is $cur, setting to $mode"
        if (mode == 'auto') {
            thermostat.auto()
        } else {
            thermostat.off()
        }
    } else {
        log.debug "thermostat is already in requested mode ($mode), not doing anything"
    }
}

def setDayTemperature() {
    setTemp(68)
}

def setNightTemperature() {
    setTemp(58)
}

def motionActiveHandler(evt) {
    log.debug "motion detected"
    state.lastMotion = new Date()
    //unschedule()
    //setMode('auto')
    //runIn(timeoutMinutes * 60, away)
}

def away() {
    log.debug "away(): no motion detected in $timeoutMinutes minutes, setting thermostat mode to off"
    setMode('off')
}

