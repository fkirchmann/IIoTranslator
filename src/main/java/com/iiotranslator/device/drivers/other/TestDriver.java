/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers.other;

import com.iiotranslator.device.drivers.NonBatchingDeviceDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.service.Device;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.Random;

@Slf4j
public class TestDriver implements NonBatchingDeviceDriver {
    private int delay;

    @Override
    public void initialize(Device device, FolderNode deviceFolderNode) {
        delay = Integer.parseInt(device.getOptionOrDefault("delay", "1000"));

        deviceFolderNode.addVariableReadOnly("test", Identifiers.Double);
        var folder = deviceFolderNode.addFolder("ExampleFolder");
        folder.addVariableReadOnly("Variable1", Identifiers.Double);
        folder.addVariableReadOnly("Variable2", Identifiers.Double);
        log.info("[{}]: Test Device initialized", device.getName());
    }

    @Override
    @SneakyThrows
    public DataValue read(VariableNode variable) {
        Thread.sleep(delay);
        return new DataValue(new Variant(new Random().nextDouble()));
    }
}
