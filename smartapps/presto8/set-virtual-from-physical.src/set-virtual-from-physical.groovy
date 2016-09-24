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
 *  Author: Preston Hunt
 */
definition(
    name: "Set Virtual From Physical",
    namespace: "presto8",
    author: "me@prestonhunt.com",
    description: "Set a virtual device state based on the state from one or more physical devices.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    section("Set virtual based on physical") {
        input "virtual", "capability.contactSensor", title: "Virtual sensor", multiple: false
        input "locks", "capability.lock", title: "Physical locks", multiple: true
        input "contacts", "capability.contactSensor", title: "Physical contacts", multiple: true
    }
}

def installed() {
    // we do all init in updated(), but implement an empty installed() to SmartThings won't complain
}

def updated() {
    // called whenever the user changes any preferences, we also use this do to init instead of installed()

    unsubscribe()
    subscribe(contacts, "contact", handler)
    subscribe(locks, "lock", handler)
    
    log.debug "installed and monitoring"
}

def handler(evt) {
    log.trace "contacts check for open"
    
    if (isAnyOpen()) {
        virtual.open()
    } else {
        virtual.close()
    }
}

def isAnyOpen() {
    return anyOpenHelper(contacts, "contact", "open") || anyOpenHelper(locks, "lock", "unlocked")
}

def anyOpenHelper(devices, field, trigger) {
    def open = devices.findAll{ it.currentValue(field) == trigger }
    return open.size() > 0
}
