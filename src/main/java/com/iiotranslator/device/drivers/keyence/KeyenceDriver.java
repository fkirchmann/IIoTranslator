/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.keyence;

import com.iiotranslator.device.Device;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import com.iiotranslator.opc.WritableVariableNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

/*
 * This driver supports Keyence's MK-U6000/MK-U2000 industrial ink-jet printer.
 */
@Slf4j
public class KeyenceDriver implements DeviceDriver {
    private Device device;
    private int timeout;

    // Read-only variables
    private VariableNode systemStatusCode,
            systemStatusNames,
            errorLevel,
            errorCodes,
            errorNames,
            time,
            currentProgram,
            lastPrinted;

    /*
     * Variables for which the "EV" (get error), "SB" (get system status), and "FR" (get current program) commands
     * need to be run.
     */
    private Set<VariableNode> errorVariables, systemStatusVariables, currentProgramVariables;

    // Read-write variables
    private WritableVariableNode lineSpeed;

    private final Map<VariableNode, DataValue> variableValues = new HashMap<>();

    @Override
    public void initialize(Device device, FolderNode folder) {
        this.device = device;
        this.timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "2000"));

        var system = folder.addFolder("System");
        systemStatusCode = system.addVariableReadOnly("System Status Code", Identifiers.Int32);
        systemStatusNames = system.addVariableReadOnly("System Status Name", Identifiers.String);
        errorLevel = system.addVariableReadOnly("Error Level", Identifiers.String);
        errorCodes = system.addVariableReadOnly("Error Codes", Identifiers.String);
        errorNames = system.addVariableReadOnly("Error Names", Identifiers.String);
        currentProgram = system.addVariableReadOnly("Current Program", Identifiers.String);
        lastPrinted = system.addVariableReadOnly("Last Printed Value", Identifiers.String);
        time = system.addVariableReadOnly("Time", Identifiers.String);
        var settings = folder.addFolder("Global Settings");
        lineSpeed = settings.addVariableReadWrite("Line Speed in mm_per_sec", Identifiers.Double);

        errorVariables = Set.of(errorLevel, errorCodes, errorNames);
        systemStatusVariables = Set.of(systemStatusCode, systemStatusNames);
        currentProgramVariables = Set.of(currentProgram, lastPrinted);
    }

    @Override
    public void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        if (!ensureConnected()) {
            for (DeviceRequest request : requestQueue) {
                if (request instanceof DeviceRequest.ReadRequest readRequest) {
                    listener.completeReadRequestExceptionally(
                            readRequest, new DataValue(StatusCodes.Bad_NoCommunication));
                } else if (request instanceof DeviceRequest.WriteRequest writeRequest) {
                    listener.completeWriteRequestExceptionally(writeRequest, new IOException("Not connected"));
                }
            }
            return;
        }

        var readVariables = requestQueue.stream()
                .filter(request -> request instanceof DeviceRequest.ReadRequest)
                .map(request -> ((DeviceRequest.ReadRequest) request).getVariable())
                .collect(Collectors.toSet());
        var writeVariables = requestQueue.stream()
                .filter(request -> request instanceof DeviceRequest.WriteRequest)
                .map(request -> ((DeviceRequest.WriteRequest) request).getVariable())
                .collect(Collectors.toSet());
        // read error variables if any of them are requested
        if (!Collections.disjoint(readVariables, errorVariables)) {
            try {
                var errorCodesString = execCommand("EV");
                var errorCodesSplit = errorCodesString.split(Pattern.quote(","), -1);
                KeyenceDriverCodes.ErrorLevel highestErrorLevel = KeyenceDriverCodes.ErrorLevel.OK;
                StringBuilder errorCodesBuilder = new StringBuilder(), errorNamesBuilder = new StringBuilder();
                for (int i = 1; i < errorCodesSplit.length; i++) {
                    var errorCode = Integer.parseInt(errorCodesSplit[i]);
                    var error = KeyenceDriverCodes.getSystemErrorCode(errorCode);
                    if (error.getLevel().compareTo(highestErrorLevel) > 0) {
                        highestErrorLevel = error.getLevel();
                    }
                    errorCodesBuilder.append(errorCode).append(",");
                    errorNamesBuilder.append(error.getName()).append(",");
                }
                variableValues.put(errorCodes, new DataValue(new Variant(errorCodesBuilder.toString())));
                variableValues.put(errorNames, new DataValue(new Variant(errorNamesBuilder.toString())));
                variableValues.put(errorLevel, new DataValue(new Variant(highestErrorLevel.name())));
            } catch (IOException e) {
                log.debug("[{}]: Error reading error codes", device.getName(), e);
                variableValues.put(errorLevel, new DataValue(StatusCodes.Bad_InternalError));
                variableValues.put(errorCodes, new DataValue(StatusCodes.Bad_InternalError));
                variableValues.put(errorNames, new DataValue(StatusCodes.Bad_InternalError));
            }
        }
        // Read system status
        if (!Collections.disjoint(readVariables, systemStatusVariables)) {
            try {
                var systemStatusCodeString = execCommand("SB");
                var systemStatusCodeSplit = systemStatusCodeString.split(Pattern.quote(","), -1);
                var systemStatusCodeValue = Integer.parseInt(systemStatusCodeSplit[1]);
                var systemStatusName = KeyenceDriverCodes.getSystemStatusCode(systemStatusCodeValue);
                variableValues.put(systemStatusCode, new DataValue(new Variant(systemStatusCodeValue)));
                variableValues.put(systemStatusNames, new DataValue(new Variant(systemStatusName.getName())));
            } catch (IOException e) {
                log.debug("[{}]: Error reading system status", device.getName(), e);
                variableValues.put(systemStatusCode, new DataValue(StatusCodes.Bad_InternalError));
                variableValues.put(systemStatusNames, new DataValue(StatusCodes.Bad_InternalError));
            }
        }
        if (readVariables.contains(lineSpeed) || writeVariables.contains(lineSpeed)) {
            try {
                var globalSettings = execCommand("FL", "CMN", "0");
                var globalSettingsSplit = globalSettings.split(Pattern.quote(","), -1);
                var lineSpeedValue = (double) Integer.parseInt(globalSettingsSplit[10]) / 10.0;
                variableValues.put(lineSpeed, new DataValue(new Variant(lineSpeedValue)));
                var writeRequests = requestQueue.stream()
                        .filter(request -> request instanceof DeviceRequest.WriteRequest)
                        .filter(request -> lineSpeed.equals(((DeviceRequest.WriteRequest) request).getVariable()))
                        .findFirst();
                if (writeRequests.isPresent()) {
                    var writeRequest = (DeviceRequest.WriteRequest) writeRequests.get();
                    var lineSpeedValueToWrite =
                            ((Double) writeRequest.getValue().getValue().getValue());
                    assert Objects.equals(globalSettingsSplit[0], "CMN");
                    globalSettingsSplit[10] = Integer.toString((int) (lineSpeedValueToWrite * 10));
                    log.trace("[{}]: Writing line speed {}", device.getName(), globalSettingsSplit[10]);
                    try {
                        var result = execCommand(
                                "FM", Arrays.copyOfRange(globalSettingsSplit, 1, globalSettingsSplit.length));
                        log.trace("[{}]: Write result {}", device.getName(), result);
                        if (!result.equals("")) {
                            throw new IOException("Error writing line speed: unexpected response \"" + result + "\"");
                        }
                        variableValues.put(lineSpeed, new DataValue(new Variant(lineSpeedValueToWrite)));
                        listener.completeWriteRequestExceptionally(writeRequest);
                    } catch (IOException e) {
                        log.debug("[{}]: Error writing line speed", device.getName(), e);
                        listener.completeWriteRequestExceptionally(writeRequest, e);
                    }
                }
            } catch (IOException e) {
                log.debug("[{}]: Error reading line speed", device.getName(), e);
                variableValues.put(lineSpeed, new DataValue(StatusCodes.Bad_InternalError));
            }
        }
        // read time with DB command
        if (readVariables.contains(time)) {
            try {
                var timeString = execCommand("DB");
                var timeSplit = timeString.split(Pattern.quote(","), -1);
                if (timeSplit.length != 7) {
                    throw new IOException("Unexpected response \"" + timeString + "\"");
                }
                var timeStringISO6801 = "20" + timeSplit[1] + "-" + timeSplit[2] + "-" + timeSplit[3] + "T"
                        + timeSplit[4] + ":" + timeSplit[5] + ":" + timeSplit[6];
                variableValues.put(time, new DataValue(new Variant(timeStringISO6801)));
            } catch (IOException e) {
                log.debug("[{}]: Error reading time", device.getName(), e);
                variableValues.put(time, new DataValue(StatusCodes.Bad_InternalError));
            }
        }
        // read current program with FR command
        if (!Collections.disjoint(readVariables, currentProgramVariables)) {
            try {
                var programString = execCommand("FR");
                var programSplit = programString.split(Pattern.quote(","), -1);
                if (programSplit.length != 2) {
                    throw new IOException("Unexpected response \"" + programString + "\"");
                }
                var programNumber = Integer.parseInt(programSplit[1]);
                variableValues.put(currentProgram, new DataValue(new Variant(programNumber)));
                if (readVariables.contains(lastPrinted)) {
                    var lastPrintedString = execCommand("UZ", Integer.toString(programNumber), "0");
                    var lastPrintedSplit = lastPrintedString.split(Pattern.quote(","), -1);
                    if (lastPrintedSplit.length != 4) {
                        throw new IOException("Unexpected response \"" + Arrays.toString(lastPrintedSplit) + "\"");
                    }
                    variableValues.put(lastPrinted, new DataValue(new Variant(lastPrintedSplit[3])));
                }
            } catch (IOException | NumberFormatException e) {
                log.debug("[{}]: Error reading current program", device.getName(), e);
                variableValues.put(currentProgram, new DataValue(StatusCodes.Bad_InternalError));
            }
        }
        // Answer read requests with variableValues
        requestQueue.stream()
                .filter(request -> request instanceof DeviceRequest.ReadRequest)
                .forEach(request -> {
                    var value = variableValues.get(((DeviceRequest.ReadRequest) request).getVariable());
                    listener.completeReadRequestExceptionally(
                            (DeviceRequest.ReadRequest) request,
                            Objects.requireNonNullElseGet(value, () -> new DataValue(StatusCodes.Bad_NoData)));
                });
    }

    private String execCommand(String command, String... parameters) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(command);
        for (String parameter : parameters) {
            stringBuilder.append(",");
            stringBuilder.append(parameter);
        }
        log.trace("[{}]: Executing command \"{}\"", device.getName(), stringBuilder);
        writer.write(stringBuilder + "\r");
        writer.flush();
        String result = reader.readLine();
        log.trace("[{}]: Received response \"{}\"", device.getName(), result);
        if (result == null) {
            throw new IOException("Unexpected end of stream");
        } else if (result.startsWith(command)) {
            return result.substring(command.length());
        } else if (result.startsWith("ER")) {
            var split = result.split(Pattern.quote(","), -1);
            var errorCode = Integer.parseInt(split[2]);
            var error = KeyenceDriverCodes.getErrorResponse(errorCode);
            throw new IOException("Error " + error + " while executing command \"" + command + "\"");
        } else {
            throw new IOException(
                    "Unexpected response from device: \"" + result + "\" for command \"" + command + "\"");
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
        if (socket != null) {
            if (socket.isConnected()) {
                return true;
            } else {
                disconnect();
            }
        }
        try {
            socket = new Socket();
            socket.setSoTimeout(timeout);
            var hostname = device.getOption("hostname");
            var port = Integer.parseInt(device.getOptionOrDefault("port", "9004"));
            socket.connect(new InetSocketAddress(hostname, port), timeout);
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.US_ASCII);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            log.trace("[{}]: Connected to device", device.getName());
            return true;
        } catch (IOException e) {
            log.trace("[{}]: Error connecting to device", device.getName(), e);
            // cooldown
            disconnect();
            return false;
        }
    }
}
