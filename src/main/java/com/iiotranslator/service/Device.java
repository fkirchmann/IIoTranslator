/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Map;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class Device {
    @Getter
    public final String name, driver;

    @Getter
    private final Map<String, String> options;

    public String getOption(String key) {
        if(!options.containsKey(key)) {
            throw new IllegalArgumentException("Device " + name + " is missing option " + key);
        }
        return options.get(key);
    }

    public String getOptionOrDefault(String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }
}
