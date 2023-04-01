/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers;

import com.iiotranslator.device.DeviceDriver;
import com.iiotranslator.device.drivers.binder.BinderKBDriver;
import com.iiotranslator.device.drivers.binder.BinderKBFDriver;
import com.iiotranslator.device.drivers.other.IPSwitchDriver;
import com.iiotranslator.device.drivers.weiss.WeissLabEventDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.service.Device;

public class KnownDeviceDrivers {
    private KnownDeviceDrivers() {
    }

    public static DeviceDriver getDriver(Device device, FolderNode deviceFolder) {
        switch (device.getDriver()) {
            case "test":
                return new TestDriver(device, deviceFolder);
            case "ipswitch":
                return new IPSwitchDriver(device, deviceFolder);
            case "binder_kb":
                return new BinderKBDriver(device, deviceFolder);
            case "binder_kbf":
                return new BinderKBFDriver(device, deviceFolder);
            case "weiss_labevent":
                return new WeissLabEventDriver(device, deviceFolder);
            default:
                throw new IllegalArgumentException("Unknown device driver: " + device.getDriver());
        }
    }
}
