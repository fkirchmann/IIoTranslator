# Binder KB & KBF driver
Supports Binder KB (round knob with LCD) and KBF (touchscreen) ovens.

Example Configuration for Binder KB:
```properties
iiot.devices.oven1.driver=binder_kb
iiot.devices.oven1.hostname=10.1.2.3
# Default timeout is 2000ms, but can be changed here
#iiot.devices.oven1.timeout=2000
```
Example Configuration for Binder KBF:
```properties
iiot.devices.oven2.driver=binder_kbf
iiot.devices.oven2.hostname=10.1.2.3
# Default timeout is 2000ms, but can be changed here
#iiot.devices.oven1.timeout=2000
```