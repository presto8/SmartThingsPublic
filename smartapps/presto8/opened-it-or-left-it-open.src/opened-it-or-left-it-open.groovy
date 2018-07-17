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

The desired end user behavior is:
- Limit the number of notifications to the absolute minimum; have a user
  setting "notification time period" and combine multiple messages during this
  period into a single notification message.
- If a sensor is opened and then closed within a short "cooling off" time
  period, only send one notification (that the sensor was opened and then
  closed)
- If a sensor is left open for a long time, keep sending notifications but
  slowly increase the time between notifications.

- A handler is called whenever a lock or contact sensor is opened
  - Handler immediately sends out a "device was opened" notification
  - Handler schedules the checker to run every 30 seconds. This is theo nly
    place the schedule is started.

- Checker 
  - Sends "still open" notification for any open device
  - If all devices are closed, unschedules all tasks. This is the only place
    the schedule is stopped.

- Notification helper
  - Automatically rate limits messages
  - Default is 5 minutes, but is used by the checker to double the time between
    "still open" messages each time
*/

preferences {
	section("Opened It or Left It Open") {
		input "contacts", "capability.contactSensor", title: "Sensor", multiple: true
        input "locks", "capability.lock", title: "Locks", multiple: true
        input "coolingOffPeriod", "number", title: "Only send one notification if sensor is opened then closed within this time (in seconds)", required: true
        input "rateLimitMinutes", "number", title: "Don't repeat open/unlock notifications within this many minutes", required: true
		input "exponentialBackoff", "bool", title: "Double time between notifications up to 8 hours", required: true
        input "musicPlayer", "capability.musicPlayer", title: "Announce with these text-to-speech devices (musicPlayer)", multiple: true, required: false
	}
}

def updated() {
	// don't use installed(), instead handle all init during updated()
	// called whenever the user changes any preferences

    // state.lastMinutes holds the number of minutes of the last notification that was sent.
    // This is used to implement the exponential backoff algorithm for notifications.
    state.lastMinutes = [:]
	state.notifications = [:]
	state.notificationCount = [:]
    state.running = false

    state.openDevices = [:]
    
	unsubscribe()
	subscribe(contacts, "contact.open", openHandler)
    subscribe(locks, "lock.unlocked", openHandler)
	//subscribe(contacts, "contact.closed", closedHandler)
    //subscribe(locks, "lock.locked", closedHandler)
    
    log.trace "installed and monitoring for opened devices"
    if (musicPlayer) {
    	musicPlayer*.playText("installed")
    }

}

// openHandler() only does two things: updates openDevices with the time the
// device was opened the device and starts the scheduler if it isn't running.
// To prevent race conditions, only openHandler is allowed to change
// openDevices.
def openHandler(evt) {
    def dev = evt.getDevice()
    if (state.openDevices[dev.id] != null) {
        log.trace "${dev.id}/${dev.name} is already open"
    } else {
        log.trace "${dev.id}/${dev.name} was opened, setting openDevices"
        state.openDevices[dev.id] = now()
    }

    if (!state.running) {
        runIn(30, checkOpen)
        state.running = true
    }
}

/*
def closedHandler(evt) {
    def dev = evt.getDevice()
    if (state.openDevices[dev.id]) {
        log.trace "${dev.id} was open but is not closed"
        state.openDevices[dev.id] = null
    } else {
        log.trace "${dev.id} was marked as closed already but received a closed event; that shouldn't have happened"
    }
}
*/

def checkOpen() {
	log.trace "checking for open devices"

    state.openDevices.each{d, t ->
        log.trace "examining device ${d.id}/${d.name}, opened at time $t"

    }

    def numStillOpen = state.openDevices.size()
    if (numStillOpen > 0) {
        log.trace "$numStillOpen devices still open; running again in 30 seconds"
        runIn(30, checkOpen)
    } else {
        log.trace "all devices closed; not checking any more"
        state.running = false
    }

/*
	def contacts = checkHelper(contacts, "contact", "open")
    def locks = checkHelper(locks, "lock", "unlocked")      
	if (contacts + locks == 0) {
        //log.trace "all sensors closed, not running any more checks"
        notify("All monitored sensors are closed")
        unschedule()
    }
*/
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
    //sendPush(msg)
    sendSmsMessage("5038079580", msg)
    log.trace "notify(): " + msg
    
    if (musicPlayer) {
    	musicPlayer*.playText(msg)
    }
    
    state.notifications[(msg)] = now() + quietMinutes * 60 * 1000L
}
