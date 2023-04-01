# IPswitch Driver
Supports [IPswitch](https://www.sms-guard.org/shop.php) devices, tested with an [IPswitch-S0m-Wifi](https://www.sms-guard.org/downloads/IPswitch-S0m-WiFi-Anleitung.pdf).
```properties
iiot.devices.smartmeter1.driver=ipswitch
iiot.devices.smartmeter1.hostname=10.1.2.3

# Default timeout is 2000ms, but can be changed here
#iiot.devices.oven1.timeout=2000
```

### EEPROM Backup
The EEPROM Backup feature ensures that the current meter reading is saved to the device's EEPROM memory every 24 hours by default. This prevents the loss of the counter value when the device is turned off. Keep in mind that setting the backup frequency too low is not advised, as the EEPROM has a limited number of write cycles (specified at 10,000).
```properties 
#iiot.devices.smartmeter1.impulseBackupIntervalHours=24
```
Additionally, if the driver detects that the device meter reading is suddenly lower, it will automatically restore the most recent measured value from its driver memory. Thus, the EEPROM backup feature is mostly useful in case where both the device and the IIoTranslator are turned off at the same time.