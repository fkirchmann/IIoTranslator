/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.opc;

import lombok.Getter;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class VariableNode extends Node {
    @Getter
    private final NodeId dataType;

    VariableNode(String name, Node parent, NodeId dataType) {
        super(name, parent);
        this.dataType = dataType;
    }

    public boolean isWritable() {
        return false;
    }
}
