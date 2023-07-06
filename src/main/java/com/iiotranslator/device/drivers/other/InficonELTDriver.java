/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.other;

import com.iiotranslator.device.Device;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class InficonELTDriver implements DeviceDriver {

    private final Map<VariableNode, DataValue> lastValues = new HashMap<>();

    private final Map<String, VariableNode> variables = new HashMap<>();

    @Override
    public void initialize(Device device, FolderNode deviceFolderNode) {
        var timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "3000"));

        IO.Options options = new IO.Options();
        options.transports = new String[] {WebSocket.NAME};
        options.reconnection = true;
        options.reconnectionAttempts = Integer.MAX_VALUE;
        options.reconnectionDelay = 1_000;
        options.reconnectionDelayMax = 3_000;
        options.timeout = timeout;

        addVariable(deviceFolderNode, "operatingUnitState", Identifiers.Int64);
        addVariable(deviceFolderNode, "operationSequence", Identifiers.Int64);
        addVariable(deviceFolderNode, "p1", Identifiers.Double);
        addVariable(deviceFolderNode, "p2", Identifiers.Double);
        addVariable(deviceFolderNode, "p3", Identifiers.Double);
        addVariable(deviceFolderNode, "trigger2Exceeded", Identifiers.Boolean);
        addVariable(deviceFolderNode, "trigger1Exceeded", Identifiers.Boolean);
        addVariable(deviceFolderNode, "rawIon", Identifiers.Double);
        addVariable(deviceFolderNode, "leakrateRawMbarls", Identifiers.Double);
        addVariable(deviceFolderNode, "deviceState", Identifiers.Int64);
        addVariable(deviceFolderNode, "operationModus", Identifiers.Int64);
        addVariable(deviceFolderNode, "timestamp", Identifiers.Int64);

        Socket socket = IO.socket(URI.create("http://" + device.getOption("hostname")), options);
        socket.on("MeasurementData", data -> {
            var json = (JSONObject) data[0];
            log.trace("Received measurement data, type {}: {}", json, data);

            var iterator = json.keys();
            while (iterator.hasNext()) {
                var key = iterator.next().toString();
                try {
                    var value = json.get(key);
                    setVariableValue(key, value);
                } catch (JSONException e) {
                    log.error("Error getting value for key {}", key, e);
                }
            }
        });
        log.trace("Connecting to Inficon ELT device");
        socket.connect();
        socket.on(Socket.EVENT_CONNECT, o -> log.trace("Connected to Inficon ELT device"));
        socket.on(Socket.EVENT_DISCONNECT, o -> {
            clearVariableValues();
            log.trace("Disconnected from Inficon ELT device: {}", o);
        });
        socket.on(Socket.EVENT_CONNECT_ERROR, o -> {
            clearVariableValues();
            log.trace("Error connecting to Inficon ELT device: {}", o);
        });
    }

    @Override
    public void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        requestQueue.forEach(request -> {
            var readRequest = (DeviceRequest.ReadRequest) request;
            listener.completeReadRequest(
                    readRequest, lastValues.getOrDefault((readRequest).getVariable(), new DataValue(StatusCode.BAD)));
        });
    }

    private void addVariable(FolderNode deviceFolderNode, String name, NodeId dataType) {
        variables.put(name, deviceFolderNode.addVariableReadOnly(name, dataType));
    }

    private void setVariableValue(String name, Object value) {
        var variable = variables.get(name);
        if (variable == null) {
            log.trace("Unknown variable name: {}", name);
            return;
        }

        if (value == null) {
            // ignore null values
        } else if (value instanceof Integer) {
            value = Long.valueOf((Integer) value);
        } else if (value instanceof String
                || value instanceof Boolean
                || value instanceof Long
                || value instanceof Double) {
            // accept the value as is, no conversion needed
        } else if (JSONObject.NULL.equals(value)) {
            value = null;
        } else {
            log.trace("Unknown value type for {}: class {}, value {}", name, value.getClass(), value);
            value = null;
        }
        var dataValue = new DataValue(new Variant(value));
        lastValues.put(variable, dataValue);
    }

    private void clearVariableValues() {
        lastValues.clear();
    }
}
