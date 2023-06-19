/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.binder;

import com.iiotranslator.device.Device;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.device.drivers.DriverUtil;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class BinderKBFDriver implements DeviceDriver {
    private Device device;
    private int timeout;
    private WebClient client;
    private String uriPrefix;
    private Map<String, VariableNode> variableNodes;

    private final Map<VariableNode, DataValue> variableValues = new HashMap<>();

    @Override
    public void initialize(Device device, FolderNode deviceFolderNode) {
        this.device = device;
        String hostname = device.getOption("hostname");
        timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "2000"));
        uriPrefix = "http://" + hostname + "/UE/ZIP/";
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        var variableNodes = new HashMap<String, VariableNode>();
        // variableNodes.put("2.175.0.5.0", deviceFolderNode.addVariableReadOnly("System Time", Identifiers.String)); //
        // Example: "15:32:41  10.01.2023"
        variableNodes.put("2.416.1.0.0", deviceFolderNode.addVariableReadOnly("Temperature", Identifiers.Double));
        variableNodes.put(
                "2.227.0.0.0", deviceFolderNode.addVariableReadOnly("Temperature Setpoint", Identifiers.Double));
        variableNodes.put("2.416.3.0.0", deviceFolderNode.addVariableReadOnly("Humidity", Identifiers.Double));
        variableNodes.put("2.413.1.0.0", deviceFolderNode.addVariableReadOnly("Humidity Setpoint", Identifiers.Double));
        variableNodes.put(
                "2.227.2.0.0", deviceFolderNode.addVariableReadOnly("Fan Speed Setpoint", Identifiers.UInt16));
        this.variableNodes = variableNodes;
    }

    @Override
    public void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        variableValues.clear();
        try {
            var input = client.get()
                    .uri(new URI(uriPrefix + String.join("//", variableNodes.keySet())))
                    .retrieve()
                    .bodyToMono(ByteArrayResource.class)
                    .block()
                    .getByteArray();
            Inflater decompresser = new Inflater();
            decompresser.setInput(input);
            byte[] result = new byte[1024];
            int resultLength = decompresser.inflate(result);
            decompresser.end();
            String output = new String(result, 0, resultLength, StandardCharsets.UTF_8);
            log.trace("[{}]: Response: {}", device.getName(), output);
            output = output.replace("<Wert>", "").replace("</Wert>", "");
            for (String keyValue : output.split(Pattern.quote("//"))) {
                try {
                    String[] keyValuePair = keyValue.split(Pattern.quote("="));
                    if (keyValuePair.length == 2) {
                        String key = keyValuePair[0];
                        String value = keyValuePair[1];
                        VariableNode variableNode = variableNodes.get(key);
                        log.trace("[{}]: {} = {}", device.getName(), key, value);
                        if (variableNode != null) {
                            if (value.equals("-----")) {
                                variableValues.put(variableNode, new DataValue(StatusCode.GOOD));
                            } else {
                                variableValues.put(variableNode, DriverUtil.convertValue(variableNode, value));
                            }
                        } else {
                            log.warn("[{}]: Unknown variable: {}", device.getName(), key);
                        }
                    }
                } catch (Exception e) {
                    log.trace("[{}]: Error parsing response: {}", device.getName(), keyValue, e);
                }
            }
        } catch (Exception e) {
            log.trace("[{}]: Error reading from device", device.getName(), e);
        } finally {
            for (DeviceRequest request : requestQueue) {
                var readRequest = (DeviceRequest.ReadRequest) request;
                var variable = readRequest.getVariable();
                listener.completeReadRequestExceptionally(
                        readRequest, variableValues.getOrDefault(variable, new DataValue(StatusCode.BAD)));
            }
        }
    }
}
