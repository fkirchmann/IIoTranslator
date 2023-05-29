/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.weiss;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.iiotranslator.device.Device;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.device.drivers.DriverUtil;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * NOTE: This driver is an abandoned prototype, designed to use reactive websockets to connect to
 * the Weiss LabEvent ovens. It is not working, and is not used in production.
 */
@Slf4j
public class WeissLabEventDriverLegacyPrototype implements DeviceDriver {
    private Device device;
    private String hostname, user, password;
    private int port, timeout;

    public WeissLabEventDriverLegacyPrototype(Device device, FolderNode deviceFolder) {}

    private final Map<String, VariableNode> variables = new HashMap<>();
    private final Map<VariableNode, DataValue> values = new HashMap<>();

    @Override
    public void initialize(Device device, FolderNode folder) {
        this.device = device;
        hostname = device.getOption("hostname");
        user = device.getOptionOrDefault("user", "admin");
        password = device.getOptionOrDefault("password", "admin");
        port = Integer.parseInt(device.getOptionOrDefault("port", "443"));
        timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "2000"));
        if (user.contains(",") || password.contains(",")) {
            throw new IllegalArgumentException("User or password must not contain a comma (,)");
        }

        addVariableNode(folder.addVariableReadOnly("ManLock", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CHB.UserLock", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PrgStartStop", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("SV.1.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("SV.2.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.1.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.2.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.3.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.4.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.5.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.6.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.7.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.8.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("DO.9.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("MV.1.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("MV.1.ActualValue", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CV.FormatValues", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.Owner", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.StartTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.FinishTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CHB.ManRuntime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.ProfileName", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("ManualModeTotalItem", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.SegmentTotalTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.SegementRemainTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.ProfileTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.RemainTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.ActiveTime", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.ActLoops", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.TotalLoops", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.CyclesAct", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("PG.CyclesSet", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CV.1.ActualValue", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CV.1.TolMin", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CV.1.TolMax", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CV.1.State", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("CV.1.SetPoint", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("AV_Temp", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("MC.2.AV_Abs", Identifiers.String));
        addVariableNode(folder.addVariableReadOnly("MC.2.Limit", Identifiers.String));
    }

    private void addVariableNode(VariableNode node) {
        variables.put(node.getName(), node);
    }

    @Override
    @SneakyThrows(InterruptedException.class)
    public void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        // TODO process the requestQueue
        connectAndProcess(listener);
        Thread.sleep(timeout);
    }

    private Sinks.Many<String> sendSink;

    private void connectAndProcess(DeviceRequestCompletionListener listener) {
        sendSink = Sinks.many().multicast().onBackpressureBuffer();
        send("{\"cmd\":\"ver\",\"data\":{\"major\":1,\"minor\":1,\"patch\":5}}");
        send("app");
        send("user:admin,admin,webseason");
        // subscribe to all variables
        send(Command.subscribe(variables.keySet().toArray(new String[0])));
        WebSocketClient client = new ReactorNettyWebSocketClient(DriverUtil.createHttpClient(timeout));
        client.execute(URI.create("ws://" + hostname + ":" + port), new WebSocketHandler() {
                    @Override
                    public List<String> getSubProtocols() {
                        return List.of("smarthmi-connect");
                    }

                    @Override
                    public Mono<Void> handle(WebSocketSession session) {
                        return Mono.zip(
                                        // Take outgoing messages from the sink and send them to the server
                                        sendSink.asFlux()
                                                .map(message -> {
                                                    log.trace("[{}] Sending: {}", device.getName(), message);
                                                    return session.textMessage(message);
                                                })
                                                .then(),
                                        // Receive messages from the server and handle them
                                        session.receive()
                                                .map(WebSocketMessage::getPayloadAsText)
                                                .doOnNext(WeissLabEventDriverLegacyPrototype.this::handleMessage)
                                                .then())
                                .then(session.close());
                    }
                })
                .block();
        log.trace("[{}]: Connection closed", device.getName());
    }

    private static final Pattern MESSAGE_VALUE_PATTERN = Pattern.compile("^@val:(.+):(.*)$");
    private final Gson gson = new Gson();

    private void handleMessage(String message) {
        log.trace("[{}]: Received message: {}", device.getName(), message);
        Matcher valMatcher = MESSAGE_VALUE_PATTERN.matcher(message);
        if (valMatcher.matches()) {
            var variable = variables.get(valMatcher.group(1));
            String value = valMatcher.group(2);
            if (variable != null) {
                values.put(variable, DriverUtil.convertValue(variable, value));
            }
        }
        // parse message JSON
        try {
            @NonNull var multiCommand = gson.fromJson(message, Command.class);
            if (multiCommand.getCmd().equals("multi")) {
                for (String command : multiCommand.getData()) {
                    handleMessage(command);
                }
            }
        } catch (JsonSyntaxException | NullPointerException ignored) {
            log.trace("[{}]: Ignoring message: \"{}\"", device.getName(), message);
        }
    }

    private void send(String s) {
        sendSink.tryEmitNext(s);
    }

    private void send(Command command) {
        send(gson.toJson(command));
    }

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
}
