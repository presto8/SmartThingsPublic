/**
 *  Copyright 2015 SmartThings
 *  Copyright 2016 Preston Hunt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  Author: SmartThings
 *  Author: Preston Hunt
 */
definition(
    name: "Opened It or Left It Open",
    namespace: "presto8",
    author: "me@prestonhunt.com",
    description: "Get a notification when an open/close sensor is opened or if it stays open.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

/*
Theory of operation:

- A handler is called whenever a lock or contact sensor is opened
  - Handler immediately sends out a "device was opened" notification
  - Handler unschedules all tasks
  - Handler schedules the checker to run every 5 minutes

- Checker 
  - Sends "still open" notification for any open device
  - If all devices are closed, unschedules all tasks

- Notification helper
  - Automatically rate limits messages
  - Default is 5 minutes, but is used by the checker to double the time between
    "still open" messages each time
*/

preferences {
	section("Opened It or Left It Open") {
		input "contacts", "capability.contactSensor", title: "Sensor", multiple: true
        input "locks", "capability.lock", title: "Locks", multiple: true
        input "rateLimitMinutes", "number", title: "Don't repeat open/unlock notifications within this many minutes", required: true
		input "exponentialBackoff", "bool", title: "Double time between notifications up to 8 hours", required: true
	}
}

def updated() {
	// don't use installed(), instead handle all init during updated()
	// called whenever the user changes any preferences

    // state.lastMinutes holds the number of minutes of the last notification that was sent.
    // This is used to implement the exponential backoff algorithm for notifications.
    state.lastMinutes = [:]
	state.notifications = [:]
    
	unsubscribe()
	subscribe(contacts, "contact.open", openHandler)
    subscribe(locks, "lock.unlocked", openHandler)
    
    log.trace "installed and monitoring for opened devices"
}

def openHandler(evt) {  
    notify("$evt.displayName was just opened")
    unschedule()
    //checkOpen()
    runEvery5Minutes(checkOpen)
}

def checkOpen() {
	log.trace "checking for open devices"

	def contacts = checkHelper(contacts, "contact", "open")
    def locks = checkHelper(locks, "lock", "unlocked")      
	if (contacts + locks == 0) {
        //log.trace "all sensors closed, not running any more checks"
        notify("all monitored sensors are closed")
        unschedule()
    }
}

// Examines each device. If device is still open, sends notification.
// If device is closed, removes entry from the state.lastMinutes.
// Returns number open devices remaining.
def checkHelper(devices, field, trigger) {
	def numOpen = 0
    devices.each{d ->
    	def open = d.currentValue(field) == trigger
		if (open) {
	        notify("$d is still $trigger", getNotifyMinutes(d))
            numOpen = numOpen + 1
        } else {
        	state.lastMinutes[d.id] = null
        }
    }
    return numOpen
}

def getNotifyMinutes(device) {
	def maxMinutes = 8 * 60
	def nextMinutes = 5 // default

	def lastMinutes = state.lastMinutes[device.id]
    log.trace "lastMinutes for $device is " + lastMinutes
	if (lastMinutes) {
    	nextMinutes = lastMinutes * 2
        if (nextMinutes > maxMinutes) {
        	nextMinutes = maxMinutes
        }
    }

	state.lastMinutes[device.id] = nextMinutes
    log.trace "setting next notification for $device to " + nextMinutes + " minutes"
    return nextMinutes
}

// Sends a notification message. Automatically rate limits messages to not send
// repeated messages within 'rateLimitMinutes'
def notify(msg, quietMinutes=rateLimitMinutes) {
	// First, clear out any expired entries
    state.notifications = state.notifications.findAll{ it.value >= now() }
    
    // Don't send a message if there's an entry in the map
    if (state.notifications[(msg)]) {
        log.trace "not sending because too soon: " + msg
        return
    }

    // If we make it this far, send the message and make a map entry with an expiration time in the future
    sendPush(msg)
    log.trace "notify(): " + msg
    
    state.notifications[(msg)] = now() + quietMinutes * 60 * 1000L
}
