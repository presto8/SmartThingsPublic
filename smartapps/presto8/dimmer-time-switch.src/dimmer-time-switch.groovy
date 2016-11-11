/**
 *  Copyright 2016 Preston Hunt
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
 *  Author: SmartThings
 *  Author: Preston Hunt
 */
definition(
    name: "Dimmer Time Switch",
    namespace: "presto8",
    author: "me@prestonhunt.com",
    description: "Change a switch's dimmer setting by time of day.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    input "switches", "capability.switch", title: "Which lights to brighten to 100%?", multiple: true
    input "sunriseLevel", "number", title: "Set dimmer to this level at sunrise", required: true
    input "sunsetLevel", "number", title: "Set dimmer to this level at sunset", required: true
}

def installed() {
}

def updated() {
    // called whenever the user changes any preferences, we also use this do to init instead of installed()

    state.levels = [:]

    unsubscribe()
    subscribe(location, "sunrise", sunriseHandler)
    subscribe(location, "sunset", sunsetHandler)
    
    log.trace "installed and monitoring"
}

def helper(evt, level) {
    /* If a switch is on, then change its level and set the triggered flag. Else if a switch is off, reset its triggered flag.
     */
    switches.each{s ->
        state.levels[s.id] = level
       	def state = s.currentValue("switch")
        if (state == "on") {
            s.setLevel(level)
        }
    }
}

def sunriseHandler(evt) {
    log.trace "sunriseHandler()"
    helper(evt, sunriseLevel)
}

def sunsetHandler(evt) {
    log.trace "sunsetHandler()"
    helper(evt, sunsetLevel)
}
