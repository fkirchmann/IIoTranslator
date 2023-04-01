/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.opc;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

@Getter
@EqualsAndHashCode(of="path")
public class Node {
    Node(String name, Node parent) {
        if(name.contains("/")) {
            throw new IllegalArgumentException("Node name cannot contain '/'");
        }
        this.name = name;
        this.parent = parent;
        this.path = Stream.concat(parent.getPath().stream(), Stream.of(name)).toList();
        this.pathString = (parent.pathString.isEmpty() ? "" : (parent.getPathString() + "/")) + name;
    }

    Node() {
        this.name = "";
        this.parent = null;
        this.path = List.of();
        this.pathString = "";
    }

    private final Node parent;
    private final String name;
    private final List<String> path;
    private final String pathString;

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
