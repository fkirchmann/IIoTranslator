/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers;

import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.device.NonBatchingDeviceDriver;
import com.iiotranslator.service.Device;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.Random;

@Slf4j
public class TestDriver extends NonBatchingDeviceDriver {

    private final int delay;

    public TestDriver(Device device, FolderNode deviceFolder) {
        super(device, deviceFolder);
        delay = Integer.parseInt(device.getOptionOrDefault("delay", "1000"));
    }

    @Override
    protected void createNodes(Device device, FolderNode deviceFolderNode) {
        deviceFolderNode.addVariableReadOnly("test", Identifiers.Double);
        var folder = deviceFolderNode.addFolder("folder");
        folder.addVariableReadOnly("foo", Identifiers.Double);
        folder.addVariableReadOnly("bar", Identifiers.Double);
    }

    @Override
    @SneakyThrows
    protected DataValue readImpl(VariableNode variable) {
        Thread.sleep(delay);
        return new DataValue(new Variant(new Random().nextDouble()));
    }
}
