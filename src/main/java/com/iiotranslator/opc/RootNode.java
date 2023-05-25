/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

public class RootNode extends FolderNode {
    public RootNode(OpcNamespace opcNamespace) {
        super(opcNamespace);
    }

    @Override
    protected UaNode createUaNode() {
        return null;
    }

    @Override
    protected void registerChild(UaNode child) {
        getOpcNamespace().registerRootChildNode(child);
    }
}
