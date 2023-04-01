/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers.keyence;

import com.iiotranslator.device.DeviceDriver;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.service.Device;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * This driver supports Keyence's MK-U6000 industrial ink-jet printer.
 *
 * TODO: This driver is incomplete and untested.
 */
@Slf4j
public class KeyenceDriver extends DeviceDriver {
    private final int timeout;

    public KeyenceDriver(Device device, FolderNode deviceFolder) {
        super(device, deviceFolder);
        this.timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "2000"));
    }

    // Read-only variables
    private VariableNode systemStatusCode, SystemStatusNames, errorLevel, errorCodes, errorNames, time;
    // Read-write variables
    private VariableNode lineSpeed;
    @Override
    protected void createNodes(Device device, FolderNode folder) {
        var system = folder.addFolder("System");
        systemStatusCode = system.addVariableReadOnly("System Status Code", Identifiers.Int32);
        SystemStatusNames = system.addVariableReadOnly("System Status Names", Identifiers.String);
        errorLevel = system.addVariableReadOnly("Error Level", Identifiers.String);
        errorCodes = system.addVariableReadOnly("Error Codes", Identifiers.String);
        errorNames = system.addVariableReadOnly("Error Names", Identifiers.String);
        time = system.addVariableReadOnly("Time", Identifiers.String);
        var settings = folder.addFolder("Settings");
        lineSpeed = settings.addVariableReadWrite("Line Speed", Identifiers.Int32);
    }

    @Override
    protected void process(List<DeviceRequest> requestQueue) {
        if(!ensureConnected()) {
            for (DeviceRequest request : requestQueue) {
                if(request instanceof DeviceRequest.ReadRequest readRequest) {
                    completeReadRequest(readRequest, new DataValue(StatusCodes.Bad_NoCommunication));
                } else if(request instanceof DeviceRequest.WriteRequest writeRequest) {
                    completeWriteRequest(writeRequest, new IOException("Not connected"));
                }
            }
            return;
        }

        var readVariables = requestQueue.stream()
                .filter(request -> request instanceof DeviceRequest.ReadRequest)
                .map(request -> ((DeviceRequest.ReadRequest) request).getVariable())
                .collect(Collectors.toSet());
        var errorVariables = readVariables.stream()
                .filter(variable -> variable == errorCodes || variable == errorNames || variable == errorLevel)
                .collect(Collectors.toSet());
        var errorVariablesReadRequests = requestQueue.stream()
                .filter(request -> request instanceof DeviceRequest.ReadRequest)
                .filter(request -> errorVariables.contains(((DeviceRequest.ReadRequest) request).getVariable())).toList();
        if(!errorVariables.isEmpty()) {
            try {
                var errorCodesString = execCommand("EV");
                var errorCodesSplit = errorCodesString.split(Pattern.quote(","));
                KeyenceDriverCodes.ErrorLevel highestErrorLevel = KeyenceDriverCodes.ErrorLevel.OK;
                StringBuilder errorCodesBuilder = new StringBuilder(), errorNamesBuilder = new StringBuilder();
                for(int i = 1; i < errorCodesSplit.length; i++) {
                    var errorCode = Integer.parseInt(errorCodesSplit[i]);
                    var error = KeyenceDriverCodes.getSystemErrorCode(errorCode);
                    if(error.getLevel().compareTo(highestErrorLevel) > 0) {
                        highestErrorLevel = error.getLevel();
                    }
                    errorCodesBuilder.append(errorCode).append(",");
                    errorNamesBuilder.append(error.getName()).append(",");
                }
                for(DeviceRequest request : errorVariablesReadRequests) {
                    if(request instanceof DeviceRequest.ReadRequest readRequest) {
                        if(readRequest.getVariable() == errorCodes) {
                            completeReadRequest(readRequest, new DataValue(new Variant(errorCodesBuilder.toString())));
                        } else if(readRequest.getVariable() == errorNames) {
                            completeReadRequest(readRequest, new DataValue(new Variant(errorNamesBuilder.toString())));
                        } else if(readRequest.getVariable() == errorLevel) {
                            completeReadRequest(readRequest, new DataValue(new Variant(highestErrorLevel.name())));
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("[{}]: Error reading error codes", getDevice().getName(), e);
            }
        }
    }

    private String execCommand(String command, String ... parameters) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(command);
        for (String parameter : parameters) {
            stringBuilder.append(",");
            stringBuilder.append(parameter);
        }
        writer.write(stringBuilder.toString() + "\r");
        writer.flush();
        String result = reader.readLine();
        if(result.startsWith(command)) {
            return result.substring(command.length());
        } else if(result.startsWith("ER")) {
            var split = result.split(Pattern.quote(","));
            var errorCode = Integer.parseInt(split[2]);
            var error = KeyenceDriverCodes.getErrorResponse(errorCode);
            throw new IOException("Error " + error + " while executing command \"" + command + "\"");
        } else {
            throw new IOException("Unexpected response from device: \"" + result + "\" for command \"" + command + "\"");
        }
    }

    private Socket socket = null;
    private PrintWriter writer = null;
    private BufferedReader reader = null;

    private void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            log.trace("[{}]: Error closing socket", getDevice().getName(), e);
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
            socket.connect(new InetSocketAddress(getDevice().getOption("hostname"),
                    Integer.parseInt(getDevice().getOptionOrDefault("port", "9004"))), timeout);
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.US_ASCII);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            return true;
        } catch (IOException e) {
            log.trace("[{}]: Error connecting to device", getDevice().getName(), e);
            // cooldown
            disconnect();
            return false;
        }
    }
}
