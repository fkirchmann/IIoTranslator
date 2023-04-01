# WeissTechnik LabEvent Driver
Supports WeissTechnik LabEvent ovens. Example configuration:
```properties
iiot.devices.oven1.driver=weiss_labevent
iiot.devices.oven1.hostname=10.1.2.3
#iiot.devices.oven1.user=admin
#iiot.devices.oven1.password=admin

# Default timeout is 2000ms, but can be changed here
#iiot.devices.oven1.timeout=2000
# The HTTPS port should not need to be changed
#iiot.devices.oven1.port=443
```