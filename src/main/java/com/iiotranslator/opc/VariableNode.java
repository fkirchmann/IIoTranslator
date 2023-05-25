/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import lombok.Getter;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class VariableNode extends Node {
    @Getter
    private final NodeId dataType;

    VariableNode(OpcNamespace opcNamespace, String name, Node parent, NodeId dataType) {
        super(opcNamespace, name, parent);
        this.dataType = dataType;
    }

    public boolean isWritable() {
        return false;
    }

    @Override
    protected UaNode createUaNode() {
        return getOpcNamespace().createVariableNode(this);
    }

    @Override
    protected void registerChild(UaNode child) {
        throw new UnsupportedOperationException("VariableNode cannot have children");
    }
}
