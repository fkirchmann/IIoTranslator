/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.opc;

import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilterContext;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class OpcNamespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:com:iiotranslator:opcuans";

    private static final Object[][] STATIC_SCALAR_NODES = new Object[][]{
        {"Boolean", Identifiers.Boolean, new Variant(false)},
        {"Byte", Identifiers.Byte, new Variant(Unsigned.ubyte(0x00))},
        {"SByte", Identifiers.SByte, new Variant((byte) 0x00)},
        {"Integer", Identifiers.Integer, new Variant(32)},
        {"Int16", Identifiers.Int16, new Variant((short) 16)},
        {"Int32", Identifiers.Int32, new Variant(32)},
        {"Int64", Identifiers.Int64, new Variant(64L)},
        {"UInteger", Identifiers.UInteger, new Variant(Unsigned.uint(32))},
        {"UInt16", Identifiers.UInt16, new Variant(Unsigned.ushort(16))},
        {"UInt32", Identifiers.UInt32, new Variant(Unsigned.uint(32))},
        {"UInt64", Identifiers.UInt64, new Variant(Unsigned.ulong(64L))},
        {"Float", Identifiers.Float, new Variant(3.14f)},
        {"Double", Identifiers.Double, new Variant(3.14d)},
        {"String", Identifiers.String, new Variant("string value")},
        {"DateTime", Identifiers.DateTime, new Variant(DateTime.now())},
        {"Guid", Identifiers.Guid, new Variant(UUID.randomUUID())},
        {"ByteString", Identifiers.ByteString, new Variant(new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04}))},
        {"XmlElement", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>"))},
        {"LocalizedText", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text"))},
        {"QualifiedName", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg"))},
        {"NodeId", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd"))},
        {"Variant", Identifiers.BaseDataType, new Variant(32)},
        {"Duration", Identifiers.Duration, new Variant(1.0)},
        {"UtcTime", Identifiers.UtcTime, new Variant(DateTime.now())},
    };

    private static final Object[][] STATIC_ARRAY_NODES = new Object[][]{
        {"BooleanArray", Identifiers.Boolean, false},
        {"ByteArray", Identifiers.Byte, Unsigned.ubyte(0)},
        {"SByteArray", Identifiers.SByte, (byte) 0x00},
        {"Int16Array", Identifiers.Int16, (short) 16},
        {"Int32Array", Identifiers.Int32, 32},
        {"Int64Array", Identifiers.Int64, 64L},
        {"UInt16Array", Identifiers.UInt16, Unsigned.ushort(16)},
        {"UInt32Array", Identifiers.UInt32, Unsigned.uint(32)},
        {"UInt64Array", Identifiers.UInt64, Unsigned.ulong(64L)},
        {"FloatArray", Identifiers.Float, 3.14f},
        {"DoubleArray", Identifiers.Double, 3.14d},
        {"StringArray", Identifiers.String, "string value"},
        {"DateTimeArray", Identifiers.DateTime, DateTime.now()},
        {"GuidArray", Identifiers.Guid, UUID.randomUUID()},
        {"ByteStringArray", Identifiers.ByteString, new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04})},
        {"XmlElementArray", Identifiers.XmlElement, new XmlElement("<a>hello</a>")},
        {"LocalizedTextArray", Identifiers.LocalizedText, LocalizedText.english("localized text")},
        {"QualifiedNameArray", Identifiers.QualifiedName, new QualifiedName(1234, "defg")},
        {"NodeIdArray", Identifiers.NodeId, new NodeId(1234, "abcd")}
    };

    private final SubscriptionModel subscriptionModel;

    @Getter
    private final RootNode rootNode;

    private final VariableNodeAccessor variableNodeAccessor;

    private final Map<VariableNode, DataValue> variableValues = new ConcurrentHashMap<>();

    OpcNamespace(@NonNull OpcUaServer server, @NonNull RootNode rootNode,
                 @NonNull VariableNodeAccessor variableNodeAccessor) {
        super(server, NAMESPACE_URI);
        this.variableNodeAccessor = variableNodeAccessor;
        this.rootNode = rootNode;
        subscriptionModel = new SubscriptionModel(server, this);
        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(() -> createAndAddNodes(rootNode));
    }

    private void createAndAddNodes(RootNode rootNode) {
        List<FolderNode> folderNodes = rootNode.getChildFolders();
        log.debug("Creating {} folders", folderNodes.size());
        List<UaFolderNode> uaFolderNodes = folderNodes.stream().map(this::createNode).toList();
        List<UaVariableNode> uaVariableNodes = rootNode.getChildVariables().stream().map(this::createNode).toList();
        log.debug("Created {} variable nodes", uaVariableNodes.size());
        log.debug("Created {} folder nodes", uaFolderNodes.size());

        Streams.concat(uaVariableNodes.stream(), uaFolderNodes.stream())
                .forEach(uaNode -> uaNode.addReference(new Reference(
                        uaNode.getNodeId(),
                        Identifiers.Organizes,
                        Identifiers.ObjectsFolder.expanded(),
                        false
                )));

        for(int i = 0; i < folderNodes.size(); i++) {
            createAndAddChildren(folderNodes.get(i), uaFolderNodes.get(i));
            log.debug("Created and added folder node: {}", folderNodes.get(i).getName());
        }
    }

    private void createAndAddChildren(FolderNode folderNode, UaFolderNode uaFolderNode) {
        var folderNodes = folderNode.getChildFolders();
        var uaFolderNodes = folderNodes.stream().map(this::createNode).toList();
        var uaVariableNodes = folderNode.getChildVariables().stream().map(this::createNode).toList();
        log.debug("Created {} variable nodes", uaVariableNodes.size());

        Streams.concat(uaVariableNodes.stream(), uaFolderNodes.stream()).forEach(uaFolderNode::addOrganizes);

        for(int i = 0; i < folderNodes.size(); i++) {
            createAndAddChildren(folderNodes.get(i), uaFolderNodes.get(i));
        }
    }

    private UaVariableNode createNode(VariableNode variableNode) {
        var uaVariableNode = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
            .setNodeId(newNodeId(variableNode.getPathString()))
            .setAccessLevel(variableNode.isWritable() ? AccessLevel.READ_WRITE : AccessLevel.READ_ONLY)
            .setBrowseName(newQualifiedName(variableNode.getName()))
            .setDisplayName(LocalizedText.english(variableNode.getName()))
            .setDataType(variableNode.getDataType())
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();
        //uaVariableNode.setAllowNulls(true);
        var writeLock = new Object();
        AtomicBoolean isWritingReadValue = new AtomicBoolean(false);

        var defaultValue = new DataValue(StatusCodes.Bad_WaitingForInitialData);
        uaVariableNode.setValue(defaultValue);

        uaVariableNode.getFilterChain().addFirst(new AttributeFilter() {
            @Override
            public Object getAttribute(AttributeFilterContext.GetAttributeContext ctx, AttributeId attributeId) {
                if (attributeId == AttributeId.Value) {
                    variableNodeAccessor.read(variableNode).thenAccept(value -> {
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
        uaVariableNode.getFilterChain().addLast(
            AttributeFilters.setValue(
                (ctx, value) -> {
                    synchronized (writeLock) {
                        if(isWritingReadValue.get()) {
                            return;
                        }
                        if (variableNode instanceof WritableVariableNode writableVariableNode) {
                            if(variableNode.isWritable()) {
                                variableNodeAccessor.writeSync(writableVariableNode, value);
                            } else {
                                log.warn("Attempt to write to read-only node: {}",
                                        variableNode.getPathString());
                            }
                        }
                    }
                }
            )
        );
        getNodeManager().addNode(uaVariableNode);
        return uaVariableNode;
    }

    private UaFolderNode createNode(FolderNode node) {
        var folder = new UaFolderNode(getNodeContext(),
                newNodeId(node.getPathString()),
                newQualifiedName(node.getName()),
                LocalizedText.english(node.getName())
        );
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
