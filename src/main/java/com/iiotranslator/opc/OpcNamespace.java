/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilterContext;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

@Slf4j
public class OpcNamespace extends ManagedNamespaceWithLifecycle {
    public static final String NAMESPACE_URI = "urn:com:iiotranslator:opcuans";

    private final OpcServer server;
    private final SubscriptionModel subscriptionModel;

    @Getter
    private final CompletableFuture<RootNode> rootNodeCompletableFuture;

    private final Map<VariableNode, DataValue> variableValues = new ConcurrentHashMap<>();

    OpcNamespace(@NonNull OpcServer server, @NonNull CompletableFuture<RootNode> rootNodeCompletableFuture) {
        super(server.getUaServer(), NAMESPACE_URI);
        this.server = server;
        this.rootNodeCompletableFuture = rootNodeCompletableFuture;
        subscriptionModel = new SubscriptionModel(server.getUaServer(), this);
        getLifecycleManager().addLifecycle(subscriptionModel);
        // Signals that nodes can be created
        getLifecycleManager().addStartupTask(() -> rootNodeCompletableFuture.complete(new RootNode(this)));
    }

    @Synchronized
    void registerRootChildNode(@NonNull UaNode uaNode) {
        uaNode.addReference(
                new Reference(uaNode.getNodeId(), Identifiers.Organizes, Identifiers.ObjectsFolder.expanded(), false));
    }

    @Synchronized
    void registerChildNode(@NonNull UaNode uaNode, @NonNull UaFolderNode parent) {
        parent.addOrganizes(uaNode);
    }

    @Synchronized
    UaVariableNode createVariableNode(@NonNull VariableNode variableNode) {
        var uaVariableNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(variableNode.getPathString()))
                .setAccessLevel(variableNode.isWritable() ? AccessLevel.READ_WRITE : AccessLevel.READ_ONLY)
                .setBrowseName(newQualifiedName(variableNode.getName()))
                .setDisplayName(LocalizedText.english(variableNode.getName()))
                .setDataType(variableNode.getDataType())
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();
        // uaVariableNode.setAllowNulls(true);
        var writeLock = new Object();
        AtomicBoolean isWritingReadValue = new AtomicBoolean(false);

        var defaultValue = new DataValue(StatusCodes.Bad_WaitingForInitialData);
        uaVariableNode.setValue(defaultValue);

        uaVariableNode.getFilterChain().addFirst(new AttributeFilter() {
            @Override
            public Object getAttribute(AttributeFilterContext.GetAttributeContext ctx, AttributeId attributeId) {
                if (attributeId == AttributeId.Value) {
                    server.getVariableNodeAccessor().read(variableNode).thenAccept(value -> {
                        variableValues.put(variableNode, value);
                        synchronized (writeLock) {
                            isWritingReadValue.set(true);
                            uaVariableNode.setValue(value);
                            isWritingReadValue.set(false);
                        }
                    });
                    return variableValues.getOrDefault(variableNode, defaultValue);
                }
                return ctx.getAttribute(attributeId);
            }
        });
        uaVariableNode.getFilterChain().addLast(AttributeFilters.setValue((ctx, value) -> {
            synchronized (writeLock) {
                if (isWritingReadValue.get()) {
                    return;
                }
                if (variableNode instanceof WritableVariableNode writableVariableNode) {
                    if (variableNode.isWritable()) {
                        server.getVariableNodeAccessor().writeSync(writableVariableNode, value);
                    } else {
                        log.warn("Attempt to write to read-only node: {}", variableNode.getPathString());
                    }
                }
            }
        }));
        getNodeManager().addNode(uaVariableNode);
        return uaVariableNode;
    }

    @Synchronized
    UaFolderNode createFolderNode(@NonNull FolderNode node) {
        var folder = new UaFolderNode(
                getNodeContext(),
                newNodeId(node.getPathString()),
                newQualifiedName(node.getName()),
                LocalizedText.english(node.getName()));
        getNodeManager().addNode(folder);
        return folder;
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }
}
