/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers.binder;

import com.iiotranslator.device.drivers.NonBatchingDeviceDriver;
import com.iiotranslator.device.drivers.DriverUtil;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.service.Device;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class BinderKBDriver implements NonBatchingDeviceDriver {
    private int timeout;
    private Device device;

    private final Map<VariableNode, String> variableMap = new HashMap<>();
    private final Set<VariableNode> convertKelvinToCelsius = new HashSet<>();

    @Override
    public void initialize(Device device, FolderNode folder) {
        this.device = device;
        this.timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "2000"));

        variableMap.put(folder.addVariableReadOnly("Communication Status", Identifiers.String), "10010010");
        variableMap.put(folder.addVariableReadOnly("Fan Speed", Identifiers.Double), "112000E1");
        var temperatureSetpoint = folder.addVariableReadOnly("Temperature Setpoint", Identifiers.Double);
        variableMap.put(temperatureSetpoint, "114000C0");
        convertKelvinToCelsius.add(temperatureSetpoint);
        var temperature = folder.addVariableReadOnly("Temperature", Identifiers.Double);
        variableMap.put(temperature, "11400080");
        convertKelvinToCelsius.add(temperature);
        var doorTemperature = folder.addVariableReadOnly("Door Temperature", Identifiers.Double);
        variableMap.put(doorTemperature, "11400082");
        convertKelvinToCelsius.add(doorTemperature);
    }

    @Override
    public DataValue readImpl(VariableNode variable) {
        if(!ensureConnected()) {
            return new DataValue(StatusCodes.Bad_NoCommunication);
        }
        try {
            var command = "CANIDGetValue:" + variableMap.get(variable);
            writer.write( command + "\r\n");
            writer.flush();
            // Discard the first line, it just contains the length of the following line
            reader.readLine();
            // Read the response to the command.
            String response = reader.readLine().substring(command.length() + 1);
            if(convertKelvinToCelsius.contains(variable)) {
                response = String.valueOf(Double.parseDouble(response) - 273.15);
            }
            return DriverUtil.convertValue(variable, response);
        } catch (IOException | NumberFormatException e) {
            log.trace("[{}]: Error reading from device", device.getName(), e);
            disconnect();
            return new DataValue(StatusCodes.Bad_CommunicationError);
        }
    }

    private Socket socket = null;
    private PrintWriter writer = null;
    private BufferedReader reader = null;

    private void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            log.trace("[{}]: Error closing socket", device.getName(), e);
        }
        socket = null;
        // As this method usually only gets called when an error occurs, wait a bit before trying to reconnect.
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean ensureConnected() {
        if(socket != null) {
            if(socket.isConnected()) {
                return true;
            } else {
                disconnect();
            }
        }
        try {
            socket = new Socket();
            socket.setSoTimeout(timeout);
            socket.connect(new InetSocketAddress(device.getOption("hostname"),
                    Integer.parseInt(device.getOptionOrDefault("port", "9000"))), timeout);
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            log.trace("[{}]: Error connecting to device", device.getName(), e);
            // cooldown
            disconnect();
            return false;
        }
    }
}
