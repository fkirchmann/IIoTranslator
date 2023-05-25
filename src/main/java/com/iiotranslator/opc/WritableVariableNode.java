/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class WritableVariableNode extends VariableNode {
    WritableVariableNode(OpcNamespace opcNamespace, String name, Node parent, NodeId type) {
        super(opcNamespace, name, parent, type);
    }

    @Override
    public boolean isWritable() {
        return true;
    }
}
