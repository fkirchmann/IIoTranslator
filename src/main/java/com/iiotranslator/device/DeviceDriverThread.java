/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device;

import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.opc.VariableNodeAccessor;
import com.iiotranslator.opc.WritableVariableNode;
import com.iiotranslator.service.Device;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * Abstract base class for device drivers.
 * <p>
 * Implementations should register their constructor in the
 * {@link com.iiotranslator.device.drivers.KnownDeviceDrivers} class.
 */
@Slf4j
public class DeviceDriverThread implements VariableNodeAccessor, DeviceRequestCompletionListener {

    @Getter
    private final Device device;

    private final FolderNode deviceFolder;
    private final Thread thread;

    private final Supplier<DeviceDriver> driverSupplier;
    private final Map<DeviceRequest, Set<CompletableFuture<?>>> pendingRequests =
            Collections.synchronizedMap(new LinkedHashMap<>());

    DeviceDriverThread(Device device, FolderNode deviceFolder, Supplier<DeviceDriver> driverSupplier) {
        this.device = device;
        this.deviceFolder = deviceFolder;
        this.driverSupplier = driverSupplier;
        this.thread = new Thread(this::thread);
        thread.setDaemon(false);
        thread.setName("DeviceDriver \"" + device.getDriver() + "\" for device \"" + device.getName() + "\"");
        thread.start();
    }

    private void thread() {
        DeviceDriver deviceDriver;
        try {
            deviceDriver = driverSupplier.get();
        } catch (Exception e) {
            log.error("[{}]: Could not instantiate device driver", getDevice().getName(), e);
            return;
        }
        try {
            deviceDriver.initialize(device, deviceFolder);
        } catch (Exception e) {
            log.error("[{}]: Error in device driver initialization", getDevice().getName(), e);
            return;
        }
        try {
            List<DeviceRequest> requests;
            while (true) {
                synchronized (pendingRequests) {
                    requests = new ArrayList<>(pendingRequests.keySet());
                    if (pendingRequests.isEmpty()) {
                        pendingRequests.wait();
                    }
                }
                if (!requests.isEmpty()) {
                    deviceDriver.process(requests, this);
                }
            }
        } catch (Exception e) {
            log.error("[{}]: Error in device driver loop", getDevice().getName(), e);
        }
    }

    public void completeReadRequest(DeviceRequest.ReadRequest request, DataValue value) {
        var futures = pendingRequests.remove(request);
        if (futures != null) {
            futures.forEach(future -> ((CompletableFuture<DataValue>) future).complete(value));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    public void completeWriteRequest(DeviceRequest.WriteRequest request) {
        var futures = pendingRequests.remove(request);
        if (futures != null) {
            futures.forEach(future -> future.complete(null));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    public void completeWriteRequest(DeviceRequest.WriteRequest request, Exception e) {
        var futures = pendingRequests.remove(request);
        if (futures != null) {
            futures.forEach(future -> future.completeExceptionally(e));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    public CompletableFuture<DataValue> read(VariableNode variable) {
        var future = new CompletableFuture<DataValue>();
        synchronized (pendingRequests) {
            pendingRequests
                    .computeIfAbsent(new DeviceRequest.ReadRequest(variable), k -> new HashSet<>())
                    .add(future);
            pendingRequests.notify();
        }
        return future;
    }

    public CompletableFuture<Void> write(WritableVariableNode variable, Object value) {
        var future = new CompletableFuture<Void>();
        synchronized (pendingRequests) {
            pendingRequests
                    .computeIfAbsent(new DeviceRequest.WriteRequest(variable, value), k -> new HashSet<>())
                    .add(future);
            pendingRequests.notify();
        }
        return future;
    }
}
