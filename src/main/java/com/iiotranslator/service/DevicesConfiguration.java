/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Slf4j
@ToString
@ConfigurationProperties(prefix = "iiot")
@Getter
public class DevicesConfiguration {
    private final List<Device> devices;

    @ConstructorBinding
    public DevicesConfiguration(Map<String, Map<String, String>> devices) {
        if (devices == null || devices.isEmpty()) {
            throw new RuntimeException("No devices were configured!"
                    + " Please check your configuration file, usually application.properties");
        }
        this.devices = devices.entrySet().stream()
                .map(entry -> new Device(
                        entry.getKey(), entry.getValue().get("driver"), Collections.unmodifiableMap(entry.getValue())))
                .toList();
    }
}
