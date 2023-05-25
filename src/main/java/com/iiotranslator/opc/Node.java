/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

@Getter
@EqualsAndHashCode(of = "path")
public abstract class Node {
    Node(OpcNamespace opcNamespace, @NonNull String name, @NonNull Node parent) {
        if (name.contains("/")) {
            throw new IllegalArgumentException("Node name cannot contain '/'");
        }
        this.name = name;
        this.parent = parent;
        this.path = Stream.concat(parent.getPath().stream(), Stream.of(name)).toList();
        this.pathString = (parent.pathString.isEmpty() ? "" : (parent.getPathString() + "/")) + name;
        this.opcNamespace = opcNamespace;
        this.uaNode = createUaNode();
        this.parent.registerChild(this.uaNode);
    }

    Node(OpcNamespace opcNamespace) {
        this.name = "";
        this.parent = null;
        this.path = List.of();
        this.pathString = "";
        this.opcNamespace = opcNamespace;
        this.uaNode = createUaNode();
    }

    private final Node parent;
    private final String name;
    private final List<String> path;
    private final String pathString;

    @Getter(AccessLevel.PACKAGE)
    private final OpcNamespace opcNamespace;

    @Getter(AccessLevel.PACKAGE)
    private final UaNode uaNode;

    protected abstract UaNode createUaNode();

    protected abstract void registerChild(UaNode child);

    public Node getParent() {
        return parent;
    }

    public boolean isParentOf(Node node) {
        return node.getPathString().startsWith(getPathString());
    }

    public boolean isChildOf(Node node) {
        return getPathString().startsWith(node.getPathString());
    }
}
