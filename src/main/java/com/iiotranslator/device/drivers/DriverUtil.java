/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers;

import com.iiotranslator.opc.VariableNode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class DriverUtil {
    public static DataValue convertValue(VariableNode variableNode, String value) {
        Variant variant;
        try {
            if (variableNode.getDataType().equals(Identifiers.Double)) {
                variant = new Variant(Double.parseDouble(value));
            } else if (variableNode.getDataType().equals(Identifiers.UInt16)) {
                variant = new Variant(Integer.parseInt(value));
            } else if (variableNode.getDataType().equals(Identifiers.Int16)) {
                variant = new Variant(Integer.parseInt(value));
            } else if (variableNode.getDataType().equals(Identifiers.UInt32)) {
                variant = new Variant(Long.parseLong(value));
            } else if (variableNode.getDataType().equals(Identifiers.Int32)) {
                variant = new Variant(Integer.parseInt(value));
            } else if (variableNode.getDataType().equals(Identifiers.UInt64)) {
                variant = new Variant(Long.parseLong(value));
            } else if (variableNode.getDataType().equals(Identifiers.Int64)) {
                variant = new Variant(Long.parseLong(value));
            } else if (variableNode.getDataType().equals(Identifiers.Boolean)) {
                variant = new Variant(Boolean.parseBoolean(value));
            } else {
                variant = new Variant(value);
            }
        } catch (NumberFormatException e) {
            log.trace("Could not convert value {} to number type {}", value, variableNode.getDataType());
            return new DataValue(StatusCodes.Bad_DecodingError);
        }
        return new DataValue(variant);
    }

    public static HttpClient createHttpClient(int timeout) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
    }
}
