/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers;

import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.opc.VariableNode;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * An abstract base class for drivers that do not need to process multiple requests at once.
 * <p>
 * This class provides a simpler interface for handling read and write requests by implementing single read and write
 * methods that are called synchronously.
 */
public interface NonBatchingDeviceDriver extends DeviceDriver {
    /**
     * This method is called when a read request is received. The driver should process the request and return the
     * result synchronously.
     *
     * @param variable The VariableNode to be read.
     * @return A DataValue object containing the result of the read operation.
     */
    DataValue read(VariableNode variable);

    /**
     * This method is called when a write request is received. The driver should process the request and write the value
     * to the specified variable synchronously. By default, this method throws an exception, as drivers that do not
     * support writes should not implement this method.
     *
     * @param variable The VariableNode to be written to.
     * @param value    The value to be written to the variable.
     */
    default void write(VariableNode variable, Object value) {
        throw new UnsupportedOperationException("Driver does not support writes");
    }

    default void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        for (DeviceRequest request : requestQueue) {
            if (request instanceof DeviceRequest.ReadRequest readRequest) {
                listener.completeReadRequestExceptionally(readRequest, read(readRequest.getVariable()));
            } else if (request instanceof DeviceRequest.WriteRequest writeRequest) {
                write(writeRequest.getVariable(), writeRequest.getValue());
                listener.completeWriteRequestExceptionally(writeRequest);
            }
        }
    }
}
