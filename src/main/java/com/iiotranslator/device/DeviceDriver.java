/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device;

import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.opc.VariableNodeAccessor;
import com.iiotranslator.opc.WritableVariableNode;
import com.iiotranslator.service.Device;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class DeviceDriver implements VariableNodeAccessor {
    @Getter
    private final Device device;
    private final FolderNode deviceFolder;

    private final Thread thread;

    private final Map<DeviceRequest, Set<CompletableFuture<?>>> pendingRequests =
            Collections.synchronizedMap(new LinkedHashMap<>());

    protected DeviceDriver(Device device, FolderNode deviceFolder) {
        this.device = device;
        this.deviceFolder = deviceFolder;
        this.thread = new Thread(this::thread);
        thread.setDaemon(false);
        thread.setName("DeviceDriver \"" + device.getDriver() + "\" for device \"" + device.getName() + "\"");
        thread.start();
    }

    private void thread() {
        try {
            createNodes(device, deviceFolder);
        } catch (Exception e) {
            log.error("[{}]: Error in device driver node creation", getDevice().getName(), e);
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
                if(!requests.isEmpty()) {
                    process(requests);
                }
            }
        } catch (Exception e) {
            log.error("[{}]: Error in device driver loop", getDevice().getName(), e);
        }
    }

    protected abstract void createNodes(Device device, FolderNode deviceFolderNode);

    protected abstract void process(List<DeviceRequest> requestQueue);

    protected void completeReadRequest(DeviceRequest.ReadRequest request, DataValue value) {
        var futures = pendingRequests.remove(request);
        if(futures != null) {
            futures.forEach(future -> ((CompletableFuture<DataValue>) future).complete(value));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    protected void completeWriteRequest(DeviceRequest.WriteRequest request) {
        var futures = pendingRequests.remove(request);
        if(futures != null) {
            futures.forEach(future -> future.complete(null));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    protected void completeWriteRequest(DeviceRequest.WriteRequest request, Exception e) {
        var futures = pendingRequests.remove(request);
        if(futures != null) {
            futures.forEach(future -> future.completeExceptionally(e));
        } else {
            throw new IllegalStateException("No pending requests for " + request);
        }
    }

    public CompletableFuture<DataValue> read(VariableNode variable) {
        var future = new CompletableFuture<DataValue>();
        synchronized (pendingRequests) {
            pendingRequests.computeIfAbsent(new DeviceRequest.ReadRequest(variable), k -> new HashSet<>())
                    .add(future);
            pendingRequests.notify();
        }
        return future;
    }

    public CompletableFuture<Void> write(WritableVariableNode variable, Object value) {
        var future = new CompletableFuture<Void>();
        synchronized (pendingRequests) {
            pendingRequests.computeIfAbsent(new DeviceRequest.WriteRequest(variable, value), k -> new HashSet<>())
                    .add(future);
            pendingRequests.notify();
        }
        return future;
    }
}
