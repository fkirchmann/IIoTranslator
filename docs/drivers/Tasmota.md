# Tasmota Driver
Supports [Tasmota](https://tasmota.github.io/docs/) devices with an energy meter, such as the [Nous A1T](https://www.berrybase.de/en/nous-a1t-smarte-steckdose-tasmota-firmware-wlan).
```properties
iiot.devices.smartmeter1.driver=tasmota
iiot.devices.smartmeter1.hostname=10.1.2.3

# Default timeout is 8000ms, but can be changed here
#iiot.devices.smartmeter1.timeout=8000
```
Currently, only reading the energy consumption (in W and Wh) and related parameters is supported.

To check if your device is supported, open 
```http://10.1.2.3/cm?cmnd=status%208``` (don't forget to replace the IP address with your device's IP address). If the response contains an ```"ENERGY"``` object, then the device is supported.