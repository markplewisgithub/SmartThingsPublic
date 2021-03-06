/**
 *  Copyright 2015 SmartThings
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
 *	Ecobee Thermostat
 *
 *	Author: SmartThings
 *	Date: 2013-06-13
 */
metadata {
	definition (name: "Ecobee Thermostat", namespace: "smartthings", author: "SmartThings") {
		capability "Actuator"
		capability "Thermostat"
		capability "Temperature Measurement"
		capability "Sensor"
		capability "Refresh"
		capability "Relative Humidity Measurement"
		capability "Health Check"

		command "generateEvent"
		command "raiseSetpoint"
		command "lowerSetpoint"
		command "resumeProgram"
		command "switchMode"
		command "switchFanMode"

		attribute "displayThermostatSetpoint", "string" // Added to be able to show "Auto"/"Off" keeping attribute thermostatSetpoint a number
		attribute "thermostatStatus", "string"
		attribute "maxHeatingSetpoint", "number"
		attribute "minHeatingSetpoint", "number"
		attribute "maxCoolingSetpoint", "number"
		attribute "minCoolingSetpoint", "number"
		attribute "deviceTemperatureUnit", "string"
		attribute "deviceAlive", "enum", ["true", "false"]
	}

	tiles {
		standardTile("temperature", "device.temperature", width: 2, height: 2, decoration: "flat") {
			state("temperature", label:'${currentValue}°', unit:"F", icon: "st.thermostat.ac.air-conditioning",
					backgroundColors:[
							// Celsius
							[value: 0, color: "#153591"],
							[value: 7, color: "#1e9cbb"],
							[value: 15, color: "#90d2a7"],
							[value: 23, color: "#44b621"],
							[value: 28, color: "#f1d801"],
							[value: 35, color: "#d04e00"],
							[value: 37, color: "#bc2323"],
							// Fahrenheit
							[value: 40, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"]
					]
			)
		}
		standardTile("mode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
			state "off", action:"switchMode", nextState: "updating", icon: "st.thermostat.heating-cooling-off"
			state "heat", action:"switchMode",  nextState: "updating", icon: "st.thermostat.heat"
			state "cool", action:"switchMode",  nextState: "updating", icon: "st.thermostat.cool"
			state "auto", action:"switchMode",  nextState: "updating", icon: "st.thermostat.auto"
			state "auxheatonly", action:"switchMode", icon: "st.thermostat.emergency-heat"
			state "updating", label:"Working", icon: "st.secondary.secondary"
		}
		standardTile("fanMode", "device.thermostatFanMode", inactiveLabel: false, decoration: "flat") {
			state "auto", action:"switchFanMode", nextState: "updating", icon: "st.thermostat.fan-auto"
			state "on", action:"switchFanMode", nextState: "updating", icon: "st.thermostat.fan-on"
			state "updating", label:"Working", icon: "st.secondary.secondary"
		}
		standardTile("upButtonControl", "device.thermostatSetpoint", inactiveLabel: false, decoration: "flat") {
			state "setpoint", action:"raiseSetpoint", icon:"st.thermostat.thermostat-up"
		}
		valueTile("displayThermostatSetpoint", "device.displayThermostatSetpoint", width: 1, height: 1, decoration: "flat") {
			state "displayThermostatSetpoint", label:'${currentValue}'
		}
		valueTile("currentStatus", "device.thermostatStatus", height: 1, width: 2, decoration: "flat") {
			state "thermostatStatus", label:'${currentValue}', backgroundColor:"#ffffff"
		}
		standardTile("downButtonControl", "device.thermostatSetpoint", inactiveLabel: false, decoration: "flat") {
			state "setpoint", action:"lowerSetpoint", icon:"st.thermostat.thermostat-down"
		}
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "setHeatingSetpoint", action:"thermostat.setHeatingSetpoint", backgroundColor:"#d04e00"
		}
		valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false, decoration: "flat") {
			state "heat", label:'${currentValue}° heat', unit:"F"
		}
		controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "setCoolingSetpoint", action:"thermostat.setCoolingSetpoint", backgroundColor: "#1e9cbb"
		}
		valueTile("coolingSetpoint", "device.coolingSetpoint", inactiveLabel: false, decoration: "flat") {
			state "cool", label:'${currentValue}° cool', unit:"F", backgroundColor:"#ffffff"
		}
		standardTile("refresh", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("resumeProgram", "device.resumeProgram", inactiveLabel: false, decoration: "flat") {
			state "resume", action:"resumeProgram", nextState: "updating", label:'Resume', icon:"st.samsung.da.oven_ic_send"
			state "updating", label:"Working", icon: "st.secondary.secondary"
		}
		valueTile("humidity", "device.humidity", decoration: "flat") {
			state "humidity", label:'${currentValue}%'
		}
		main "temperature"
		details(["temperature", "upButtonControl", "displayThermostatSetpoint", "currentStatus", "downButtonControl", "mode", "fanMode","humidity", "resumeProgram", "refresh"])
	}

	preferences {
		input "holdType", "enum", title: "Hold Type", description: "When changing temperature, use Temporary (Until next transition) or Permanent hold (default)", required: false, options:["Temporary", "Permanent"]
	}

}

void installed() {
    // The device refreshes every 5 minutes by default so if we miss 2 refreshes we can consider it offline
    // Using 12 minutes because in testing, device health team found that there could be "jitter"
    sendEvent(name: "checkInterval", value: 60 * 12, data: [protocol: "cloud"], displayed: false)
}

// Device Watch will ping the device to proactively determine if the device has gone offline
// If the device was online the last time we refreshed, trigger another refresh as part of the ping.
def ping() {
    def isAlive = device.currentValue("deviceAlive") == "true" ? true : false
    if (isAlive) {
        refresh()
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def refresh() {
	log.debug "refresh called"
	poll()
	log.debug "refresh ended"
}

void poll() {
	log.debug "Executing 'poll' using parent SmartApp"
	parent.pollChild()
}

def generateEvent(Map results) {
	log.debug "parsing data $results"
	if(results) {

			def linkText = getLinkText(device)
		def supportedThermostatModes = ["off"]
		def thermostatMode = null

		results.each { name, value ->
			def event = [name: name, linkText: linkText, descriptionText: getThermostatDescriptionText(name, value, linkText),
						 handlerName: name]

			if (name=="temperature" || name=="heatingSetpoint" || name=="coolingSetpoint" ) {
				def sendValue =  location.temperatureScale == "C"? roundC(convertFtoC(value.toDouble())) : value.toInteger()
				event << [value: sendValue, unit: temperatureScale]
			}  else if (name=="maxCoolingSetpoint" || name=="minCoolingSetpoint" || name=="maxHeatingSetpoint" || name=="minHeatingSetpoint") {
				def sendValue =  location.temperatureScale == "C"? roundC(convertFtoC(value.toDouble())) : value.toInteger()
				event << [value: sendValue, unit: temperatureScale, displayed: false]
			}  else if (name=="heatMode" || name=="coolMode" || name=="autoMode" || name=="auxHeatMode"){
				if (value == true) {
					supportedThermostatModes << ((name == "auxHeatMode") ? "auxheatonly" : name - "Mode")
				}
				return // as we don't want to send this event here, proceed to next name/value pair
			}  else if (name=="thermostatFanMode"){
				sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false)
				event << [value: value.toString(), data:[supportedThermostatFanModes: fanModes()]]
			}  else if (name=="humidity") {
				event << [value: value.toString(), displayed: false, unit: "%"]
			} else if (name == "deviceAlive") {
				event['displayed'] = false
			} else if (name == "thermostatMode") {
				thermostatMode = value.toLowerCase()
				return // as we don't want to send this event here, proceed to next name/value pair
			} else {
				event << [value: value.toString()]
			}
			sendEvent(event)
		}
		if (state.supportedThermostatModes != supportedThermostatModes) {
			state.supportedThermostatModes = supportedThermostatModes
			sendEvent(name: "supportedThermostatModes", value: supportedThermostatModes, displayed: false)
		}
		if (thermostatMode) {
			sendEvent(name: "thermostatMode", value: thermostatMode, data:[supportedThermostatModes:state.supportedThermostatModes], linkText: linkText,
					descriptionText: getThermostatDescriptionText("thermostatMode", thermostatMode, linkText), handlerName: "thermostatMode")
		}
		generateSetpointEvent ()
		generateStatusEvent ()
	}
}

//return descriptionText to be shown on mobile activity feed
private getThermostatDescriptionText(name, value, linkText) {
	if(name == "temperature") {
		def sendValue = convertTemperatureIfNeeded(value.toDouble(), "F", 1) //API return temperature value in F
		sendValue =  location.temperatureScale == "C"? roundC(sendValue) : sendValue
		return "$linkText temperature is $sendValue ${location.temperatureScale}"

	} else if(name == "heatingSetpoint") {
		def sendValue = convertTemperatureIfNeeded(value.toDouble(), "F", 1) //API return temperature value in F
		sendValue =  location.temperatureScale == "C"? roundC(sendValue) : sendValue
		return "heating setpoint is $sendValue ${location.temperatureScale}"

	} else if(name == "coolingSetpoint"){
		def sendValue = convertTemperatureIfNeeded(value.toDouble(), "F", 1) //API return temperature value in F
		sendValue =  location.temperatureScale == "C"? roundC(sendValue) : sendValue
		return "cooling setpoint is $sendValue ${location.temperatureScale}"

	} else if (name == "thermostatMode") {
		return "thermostat mode is ${value}"

	} else if (name == "thermostatFanMode") {
		return "thermostat fan mode is ${value}"

	} else if (name == "humidity") {
		return "humidity is ${value} %"
	} else {
		return "${name} = ${value}"
	}
}

void setHeatingSetpoint(setpoint) {
	log.debug "***heating setpoint $setpoint"
	def heatingSetpoint = setpoint
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def deviceId = device.deviceNetworkId.split(/\./).last()
	def maxHeatingSetpoint = device.currentValue("maxHeatingSetpoint")
	def minHeatingSetpoint = device.currentValue("minHeatingSetpoint")

	//enforce limits of heatingSetpoint
	if (heatingSetpoint > maxHeatingSetpoint) {
		heatingSetpoint = maxHeatingSetpoint
	} else if (heatingSetpoint < minHeatingSetpoint) {
		heatingSetpoint = minHeatingSetpoint
	}

	//enforce limits of heatingSetpoint vs coolingSetpoint
	if (heatingSetpoint >= coolingSetpoint) {
		coolingSetpoint = heatingSetpoint
	}

	log.debug "Sending setHeatingSetpoint> coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}"

	def coolingValue = location.temperatureScale == "C"? convertCtoF(coolingSetpoint) : coolingSetpoint
	def heatingValue = location.temperatureScale == "C"? convertCtoF(heatingSetpoint) : heatingSetpoint

	def sendHoldType = holdType ? (holdType=="Temporary")? "nextTransition" : (holdType=="Permanent")? "indefinite" : "indefinite" : "indefinite"
	if (parent.setHold(heatingValue, coolingValue, deviceId, sendHoldType)) {
		sendEvent("name":"heatingSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"coolingSetpoint", "value":coolingSetpoint, "unit":location.temperatureScale)
		log.debug "Done setHeatingSetpoint> coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}"
		generateSetpointEvent()
		generateStatusEvent()
	} else {
		log.error "Error setHeatingSetpoint(setpoint)"
	}
}

void setCoolingSetpoint(setpoint) {
	log.debug "***cooling setpoint $setpoint"
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = setpoint
	def deviceId = device.deviceNetworkId.split(/\./).last()
	def maxCoolingSetpoint = device.currentValue("maxCoolingSetpoint")
	def minCoolingSetpoint = device.currentValue("minCoolingSetpoint")

	if (coolingSetpoint > maxCoolingSetpoint) {
		coolingSetpoint = maxCoolingSetpoint
	} else if (coolingSetpoint < minCoolingSetpoint) {
		coolingSetpoint = minCoolingSetpoint
	}

	//enforce limits of heatingSetpoint vs coolingSetpoint
	if (heatingSetpoint >= coolingSetpoint) {
		heatingSetpoint = coolingSetpoint
	}

	log.debug "Sending setCoolingSetpoint> coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}"

	def coolingValue = location.temperatureScale == "C"? convertCtoF(coolingSetpoint) : coolingSetpoint
	def heatingValue = location.temperatureScale == "C"? convertCtoF(heatingSetpoint) : heatingSetpoint

	def sendHoldType = holdType ? (holdType=="Temporary")? "nextTransition" : (holdType=="Permanent")? "indefinite" : "indefinite" : "indefinite"
	if (parent.setHold(heatingValue, coolingValue, deviceId, sendHoldType)) {
		sendEvent("name":"heatingSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"coolingSetpoint", "value":coolingSetpoint, "unit":location.temperatureScale)
		log.debug "Done setCoolingSetpoint>> coolingSetpoint = ${coolingSetpoint}, heatingSetpoint = ${heatingSetpoint}"
		generateSetpointEvent()
		generateStatusEvent()
	} else {
		log.error "Error setCoolingSetpoint(setpoint)"
	}
}

void resumeProgram() {
	log.debug "resumeProgram() is called"
	sendEvent("name":"thermostatStatus", "value":"resuming schedule", "description":statusText, displayed: false)
	def deviceId = device.deviceNetworkId.split(/\./).last()
	if (parent.resumeProgram(deviceId)) {
		sendEvent("name":"thermostatStatus", "value":"setpoint is updating", "description":statusText, displayed: false)
		runIn(5, "poll")
		log.debug "resumeProgram() is done"
		sendEvent("name":"resumeProgram", "value":"resume", descriptionText: "resumeProgram is done", displayed: false, isStateChange: true)
	} else {
		sendEvent("name":"thermostatStatus", "value":"failed resume click refresh", "description":statusText, displayed: false)
		log.error "Error resumeProgram() check parent.resumeProgram(deviceId)"
	}

}

def modes() {
	return state.supportedThermostatModes
}

def fanModes() {
	["on", "auto"]
}

def switchMode() {
	log.debug "in switchMode"
	def currentMode = device.currentState("thermostatMode")?.value
	def lastTriedMode = state.lastTriedMode ?: currentMode ?: "off"
	def modeOrder = modes()
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	switchToMode(nextMode)
}

def switchToMode(nextMode) {
	log.debug "In switchToMode = ${nextMode}"
	if (nextMode in modes()) {
		state.lastTriedMode = nextMode
		"$nextMode"()
	} else {
		log.debug("no mode method '$nextMode'")
	}
}

def switchFanMode() {
	def currentFanMode = device.currentState("thermostatFanMode")?.value
	log.debug "switching fan from current mode: $currentFanMode"
	def returnCommand

	switch (currentFanMode) {
		case "on":
			returnCommand = switchToFanMode("auto")
			break
		case "auto":
			returnCommand = switchToFanMode("on")
			break

	}
	if(!currentFanMode) { returnCommand = switchToFanMode("auto") }
	returnCommand
}

def switchToFanMode(nextMode) {
	log.debug "switching to fan mode: $nextMode"
	def returnCommand

	if(nextMode == "auto") {
		if(!fanModes.contains("auto")) {
			returnCommand = fanAuto()
		} else {
			returnCommand = switchToFanMode("on")
		}
	} else if(nextMode == "on") {
		if(!fanModes.contains("on")) {
			returnCommand = fanOn()
		} else {
			returnCommand = switchToFanMode("auto")
		}
	}

	returnCommand
}

def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

def setThermostatMode(String mode) {
	log.debug "setThermostatMode($mode)"
	mode = mode.toLowerCase()
	switchToMode(mode)
}

def setThermostatFanMode(String mode) {
	log.debug "setThermostatFanMode($mode)"
	mode = mode.toLowerCase()
	switchToFanMode(mode)
}

def generateModeEvent(mode) {
	sendEvent(name: "thermostatMode", value: mode, data:[supportedThermostatModes: state.supportedThermostatModes],
			descriptionText: "$device.displayName is in ${mode} mode")
}

def generateFanModeEvent(fanMode) {
	sendEvent(name: "thermostatFanMode", value: fanMode, data:[supportedThermostatFanModes: fanModes()],
			descriptionText: "$device.displayName fan is in ${fanMode} mode")
}

def generateOperatingStateEvent(operatingState) {
	sendEvent(name: "thermostatOperatingState", value: operatingState, descriptionText: "$device.displayName is ${operatingState}", displayed: true)
}

def off() {
	log.debug "off"
	def deviceId = device.deviceNetworkId.split(/\./).last()
	if (parent.setMode ("off", deviceId))
		generateModeEvent("off")
	else {
		log.debug "Error setting new mode."
		def currentMode = device.currentState("thermostatMode")?.value
		generateModeEvent(currentMode) // reset the tile back
	}
	generateSetpointEvent()
	generateStatusEvent()
}

def heat() {
	log.debug "heat"
	def deviceId = device.deviceNetworkId.split(/\./).last()
	if (parent.setMode ("heat", deviceId))
		generateModeEvent("heat")
	else {
		log.debug "Error setting new mode."
		def currentMode = device.currentState("thermostatMode")?.value
		generateModeEvent(currentMode) // reset the tile back
	}
	generateSetpointEvent()
	generateStatusEvent()
}

def emergencyHeat() {
	auxheatonly()
}

def auxheatonly() {
	log.debug "auxheatonly()"
	def deviceId = device.deviceNetworkId.split(/\./).last()
	if (parent.setMode ("auxHeatOnly", deviceId))
		generateModeEvent("auxheatonly")
	else {
		log.debug "Error setting new mode."
		def currentMode = device.currentState("thermostatMode")?.value
		generateModeEvent(currentMode) // reset the tile back
	}
	generateSetpointEvent()
	generateStatusEvent()
}

def cool() {
	log.debug "cool"
	def deviceId = device.deviceNetworkId.split(/\./).last()
	if (parent.setMode ("cool", deviceId))
		generateModeEvent("cool")
	else {
		log.debug "Error setting new mode."
		def currentMode = device.currentState("thermostatMode")?.value
		generateModeEvent(currentMode) // reset the tile back
	}
	generateSetpointEvent()
	generateStatusEvent()
}

def auto() {
	log.debug "auto"
	def deviceId = device.deviceNetworkId.split(/\./).last()
	if (parent.setMode ("auto", deviceId))
		generateModeEvent("auto")
	else {
		log.debug "Error setting new mode."
		def currentMode = device.currentState("thermostatMode")?.value
		generateModeEvent(currentMode) // reset the tile back
	}
	generateSetpointEvent()
	generateStatusEvent()
}

def fanOn() {
	log.debug "fanOn"
	String fanMode = "on"
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def deviceId = device.deviceNetworkId.split(/\./).last()

	def sendHoldType = holdType ? (holdType=="Temporary")? "nextTransition" : (holdType=="Permanent")? "indefinite" : "indefinite" : "indefinite"

	def coolingValue = location.temperatureScale == "C"? convertCtoF(coolingSetpoint) : coolingSetpoint
	def heatingValue = location.temperatureScale == "C"? convertCtoF(heatingSetpoint) : heatingSetpoint

	if (parent.setFanMode(heatingValue, coolingValue, deviceId, sendHoldType, fanMode)) {
		generateFanModeEvent(fanMode)
	} else {
		log.debug "Error setting new mode."
		def currentFanMode = device.currentState("thermostatFanMode")?.value
		generateFanModeEvent(currentFanMode) // reset the tile back
	}
}

def fanAuto() {
	log.debug "fanAuto"
	String fanMode = "auto"
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def deviceId = device.deviceNetworkId.split(/\./).last()

	def sendHoldType = holdType ? (holdType=="Temporary")? "nextTransition" : (holdType=="Permanent")? "indefinite" : "indefinite" : "indefinite"

	def coolingValue = location.temperatureScale == "C"? convertCtoF(coolingSetpoint) : coolingSetpoint
	def heatingValue = location.temperatureScale == "C"? convertCtoF(heatingSetpoint) : heatingSetpoint

	if (parent.setFanMode(heatingValue, coolingValue, deviceId, sendHoldType, fanMode)) {
		generateFanModeEvent(fanMode)
	} else {
		log.debug "Error setting new mode."
		def currentFanMode = device.currentState("thermostatFanMode")?.value
		generateFanModeEvent(currentFanMode) // reset the tile back
	}
}

def generateSetpointEvent() {
	log.debug "Generate SetPoint Event"

	def mode = device.currentValue("thermostatMode")

	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def maxHeatingSetpoint = device.currentValue("maxHeatingSetpoint")
	def maxCoolingSetpoint = device.currentValue("maxCoolingSetpoint")
	def minHeatingSetpoint = device.currentValue("minHeatingSetpoint")
	def minCoolingSetpoint = device.currentValue("minCoolingSetpoint")

	if(location.temperatureScale == "C") {
		maxHeatingSetpoint = maxHeatingSetpoint > 40 ? roundC(convertFtoC(maxHeatingSetpoint)) : roundC(maxHeatingSetpoint)
		maxCoolingSetpoint = maxCoolingSetpoint > 40 ? roundC(convertFtoC(maxCoolingSetpoint)) : roundC(maxCoolingSetpoint)
		minHeatingSetpoint = minHeatingSetpoint > 40 ? roundC(convertFtoC(minHeatingSetpoint)) : roundC(minHeatingSetpoint)
		minCoolingSetpoint = minCoolingSetpoint > 40 ? roundC(convertFtoC(minCoolingSetpoint)) : roundC(minCoolingSetpoint)
		heatingSetpoint = heatingSetpoint > 40 ? roundC(convertFtoC(heatingSetpoint)) : roundC(heatingSetpoint)
		coolingSetpoint = coolingSetpoint > 40 ? roundC(convertFtoC(coolingSetpoint)) : roundC(coolingSetpoint)
	} else {
		maxHeatingSetpoint = maxHeatingSetpoint < 40 ? roundC(convertCtoF(maxHeatingSetpoint)) : maxHeatingSetpoint
		maxCoolingSetpoint = maxCoolingSetpoint < 40 ? roundC(convertCtoF(maxCoolingSetpoint)) : maxCoolingSetpoint
		minHeatingSetpoint = minHeatingSetpoint < 40 ? roundC(convertCtoF(minHeatingSetpoint)) : minHeatingSetpoint
		minCoolingSetpoint = minCoolingSetpoint < 40 ? roundC(convertCtoF(minCoolingSetpoint)) : minCoolingSetpoint
		heatingSetpoint = heatingSetpoint < 40 ? roundC(convertCtoF(heatingSetpoint)) : heatingSetpoint
		coolingSetpoint = coolingSetpoint < 40 ? roundC(convertCtoF(coolingSetpoint)) : coolingSetpoint
	}

	log.debug "Current Mode = ${mode}"
	log.debug "Heating Setpoint = ${heatingSetpoint}"
	log.debug "Cooling Setpoint = ${coolingSetpoint}"

	sendEvent("name":"maxHeatingSetpoint", "value":maxHeatingSetpoint, "unit":location.temperatureScale)
	sendEvent("name":"maxCoolingSetpoint", "value":maxCoolingSetpoint, "unit":location.temperatureScale)
	sendEvent("name":"minHeatingSetpoint", "value":minHeatingSetpoint, "unit":location.temperatureScale)
	sendEvent("name":"minCoolingSetpoint", "value":minCoolingSetpoint, "unit":location.temperatureScale)
	sendEvent("name":"heatingSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale)
	sendEvent("name":"coolingSetpoint", "value":coolingSetpoint, "unit":location.temperatureScale)

	def averageSetpoint = roundC((heatingSetpoint + coolingSetpoint) / 2)
	if (mode == "heat") {
		sendEvent("name":"thermostatSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"displayThermostatSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale, displayed: false)
	}
	else if (mode == "cool") {
		sendEvent("name":"thermostatSetpoint", "value":coolingSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"displayThermostatSetpoint", "value":coolingSetpoint, "unit":location.temperatureScale, displayed: false)
	} else if (mode == "auto") {
		sendEvent("name":"thermostatSetpoint", "value":averageSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"displayThermostatSetpoint", "value":"Auto", displayed: false)
	} else if (mode == "off") {
		sendEvent("name":"thermostatSetpoint", "value":averageSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"displayThermostatSetpoint", "value":"Off", displayed: false)
	} else if (mode == "auxheatonly") {
		sendEvent("name":"thermostatSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale)
		sendEvent("name":"displayThermostatSetpoint", "value":heatingSetpoint, "unit":location.temperatureScale, displayed: false)
	}
}

void raiseSetpoint() {
	def mode = device.currentValue("thermostatMode")
	def targetvalue
	def maxHeatingSetpoint = device.currentValue("maxHeatingSetpoint")
	def maxCoolingSetpoint = device.currentValue("maxCoolingSetpoint")

	if (mode == "off" || mode == "auto") {
		log.warn "this mode: $mode does not allow raiseSetpoint"
	} else {

		def heatingSetpoint = device.currentValue("heatingSetpoint")
		def coolingSetpoint = device.currentValue("coolingSetpoint")
		def thermostatSetpoint = device.currentValue("thermostatSetpoint")

		if (location.temperatureScale == "C") {
			maxHeatingSetpoint = maxHeatingSetpoint > 40 ? convertFtoC(maxHeatingSetpoint) : maxHeatingSetpoint
			maxCoolingSetpoint = maxCoolingSetpoint > 40 ? convertFtoC(maxCoolingSetpoint) : maxCoolingSetpoint
			heatingSetpoint = heatingSetpoint > 40 ? convertFtoC(heatingSetpoint) : heatingSetpoint
			coolingSetpoint = coolingSetpoint > 40 ? convertFtoC(coolingSetpoint) : coolingSetpoint
			thermostatSetpoint = thermostatSetpoint > 40 ? convertFtoC(thermostatSetpoint) : thermostatSetpoint
		} else {
			maxHeatingSetpoint = maxHeatingSetpoint < 40 ? convertCtoF(maxHeatingSetpoint) : maxHeatingSetpoint
			maxCoolingSetpoint = maxCoolingSetpoint < 40 ? convertCtoF(maxCoolingSetpoint) : maxCoolingSetpoint
			heatingSetpoint = heatingSetpoint < 40 ? convertCtoF(heatingSetpoint) : heatingSetpoint
			coolingSetpoint = coolingSetpoint < 40 ? convertCtoF(coolingSetpoint) : coolingSetpoint
			thermostatSetpoint = thermostatSetpoint < 40 ? convertCtoF(thermostatSetpoint) : thermostatSetpoint
		}

		log.debug "raiseSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}"

		targetvalue = thermostatSetpoint ? thermostatSetpoint : 0
		targetvalue = location.temperatureScale == "F"? targetvalue + 1 : targetvalue + 0.5

		if ((mode == "heat" || mode == "auxheatonly") && targetvalue > maxHeatingSetpoint) {
			targetvalue = maxHeatingSetpoint
		} else if (mode == "cool" && targetvalue > maxCoolingSetpoint) {
			targetvalue = maxCoolingSetpoint
		}

		sendEvent("name":"thermostatSetpoint", "value":targetvalue, "unit":location.temperatureScale, displayed: false)
		sendEvent("name":"displayThermostatSetpoint", "value":targetvalue, "unit":location.temperatureScale, displayed: false)
		log.info "In mode $mode raiseSetpoint() to $targetvalue"

		runIn(3, "alterSetpoint", [data: [value:targetvalue], overwrite: true]) //when user click button this runIn will be overwrite
	}
}

//called by tile when user hit raise temperature button on UI
void lowerSetpoint() {
	def mode = device.currentValue("thermostatMode")
	def targetvalue
	def minHeatingSetpoint = device.currentValue("minHeatingSetpoint")
	def minCoolingSetpoint = device.currentValue("minCoolingSetpoint")

	if (mode == "off" || mode == "auto") {
		log.warn "this mode: $mode does not allow lowerSetpoint"
	} else {
		def heatingSetpoint = device.currentValue("heatingSetpoint")
		def coolingSetpoint = device.currentValue("coolingSetpoint")
		def thermostatSetpoint = device.currentValue("thermostatSetpoint")

		if (location.temperatureScale == "C") {
			minHeatingSetpoint = minHeatingSetpoint > 40 ? convertFtoC(minHeatingSetpoint) : minHeatingSetpoint
			minCoolingSetpoint = minCoolingSetpoint > 40 ? convertFtoC(minCoolingSetpoint) : minCoolingSetpoint
			heatingSetpoint = heatingSetpoint > 40 ? convertFtoC(heatingSetpoint) : heatingSetpoint
			coolingSetpoint = coolingSetpoint > 40 ? convertFtoC(coolingSetpoint) : coolingSetpoint
			thermostatSetpoint = thermostatSetpoint > 40 ? convertFtoC(thermostatSetpoint) : thermostatSetpoint
		} else {
			minHeatingSetpoint = minHeatingSetpoint < 40 ? convertCtoF(minHeatingSetpoint) : minHeatingSetpoint
			minCoolingSetpoint = minCoolingSetpoint < 40 ? convertCtoF(minCoolingSetpoint) : minCoolingSetpoint
			heatingSetpoint = heatingSetpoint < 40 ? convertCtoF(heatingSetpoint) : heatingSetpoint
			coolingSetpoint = coolingSetpoint < 40 ? convertCtoF(coolingSetpoint) : coolingSetpoint
			thermostatSetpoint = thermostatSetpoint < 40 ? convertCtoF(thermostatSetpoint) : thermostatSetpoint
		}
		log.debug "lowerSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}"

		targetvalue = thermostatSetpoint ? thermostatSetpoint : 0
		targetvalue = location.temperatureScale == "F"? targetvalue - 1 : targetvalue - 0.5

		if ((mode == "heat" || mode == "auxheatonly") && targetvalue < minHeatingSetpoint) {
			targetvalue = minHeatingSetpoint
		} else if (mode == "cool" && targetvalue < minCoolingSetpoint) {
			targetvalue = minCoolingSetpoint
		}

		sendEvent("name":"thermostatSetpoint", "value":targetvalue, "unit":location.temperatureScale, displayed: false)
		sendEvent("name":"displayThermostatSetpoint", "value":targetvalue, "unit":location.temperatureScale, displayed: false)
		log.info "In mode $mode lowerSetpoint() to $targetvalue"

		runIn(3, "alterSetpoint", [data: [value:targetvalue], overwrite: true]) //when user click button this runIn will be overwrite
	}
}

//called by raiseSetpoint() and lowerSetpoint()
void alterSetpoint(temp) {
	def mode = device.currentValue("thermostatMode")

	if (mode == "off" || mode == "auto") {
		log.warn "this mode: $mode does not allow alterSetpoint"
	} else {
		def heatingSetpoint = device.currentValue("heatingSetpoint")
		def coolingSetpoint = device.currentValue("coolingSetpoint")
		def deviceId = device.deviceNetworkId.split(/\./).last()

		def targetHeatingSetpoint
		def targetCoolingSetpoint

		def temperatureScaleHasChanged = false

		if (location.temperatureScale == "C") {
			if ( heatingSetpoint > 40.0 || coolingSetpoint > 40.0 ) {
				temperatureScaleHasChanged = true
			}
		} else {
			if ( heatingSetpoint < 40.0 || coolingSetpoint < 40.0 ) {
				temperatureScaleHasChanged = true
			}
		}

		//step1: check thermostatMode, enforce limits before sending request to cloud
		if (mode == "heat" || mode == "auxheatonly"){
			if (temp.value > coolingSetpoint){
				targetHeatingSetpoint = temp.value
				targetCoolingSetpoint = temp.value
			} else {
				targetHeatingSetpoint = temp.value
				targetCoolingSetpoint = coolingSetpoint
			}
		} else if (mode == "cool") {
			//enforce limits before sending request to cloud
			if (temp.value < heatingSetpoint){
				targetHeatingSetpoint = temp.value
				targetCoolingSetpoint = temp.value
			} else {
				targetHeatingSetpoint = heatingSetpoint
				targetCoolingSetpoint = temp.value
			}
		}

		log.debug "alterSetpoint >> in mode ${mode} trying to change heatingSetpoint to $targetHeatingSetpoint " +
				"coolingSetpoint to $targetCoolingSetpoint with holdType : ${holdType}"

		def sendHoldType = holdType ? (holdType=="Temporary")? "nextTransition" : (holdType=="Permanent")? "indefinite" : "indefinite" : "indefinite"

		def coolingValue = location.temperatureScale == "C"? convertCtoF(targetCoolingSetpoint) : targetCoolingSetpoint
		def heatingValue = location.temperatureScale == "C"? convertCtoF(targetHeatingSetpoint) : targetHeatingSetpoint

		if (parent.setHold(heatingValue, coolingValue, deviceId, sendHoldType)) {
			sendEvent("name": "thermostatSetpoint", "value": temp.value, displayed: false)
			sendEvent("name": "displayThermostatSetpoint", "value": temp.value, displayed: false)
			sendEvent("name": "heatingSetpoint", "value": targetHeatingSetpoint, "unit": location.temperatureScale)
			sendEvent("name": "coolingSetpoint", "value": targetCoolingSetpoint, "unit": location.temperatureScale)
			log.debug "alterSetpoint in mode $mode succeed change setpoint to= ${temp.value}"
		} else {
			log.error "Error alterSetpoint()"
			if (mode == "heat" || mode == "auxheatonly"){
				sendEvent("name": "thermostatSetpoint", "value": heatingSetpoint.toString(), displayed: false)
				sendEvent("name": "displayThermostatSetpoint", "value": heatingSetpoint.toString(), displayed: false)
			} else if (mode == "cool") {
				sendEvent("name": "thermostatSetpoint", "value": coolingSetpoint.toString(), displayed: false)
				sendEvent("name": "displayThermostatSetpoint", "value": heatingSetpoint.toString(), displayed: false)
			}
		}

		if ( temperatureScaleHasChanged )
			generateSetpointEvent()
		generateStatusEvent()
	}
}

def generateStatusEvent() {
	def mode = device.currentValue("thermostatMode")
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def temperature = device.currentValue("temperature")
	def statusText
	def operatingState = "idle"

	log.debug "Generate Status Event for Mode = ${mode}"
	log.debug "Temperature = ${temperature}"
	log.debug "Heating set point = ${heatingSetpoint}"
	log.debug "Cooling set point = ${coolingSetpoint}"
	log.debug "HVAC Mode = ${mode}"

	if (mode == "heat" || mode == "auxheatonly") {
		if (temperature >= heatingSetpoint) {
			statusText = "Right Now: Idle"
		} else {
			statusText = "Heating to ${heatingSetpoint} ${location.temperatureScale}"
			operatingState = "heating"
		}
	} else if (mode == "cool") {
		if (temperature <= coolingSetpoint) {
			statusText = "Right Now: Idle"
		} else {
			statusText = "Cooling to ${coolingSetpoint} ${location.temperatureScale}"
			operatingState = "cooling"
		}
	} else if (mode == "auto") {
		statusText = "Right Now: Auto"
		if (temperature < heatingSetpoint) {
			operatingState = "heating"
		} else if (temperature > coolingSetpoint) {
			operatingState = "cooling"
		}
	} else if (mode == "off") {
		statusText = "Right Now: Off"
	} else {
		statusText = "?"
	}

	log.debug "Generate Status Event = ${statusText}"
	sendEvent("name":"thermostatStatus", "value":statusText, "description":statusText, displayed: true)
	sendEvent("name":"thermostatOperatingState", "value":operatingState, "description":operatingState, displayed: false)
}

def generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "$device.displayName $notificationMessage", descriptionText: "$device.displayName $notificationMessage", displayed: true)
}

def roundC (tempC) {
	return (Math.round(tempC.toDouble() * 2))/2
}

def convertFtoC (tempF) {
	return ((Math.round(((tempF - 32)*(5/9)) * 2))/2).toDouble()
}

def convertCtoF (tempC) {
	return (Math.round(tempC * (9/5)) + 32).toInteger()
}
