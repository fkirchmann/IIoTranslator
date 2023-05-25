/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.weiss;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.iiotranslator.device.drivers.DriverUtil;
import com.iiotranslator.device.drivers.NonBatchingDeviceDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.service.Device;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;

/**
 * Supports the Weiss LabEvent ovens.
 */
@Slf4j
public class WeissLabEventDriver implements NonBatchingDeviceDriver {
    private Device device;

    private String hostname, user, password;
    private int port, timeout;

    private URI uri;

    private final Map<String, VariableNode> variables = new HashMap<>();
    private final Map<VariableNode, DataValue> values = new ConcurrentHashMap<>();

    @Override
    public void initialize(Device device, FolderNode folder) {
        this.device = device;
        hostname = device.getOption("hostname");
        port = Integer.parseInt(device.getOptionOrDefault("port", "443"));
        uri = URI.create("ws://" + hostname + ":" + port);
        user = device.getOptionOrDefault("user", "admin");
        password = device.getOptionOrDefault("password", "admin");
        timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "2000"));
        if (user.contains(",") || password.contains(",")) {
            throw new IllegalArgumentException("User or password must not contain a comma (,)");
        }

        addVariableNode(folder.addVariableReadOnly("AV_Temp", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CHB.ManRuntime", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("CHB.UserLock", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("CV.1.ActualValue", Identifiers.Double));
        addVariableNode(folder.addVariableReadOnly("CV.1.SetPoint", Identifiers.Double));
        addVariableNode(folder.addVariableReadOnly("CV.1.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("CV.1.TolMax", Identifiers.Double));
        addVariableNode(folder.addVariableReadOnly("CV.1.TolMin", Identifiers.Double));
        addVariableNode(folder.addVariableReadOnly("CV.FormatValues", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.1.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.2.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.3.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.4.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.5.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.6.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.7.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.8.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("DO.9.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("MC.2.AV_Abs", Identifiers.Double));
        addVariableNode(folder.addVariableReadOnly("MC.2.Limit", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("MV.1.ActualValue", Identifiers.Double));
        addVariableNode(folder.addVariableReadOnly("MV.1.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("ManLock", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("ManualModeTotalItem", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("PG.ActLoops", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("PG.ActiveTime", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("PG.CyclesAct", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("PG.CyclesSet", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("PG.FinishTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.Owner", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.ProfileName", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.ProfileTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.RemainTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.SegementRemainTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.SegmentTotalTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.StartTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.TotalLoops", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("PrgStartStop", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("SV.1.State", Identifiers.Int64));
        addVariableNode(folder.addVariableReadOnly("SV.2.State", Identifiers.Int64));
    }

    private void addVariableNode(VariableNode node) {
        variables.put(node.getName(), node);
    }

    @Override
    public DataValue read(VariableNode variable) {
        if (client != null
                && (client.getReadyState() == ReadyState.OPEN
                        || client.getReadyState() == ReadyState.NOT_YET_CONNECTED)) {
            if (client.isOpen()) {
                return values.getOrDefault(variable, new DataValue(StatusCode.BAD));
            } else {
                return new DataValue(StatusCode.BAD);
            }
        } else {
            if (client != null) {
                client.close();
            }
            client = new WeissLabEventWebsocketClient();
            client.connect();
            return new DataValue(StatusCode.BAD);
        }
    }

    private WeissLabEventWebsocketClient client = null;

    private static final Pattern MESSAGE_VALUE_PATTERN = Pattern.compile("^@val:([^:]+):(.*)$");

    @Data
    public static class Command {
        private final String cmd;
        private final String[] data;

        public static Command subscribe(String... variables) {
            return new Command("sub", variables);
        }

        public static Command unsubscribe(String... variables) {
            return new Command("unsub", variables);
        }
    }

    private class WeissLabEventWebsocketClient extends org.java_websocket.client.WebSocketClient {
        private final Gson gson = new Gson();

        private WeissLabEventWebsocketClient() {
            super(
                    WeissLabEventDriver.this.uri,
                    new Draft_6455(
                            Collections.emptyList(), Collections.singletonList(new Protocol("smarthmi-connect"))),
                    null,
                    timeout);
            this.setConnectionLostTimeout(timeout);
        }

        @Override
        @SneakyThrows(IOException.class)
        public void onOpen(ServerHandshake handshakedata) {
            getSocket().setSoTimeout(timeout);
            log.debug("[{}] Connected", device.getName());
            send("{\"cmd\":\"ver\",\"data\":{\"major\":1,\"minor\":1,\"patch\":5}}");
            send("app");
            send("user:admin,admin,webseason");
            send(Command.unsubscribe("Systemzeit"));
            send(Command.subscribe(variables.keySet().toArray(new String[0])));
        }

        // override send method
        @Override
        public void send(String message) {
            log.trace("[{}] Send: {}", this, message);
            super.send(message);
        }

        public void send(Command command) {
            send(gson.toJson(command));
        }

        @SneakyThrows(InterruptedException.class)
        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (remote) {
                log.debug("[{}] closed with exit code {}, reason: {}", this, code, reason);
                closeBlocking();
            }
        }

        @Override
        public void onMessage(String message) {
            log.trace("[{}]: Receive: {}", device.getName(), message);
            if (message.startsWith("@item:") || message.startsWith("@user:") || message.startsWith("@app:")) {
                return; // ignore
            }
            Matcher valMatcher = MESSAGE_VALUE_PATTERN.matcher(message);
            if (valMatcher.matches()) {
                var variable = variables.get(valMatcher.group(1));
                String value = valMatcher.group(2);
                if (variable != null) {
                    values.put(variable, DriverUtil.convertValue(variable, value));
                } else {
                    log.debug("[{}] Ignoring unknown variable: {}", device.getName(), valMatcher.group(1));
                }
            } else {
                // parse message JSON
                try {
                    @NonNull var jsonMessage = gson.fromJson(message, Command.class);
                    if (jsonMessage.getCmd().equals("multi")) {
                        for (String command : jsonMessage.getData()) {
                            onMessage(command);
                        }
                    } else {
                        log.debug("[{}] Ignoring unknown JSON message: {}", device.getName(), message);
                    }
                } catch (JsonSyntaxException | NullPointerException ignored) {
                    log.debug("[{}]: Could not parse message, ignoring: \"{}\"", device.getName(), message);
                }
            }
        }

        @Override
        public void onError(Exception ex) {
            log.debug("[{}] A WebSocket error occurred:", device.getName(), ex);
            close();
        }
    }
}
