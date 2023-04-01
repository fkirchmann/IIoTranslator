/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device;

import com.iiotranslator.device.drivers.KnownDeviceDrivers;
import com.iiotranslator.opc.*;
import com.iiotranslator.service.DevicesConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Device Drivers and distributes requests to them.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DevicesService implements VariableNodeAccessor {
    private final DevicesConfiguration config;

    @Getter
    private final RootNode rootNode = new RootNode();

    private final Map<FolderNode, DeviceDriver> devices = new ConcurrentHashMap<>();

    @PostConstruct
    private void initialize() {
        config.getDevices().parallelStream().forEach(device -> {
            log.info("[{}]: Initializing device", device.getName());
            var deviceFolder = rootNode.addFolder(device.getName());
            var driver = KnownDeviceDrivers.getDriver(device, deviceFolder);
            devices.put(deviceFolder, driver);
            log.info("[{}]: Device initialized", device.getName());
        });
        log.info("All devices initialized");
    }

    @Override
    public CompletableFuture<DataValue> read(VariableNode variable) {
        return getDriver(variable).read(variable);
    }

    @Override
    public CompletableFuture<Void> write(WritableVariableNode variable, Object value) {
        return getDriver(variable).write(variable, value);
    }

    private DeviceDriver getDriver(Node node) {
        for(var entry : devices.entrySet()) {
            if(entry.getKey().isParentOf(node)) {
                return entry.getValue();
            }
        }
        log.warn("No driver found for node {}", node);
        throw new IllegalArgumentException("Node is not a child of any device");
    }
}
