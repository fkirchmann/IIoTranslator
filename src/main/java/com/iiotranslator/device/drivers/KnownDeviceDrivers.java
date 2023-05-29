/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers;

import com.iiotranslator.device.Device;
import com.iiotranslator.device.drivers.binder.BinderKBDriver;
import com.iiotranslator.device.drivers.binder.BinderKBFDriver;
import com.iiotranslator.device.drivers.other.IPSwitchDriver;
import com.iiotranslator.device.drivers.other.TestDriver;
import com.iiotranslator.device.drivers.weiss.WeissLabEventDriver;
import java.util.function.Supplier;

public class KnownDeviceDrivers {
    private KnownDeviceDrivers() {
        // This class is not meant to be instantiated.
    }

    public static Supplier<DeviceDriver> getDriverSupplier(Device device) throws UnknownDriverException {
        return switch (device.getDriver()) {
                // ------------------------------------------------------------------------------------------
                // Register all drivers here
                // ------------------------------------------------------------------------------------------
            case "test" -> TestDriver::new;
            case "ipswitch" -> IPSwitchDriver::new;
            case "binder_kb" -> BinderKBDriver::new;
            case "binder_kbf" -> BinderKBFDriver::new;
            case "weiss_labevent" -> WeissLabEventDriver::new;
                // ------------------------------------------------------------------------------------------
            default -> throw new UnknownDriverException("Unknown device driver: " + device.getDriver());
        };
    }

    public static class UnknownDriverException extends RuntimeException {
        public UnknownDriverException(String message) {
            super(message);
        }
    }
}
