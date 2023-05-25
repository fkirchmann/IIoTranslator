/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Synchronized;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class FolderNode extends Node {
    private final List<Node> children = Collections.synchronizedList(new ArrayList<>());

    private FolderNode(OpcNamespace opcNamespace, String name, FolderNode parent) {
        super(opcNamespace, name, parent);
    }

    FolderNode(OpcNamespace opcNamespace) {
        super(opcNamespace);
    }

    @Override
    protected UaNode createUaNode() {
        return getOpcNamespace().createFolderNode(this);
    }

    @Override
    protected void registerChild(UaNode child) {
        getOpcNamespace().registerChildNode(child, (UaFolderNode) getUaNode());
    }

    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<VariableNode> getChildVariables() {
        return children.stream()
                .filter(VariableNode.class::isInstance)
                .map(VariableNode.class::cast)
                .toList();
    }

    @Synchronized("children")
    public List<FolderNode> getChildFolders() {
        return children.stream()
                .filter(FolderNode.class::isInstance)
                .map(FolderNode.class::cast)
                .toList();
    }

    @Synchronized("children")
    public VariableNode addVariableReadOnly(String name, NodeId type) {
        var child = new VariableNode(getOpcNamespace(), name, this, type);
        addChild(child);
        return child;
    }

    @Synchronized("children")
    public WritableVariableNode addVariableReadWrite(String name, NodeId type) {
        var child = new WritableVariableNode(getOpcNamespace(), name, this, type);
        addChild(child);
        return child;
    }

    @Synchronized("children")
    public FolderNode addFolder(String name) {
        var childFolder = new FolderNode(getOpcNamespace(), name, this);
        addChild(childFolder);
        return childFolder;
    }

    @Synchronized("children")
    public Node getChild(String name) {
        return children.stream()
                .filter(child -> child.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Synchronized("children")
    private void addChild(Node child) {
        if (getChild(child.getName()) != null) {
            throw new IllegalArgumentException("Node with path " + child.getPath() + " already exists!");
        }
        children.add(child);
    }
}
