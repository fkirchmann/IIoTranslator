/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

public interface DeviceRequestCompletionListener {
    void completeReadRequestExceptionally(DeviceRequest.ReadRequest request, DataValue value);

    default void completeReadRequestExceptionally(DeviceRequest.ReadRequest request, Exception e) {
        completeReadRequestExceptionally(request, new DataValue(StatusCodes.Bad_InternalError));
    }

    void completeWriteRequestExceptionally(DeviceRequest.WriteRequest request);

    void completeWriteRequestExceptionally(DeviceRequest.WriteRequest request, Exception e);
}
