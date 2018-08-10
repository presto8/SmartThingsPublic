/*
Presto's Thermostat Controller

Give it a thermostat and some motion sensors and light switches.
If motion is not detected after a period of time, the thermostat's set points are set to "away" values.
If lights are on, somebody is assumed home.
The operating state (cool/heat/auto/off) is never touched.

Desired behavior:
- At night, temperature should not go outside specified range; note, there won't be any motion because everyone is in bed.
- If no motion is detected for 16 hours, then go into vacation mode.
- As soon as motion is detected, exit vacation mode.
- TODO: Figure out pet detection.
- Automatically save the user's set points for temporary overrides.

Test cases:
1. Time after 10pm, no hotter than 76
2.

New operation:

- Don't have day and night temperatures; use presence detection to set temperature
- Have summer and winter temperatures
- If inactive, save current temperature and set to awayTemperature
- If active, restore previous temperature

Implementation
- Register callbacks to save last motion time
- Schedule every 15 minutes a check, if motion time has expired at that time, then take appropriate action
- If the user ever changes the temperature, set that as the new "present" temperature
- "
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
    input "switches", "capability.switch", title: "Switches", multiple: true, required: true
    //input "dayHour", "number", title: "Day starts at (hour)"
    //input "nightHour", "number", title: "Night starts at (hour)"
    input "timeoutMinutes", "number", title: "timeout (minutes)"
	input "heatingSetPoint", "number", title: "Heating set point (F)", defaultValue: 68
  	input "coolingSetPoint", "number", title: "Cooling set point (F)", defaultValue: 76
}

def installed() {
}

def updated() {
    state.lastMotion = new Date().getTime()
    state.present = false
    state.mode = 'Unknown' // same as location.mode, e.g. Home, Night, Away
    saveTemps()
    
    log.debug "updated(): ${settings}"
    unsubscribe()
    subscribe(sensors, "motion.active", motionActiveHandler)
    subscribe(switches, "switch", switchHandler)
    subscribe(thermostat, "temperature", temperatureHandler)
    
    // for dev, use every 1 minute, for production, every 15
    runEvery1Minute(periodic)
    //runEvery15Minutes(periodic)
}

def periodic() {
    log.debug "entering periodic, location.mode=${location.mode} state.mode=${state.mode} present=${state.present}"
    
    // Change mode and heating & cooling setpoints
    if (location.mode != state.mode) {
        setMode(location.mode)
    }
     
    if (state.mode == 'Home' && state.present) {
        // During awake time, take away action after timeoutMinutes
        def timeLeft = timeoutMinutes*60*1000 - (new Date().getTime() - state.lastMotion)
        log.debug "time left: " + timeLeft
        if (timeLeft <= 0) {
            log.debug "no motion in 30 minutes, time to set away action"
            absent()
        }
    }
}

def motionActiveHandler(evt) {
    log.debug "motion detected: ${evt.displayName}"
    state.lastMotion = new Date().getTime()
    present()
}

def switchHandler(evt) {
    log.debug "switch changed: $evt"
}

def temperatureHandler(evt) {
    log.debug "temperature change: $evt"
}

def setHeatCool(temp) {
    if (thermostat.currentHeatingSetpoint != temp['heat']) {
        log.debug "setting heating setpoint to: ${temp.heat}"
        thermostat.setHeatingSetpoint(temp['heat'])
    }
    
    if (thermostat.currentCoolingSetpoint != temp['cool']) {
        log.debug "setting cooling setpoint to: ${temp.cool}"
        thermostat.setCoolingSetpoint(temp['cool'])
    }
}

def setMode(newMode) {
    def map = [
    	'Away':  [heat: 58, cool: 88],
        'Night': [heat: 64, cool: 76],
        'Home':  [heat: 68, cool: 76]
    ]
    
    if (state.mode != newMode) {
        log.debug "changing mode from ${state.mode} to $newMode"
        setHeatCool(map[newMode])
        state.mode = newMode
    }
}

def absent() {
    if (state.mode == 'Home' && state.present) {
		log.debug "setting present=false"
		saveTemps()
		setHeatCool([heat: 58, cool: 88])
        state.present = false
    }
}

def present() {
    if (state.mode == 'Home' && !state.present) {
        log.debug "setting present=true"
        setHeatCool(state.prevTemp)
        state.present = true
    }
}

def saveTemps() {
    state.prevTemp = [heat: thermostat.currentHeatingSetpoint,
    	              cool: thermostat.currentCoolingSetpoint]
}
