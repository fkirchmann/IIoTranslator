/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device;

import com.iiotranslator.device.drivers.KnownDeviceDrivers;
import com.iiotranslator.opc.*;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.stereotype.Service;

/**
 * Manages Device Drivers and distributes requests to them. Also creates an OPC UA folder for each device, and provides
 * that to its driver so that it can create its own nodes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DevicesService implements OpcVariableNodeAccessor {
    private final OpcServerService opcServer;

    private final DevicesConfiguration config;

    private final Map<FolderNode, DeviceDriverThread> devices = new ConcurrentHashMap<>();

    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    @PostConstruct
    private void initialize() {
        var rootNode = opcServer.getServer().getRootNode().get();
        log.info("Starting device driver threads");
        config.getDevices().parallelStream().forEach(device -> {
            log.info("[{}]: Initializing device", device.getName());
            var deviceFolder = rootNode.addFolder(device.getName());
            try {
                var driverSupplier = KnownDeviceDrivers.getDriverSupplier(device);
                var driverThread = new DeviceDriverThread(device, deviceFolder, driverSupplier);
                devices.put(deviceFolder, driverThread);
                log.info("[{}]: Device driver thread started", device.getName());
            } catch (KnownDeviceDrivers.UnknownDriverException e) {
                log.error("[{}]: Unknown driver {}, device not initialized", device.getName(), device.getDriver());
            }
        });
        log.info("All device driver threads started");
        opcServer.getServer().setVariableNodeAccessor(this);
    }

    @Override
    public CompletableFuture<DataValue> read(VariableNode variable) {
        return getDriver(variable).read(variable);
    }

    @Override
    public CompletableFuture<Void> write(WritableVariableNode variable, DataValue value) {
        return getDriver(variable).write(variable, value);
    }

    private DeviceDriverThread getDriver(Node node) {
        for (var entry : devices.entrySet()) {
            if (entry.getKey().isParentOf(node)) {
                return entry.getValue();
            }
        }
        log.warn("No driver found for node {}", node);
        throw new IllegalArgumentException("Node is not a child of any device");
    }
}
