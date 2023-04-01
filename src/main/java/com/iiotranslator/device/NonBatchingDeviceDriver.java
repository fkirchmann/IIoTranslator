/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device;

import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.service.Device;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.util.List;

@Slf4j
public abstract class NonBatchingDeviceDriver extends DeviceDriver {
    protected NonBatchingDeviceDriver(Device device, FolderNode deviceFolder) {
        super(device, deviceFolder);
    }

    protected void process(List<DeviceRequest> requestQueue) {
        for (DeviceRequest request : requestQueue) {
            if (request instanceof DeviceRequest.ReadRequest readRequest) {
                completeReadRequest(readRequest, readImpl(readRequest.getVariable()));
            } else if (request instanceof DeviceRequest.WriteRequest writeRequest) {
                writeImpl(writeRequest.getVariable(), writeRequest.getValue());
                completeWriteRequest(writeRequest);
            }
        }
    }

    protected abstract DataValue readImpl(VariableNode variable);
    protected void writeImpl(VariableNode variable, Object value) {
        log.error("A write was attempted for variable {}, but the driver does not support writes", variable.getPath());
        throw new UnsupportedOperationException("Driver does not support writes");
    }
}
