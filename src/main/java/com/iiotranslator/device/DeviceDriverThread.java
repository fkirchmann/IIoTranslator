/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device;

import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.OpcVariableNodeAccessor;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.opc.WritableVariableNode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * This class provides a seperate thread and {@link DeviceDriver} instance for each device, preventing
 * devices from blocking each other.
 */
@Slf4j
public class DeviceDriverThread implements OpcVariableNodeAccessor {
    private final Device device;
    private DeviceDriver deviceDriver;
    private final FolderNode deviceFolder;
    private final Map<DeviceRequest, Set<CompletableFuture<?>>> pendingRequests =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private final DeviceRequestCompletionListener threadRequestCompletionListener =
            new DeviceRequestCompletionListener() {
                @Override
                public void completeReadRequestExceptionally(DeviceRequest.ReadRequest request, DataValue value) {
                    DeviceDriverThread.this.completeReadRequest(request, value);
                }

                @Override
                public void completeWriteRequestExceptionally(DeviceRequest.WriteRequest request) {
                    DeviceDriverThread.this.completeWriteRequest(request);
                }

                @Override
                public void completeWriteRequestExceptionally(DeviceRequest.WriteRequest request, Exception e) {
                    DeviceDriverThread.this.completeWriteRequest(request, e);
                }
            };

    DeviceDriverThread(Device device, FolderNode deviceFolder, Supplier<DeviceDriver> driverSupplier) {
        this.device = device;
        this.deviceFolder = deviceFolder;
        Thread thread = new Thread(() -> thread(driverSupplier));
        thread.setDaemon(false);
        thread.setName("DeviceDriver \"" + device.getDriver() + "\" for device \"" + device.getName() + "\"");
        thread.start();
    }

    private void thread(Supplier<DeviceDriver> driverSupplier) {
        try {
            deviceDriver = driverSupplier.get();
        } catch (Exception e) {
            log.error("[{}]: Could not instantiate device driver", device.getName(), e);
            return;
        }
        try {
            deviceDriver.initialize(device, deviceFolder);
        } catch (Exception e) {
            log.error("[{}]: Error in device driver initialization", device.getName(), e);
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
                    deviceDriver.process(requests, threadRequestCompletionListener);
                }
            }
        } catch (Exception e) {
            log.error("[{}]: Error in device driver loop", device.getName(), e);
        }
    }

    private void completeReadRequest(DeviceRequest.ReadRequest request, DataValue value) {
        var futures = pendingRequests.remove(request);
        if (futures != null) {
            futures.forEach(future -> ((CompletableFuture<DataValue>) future).complete(value));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    private void completeWriteRequest(DeviceRequest.WriteRequest request) {
        var futures = pendingRequests.remove(request);
        if (futures != null) {
            futures.forEach(future -> future.complete(null));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    private void completeWriteRequest(DeviceRequest.WriteRequest request, Exception e) {
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

    public CompletableFuture<Void> write(WritableVariableNode variable, DataValue value) {
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
