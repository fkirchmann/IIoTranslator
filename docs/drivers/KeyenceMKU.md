# Keyence MK-U Series
Supports Keyence MK-U6000/MK-U2000 series industrial ink-jet printers.

You need to configure the following settings on the printer:
* Configure Ethernet connectivity and addressing
* Ensure that the port number is set the same as the one configured in the driver (default is 9004)
* Delimiter: CR (not ETX)
* No checksum

Note that the *line speed* tag currently only edits the global line speed setting. To use it, the line printing settings in your program need to use the global defaults.

Example Configuration:
```properties
iiot.devices.printer1.driver=keyence_mku
iiot.devices.printer1.hostname=10.1.2.3
# Default timeout is 2000ms, but can be changed here
#iiot.devices.printer1.timeout=2000
# Default port is 9004, but can be changed here
#iiot.devices.printer1.port=9004
```