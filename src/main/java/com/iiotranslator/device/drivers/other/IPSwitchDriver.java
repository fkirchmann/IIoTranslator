/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.device.drivers.other;

import com.iiotranslator.device.Device;
import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.device.drivers.DeviceDriver;
import com.iiotranslator.device.drivers.DriverUtil;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.opc.VariableNode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Driver to read the energy usage of an IPswitch device. Tested with IPswitch-S0m-Wifi.
 *
 * @see <a href="https://www.sms-guard.org/downloads/IPswitch-S0m-WiFi-Anleitung.pdf">IPswitch-S0m-WiFi</a>
 */
@Slf4j
public class IPSwitchDriver implements DeviceDriver {
    private Device device;
    private Duration impulseBackupInterval;
    private WebClient client;

    private VariableNode name,
            device_model,
            mac_address,
            signal_strength,
            energy_Wh,
            power_W,
            impulse_counter,
            impulses_per_kWh;

    @Override
    public void initialize(Device device, FolderNode deviceFolderNode) {
        this.device = device;
        String hostname = device.getOption("hostname");
        // IPswitch devices typically have a 2.4 GHz WiFi module with a small antenna,
        // causing delays from retransmits when the signal is weak. To prevent these delays
        // from causing problems, we use a longer default timeout than usual.
        int timeout = Integer.parseInt(device.getOptionOrDefault("timeout", "8000"));
        Duration backupInterval = null;
        try {
            backupInterval =
                    Duration.ofHours(Long.parseLong(device.getOptionOrDefault("impulseBackupIntervalHours", "24")));
        } catch (NumberFormatException ignored) {
        }
        impulseBackupInterval = backupInterval;
        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(DriverUtil.createHttpClient(timeout)))
                .baseUrl("http://" + hostname)
                .build();

        var sysFolder = deviceFolderNode.addFolder("Device Status");
        name = sysFolder.addVariableReadOnly("Device Name", Identifiers.String);
        device_model = sysFolder.addVariableReadOnly("Device Model", Identifiers.String);
        mac_address = sysFolder.addVariableReadOnly("MAC Address", Identifiers.String);
        signal_strength = sysFolder.addVariableReadOnly("WiFi Signal Strength (dBm)", Identifiers.Int32);

        energy_Wh = deviceFolderNode.addVariableReadOnly("Energy (Wh)", Identifiers.Int64);
        power_W = deviceFolderNode.addVariableReadOnly("Estimated Power (W)", Identifiers.Int64);
        impulse_counter = deviceFolderNode.addVariableReadOnly("Impulse Counter", Identifiers.Int64);
        impulses_per_kWh = deviceFolderNode.addVariableReadOnly("Impulses per kWh", Identifiers.Int64);
    }

    private static final Pattern IMPULSE_PATTERN = Pattern.compile("\\d+Wh = (\\d+)Imp, imp= (\\d+)imp/kWh");

    @Override
    public void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener) {
        variableValues.remove(signal_strength);
        variableValues.remove(energy_Wh);
        variableValues.remove(power_W);
        variableValues.remove(impulse_counter);
        variableValues.remove(impulses_per_kWh);
        try {
            String impulseRaw = client.get()
                    .uri("/?S0=?")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.trace("[{}]: Response: {}", device.getName(), impulseRaw);
            var matcher = IMPULSE_PATTERN.matcher(impulseRaw);
            if (matcher.find()) {
                var impulseCount = Long.parseLong(matcher.group(1));
                checkAndUpdateImpulseCount(impulseCount);
                var impulsesPerKWh = Long.parseLong(matcher.group(2));
                variableValues.put(impulses_per_kWh, new DataValue(new Variant(impulsesPerKWh)));
            } else {
                log.warn("[{}]: Could not parse impulse count from response: {}", device.getName(), impulseRaw);
            }
            // the above code may have changed the energy_Wh value (due to changing the impulse count), so we need to
            // read it afterwards
            String valuesRaw = client.get()
                    .uri("/csv.html")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.trace("[{}]: Response: {}", device.getName(), valuesRaw);
            String[] values = valuesRaw.split(",");
            variableValues.put(device_model, new DataValue(new Variant(values[0])));
            for (int i = 2; i < values.length - 1; i += 2) {
                var key = values[i].substring(0, values[i].length() - 1);
                var value = values[i + 1];
                switch (key) {
                    case "name" -> variableValues.put(name, new DataValue(new Variant(value)));
                    case "mac" -> variableValues.put(mac_address, new DataValue(new Variant(value)));
                    case "S01" -> variableValues.put(energy_Wh, new DataValue(new Variant(Long.parseLong(value))));
                    case "Verbrauch_Ver" -> variableValues.put(
                            power_W, new DataValue(new Variant(Long.parseLong(value))));
                    case "rssi" -> variableValues.put(
                            signal_strength, new DataValue(new Variant(Integer.parseInt(value))));
                }
            }
        } catch (Exception e) {
            log.trace("[{}]: Error reading from device: {}", device.getName(), e);
        } finally {
            for (DeviceRequest request : requestQueue) {
                var readRequest = (DeviceRequest.ReadRequest) request;
                var variable = readRequest.getVariable();
                listener.completeReadRequestExceptionally(
                        readRequest, variableValues.getOrDefault(variable, new DataValue(StatusCode.BAD)));
            }
        }
    }

    private final Map<VariableNode, DataValue> variableValues = new HashMap<>();

    private Long lastImpulseCount = null;
    private Long lastBackupImpulseCount = null;
    private Instant lastBackupTime = Instant.now();

    private void checkAndUpdateImpulseCount(long impulseCount) {
        if (lastImpulseCount == null || impulseCount > lastImpulseCount) {
            lastImpulseCount = impulseCount;
        } else if (impulseCount < lastImpulseCount) {
            log.debug(
                    "[{}]: Impulse counter reset detected ({} to {}), restoring old value",
                    device.getName(),
                    lastImpulseCount,
                    impulseCount);
            writeImpulseCount(lastImpulseCount);
        }
        variableValues.put(impulse_counter, new DataValue(new Variant(lastImpulseCount)));
        backupIfNecessary();
    }

    private void writeImpulseCount(long impulseCount) {
        enableEepromWrite();
        // Write impulse count
        client.get()
                .uri("/?S0=" + impulseCount)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        lastBackupImpulseCount = impulseCount;
        lastBackupTime = Instant.now();
    }

    private boolean eepromWriteEnabled = false;

    private void enableEepromWrite() {
        if (!eepromWriteEnabled) {
            log.debug("[{}]: Enabling EEPROM write", device.getName());
            client.get().uri("/?eep=1").retrieve().bodyToMono(String.class).block();
            eepromWriteEnabled = true;
        }
    }

    private void backupIfNecessary() {
        // Backup impulse count to device, by default every 24 hours
        if (impulseBackupInterval != null && Instant.now().isAfter(lastBackupTime.plus(impulseBackupInterval))) {
            // don't backup if the impulse count didn't change
            if (lastBackupImpulseCount == null || !lastBackupImpulseCount.equals(lastImpulseCount)) {
                log.debug("[{}]: Backing up impulse count {}", device.getName(), lastImpulseCount);
                writeImpulseCount(lastImpulseCount);
                lastBackupImpulseCount = lastImpulseCount;
            }
            lastBackupTime = Instant.now();
        }
    }
}
