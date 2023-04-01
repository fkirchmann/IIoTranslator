/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.opc;

import lombok.Synchronized;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FolderNode extends Node {
    private final List<Node> children = Collections.synchronizedList(new ArrayList<>());

    private FolderNode(String name, FolderNode parent) {
        super(name, parent);
    }

    FolderNode() {
        super();
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
        var child = new VariableNode(name, this, type);
        addChild(child);
        return child;
    }

    @Synchronized("children")
    public WritableVariableNode addVariableReadWrite(String name, NodeId type) {
        var child = new WritableVariableNode(name, this, type);
        addChild(child);
        return child;
    }

    @Synchronized("children")
    public FolderNode addFolder(String name) {
        var childFolder = new FolderNode(name, this);
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
        if(getChild(child.getName()) != null) {
            throw new IllegalArgumentException("Node with path " + child.getPath() + " already exists!");
        }
        children.add(child);
    }
}
