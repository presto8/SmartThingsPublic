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
    name: "Sunrise Actions",
    namespace: "presto8",
    author: "me@prestonhunt.com",
    description: "Take various actions at sunrise each day.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    input "switches", "capability.switch", title: "Which lights to brighten to 100%?", multiple: true
}

def installed() {
}

def updated() {
    // called whenever the user changes any preferences, we also use this do to init instead of installed()

    unsubscribe()
    //subscribe(location, "sunrise", sunriseHandler)
    log.trace "currently disabled"
    
    log.trace "installed and monitoring"
    //setSwitchesLevel(switches, 40)
}

def setSwitchesLevel(switches, level) {
    switches.each{s ->
    	log.trace "processing " + s
       	def prevState = s.currentValue("switch")
        log.trace "setting " + s + " level to " + level + "%"
        log.trace "switch previous setting was " + prevState
        s.setLevel(level)
        if (prevState == "off") {
        	log.trace "switch was previously off, sending off command"
        	//s.off([delay: 5000])
        }
    }
}

def sunriseHandler(evt) {
    setSwitchesLevel(switches, 100)
}