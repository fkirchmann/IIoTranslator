/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device;

import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.opc.WritableVariableNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

public class DeviceRequest {
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @Getter
    public static class ReadRequest extends DeviceRequest {
        private final VariableNode variable;
    }

    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    @Getter
    public static class WriteRequest extends DeviceRequest {
        private final WritableVariableNode variable;
        private final DataValue value;
    }
}
