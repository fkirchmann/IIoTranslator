/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.other;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.iiotranslator.device.Device;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.device.drivers.DriverUtil;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Driver to read the energy usage of a Tasmota device.
 */
@Slf4j
public class TasmotaDriver implements DeviceDriver {
    private Device device;
    private WebClient client;

    private final Gson gson = new Gson();

    private VariableNode time,
            totalStartTime,
            energy_Wh,
            power_W,
            apparentPower_VA,
            reactivePower_VAr,
            powerFactor,
            voltage_V,
            current_A;

    @Override
    public void initialize(Device device, FolderNode deviceFolderNode) {
        this.device = device;
        String hostname = device.getOption("hostname");
        // Tasmota devices typically have a 2.4 GHz WiFi module with a small antenna,
        // potentially causing delays from retransmits when the signal is weak. To prevent these delays
        // from causing problems, we use a longer default timeout than usual.
        int timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "8000"));

        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(DriverUtil.createHttpClient(timeout)))
                .baseUrl("http://" + hostname)
                .build();

        time = deviceFolderNode.addVariableReadOnly("Time", Identifiers.String);
        totalStartTime = deviceFolderNode.addVariableReadOnly("Energy Measurement Start Time", Identifiers.String);
        energy_Wh = deviceFolderNode.addVariableReadOnly("Energy (Wh)", Identifiers.Double);
        power_W = deviceFolderNode.addVariableReadOnly("Power (W)", Identifiers.Int64);
        apparentPower_VA = deviceFolderNode.addVariableReadOnly("Apparent Power (VA)", Identifiers.Int64);
        reactivePower_VAr = deviceFolderNode.addVariableReadOnly("Reactive Power (VAr)", Identifiers.Int64);
        powerFactor = deviceFolderNode.addVariableReadOnly("Power Factor", Identifiers.Double);
        voltage_V = deviceFolderNode.addVariableReadOnly("Voltage (V)", Identifiers.Int64);
        current_A = deviceFolderNode.addVariableReadOnly("Current (A)", Identifiers.Double);
    }

    @Override
    public void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        Map<VariableNode, DataValue> variableValues = new HashMap<>();
        try {
            String powerStatusJSON = client.get()
                    .uri("/cm?cmnd=status 8")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.trace("[{}]: Response: {}", device.getName(), powerStatusJSON);
            // Example contents of powerStatusJSON:
            // {"StatusSNS":{"Time":"2023-06-29T17:42:21","ENERGY":{"TotalStartTime":"2020-02-16T07:24:07","Total":1353.041,"Yesterday":0.676,"Today":1.093,"Power":105,"ApparentPower":122,"ReactivePower":63,"Factor":0.86,"Voltage":287,"Current":0.427}}}
            // Decode JSON using gson:
            var response = gson.fromJson(powerStatusJSON, TasmotaStatus8Response.class);
            // Update variables:
            var energy = response.getStatusSNS().getEnergy();
            variableValues.put(
                    time, new DataValue(new Variant(response.getStatusSNS().getTime())));
            variableValues.put(totalStartTime, new DataValue(new Variant(energy.getTotalStartTime())));
            variableValues.put(energy_Wh, new DataValue(new Variant(energy.getTotal() * 1000.0)));
            variableValues.put(power_W, new DataValue(new Variant(energy.getPower())));
            variableValues.put(apparentPower_VA, new DataValue(new Variant(energy.getApparentPower())));
            variableValues.put(reactivePower_VAr, new DataValue(new Variant(energy.getReactivePower())));
            variableValues.put(powerFactor, new DataValue(new Variant(energy.getFactor())));
            variableValues.put(voltage_V, new DataValue(new Variant(energy.getVoltage())));
            variableValues.put(current_A, new DataValue(new Variant(energy.getCurrent())));
        } catch (Exception e) {
            log.trace("[{}]: Error reading from device", device.getName(), e);
        } finally {
            for (DeviceRequest request : requestQueue) {
                var readRequest = (DeviceRequest.ReadRequest) request;
                var variable = readRequest.getVariable();
                listener.completeReadRequest(
                        readRequest, variableValues.getOrDefault(variable, new DataValue(StatusCode.BAD)));
            }
        }
    }

    // The classes below are used to decode the JSON response from the Tasmota device.

    @Data
    public static class TasmotaStatus8Response {
        @SerializedName("StatusSNS")
        private final TasmotaStatusSNS statusSNS;
    }

    @Data
    public static class TasmotaStatusSNS {
        @SerializedName("Time")
        private final String time;

        @SerializedName("ENERGY")
        private final TasmotaEnergy energy;
    }

    @Data
    public static class TasmotaEnergy {
        @SerializedName("TotalStartTime")
        private final String totalStartTime;

        @SerializedName("Total")
        private final double total;

        @SerializedName("Power")
        private final long power;

        @SerializedName("ApparentPower")
        private final long apparentPower;

        @SerializedName("ReactivePower")
        private final long reactivePower;

        @SerializedName("Factor")
        private final double factor;

        @SerializedName("Voltage")
        private final long voltage;

        @SerializedName("Current")
        private final double current;
    }
}
