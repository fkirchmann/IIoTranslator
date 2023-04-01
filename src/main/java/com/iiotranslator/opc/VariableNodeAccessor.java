/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.opc;

import lombok.SneakyThrows;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.util.concurrent.CompletableFuture;

public interface VariableNodeAccessor {
    CompletableFuture<DataValue> read(VariableNode variable);

    @SneakyThrows
    default DataValue readSync(VariableNode variable) {
        return read(variable).get();
    }

    CompletableFuture<Void> write(WritableVariableNode variable, Object value);

    @SneakyThrows
    default void writeSync(WritableVariableNode variable, Object value) {
        write(variable, value).get();
    }
}
