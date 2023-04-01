/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator;

import com.iiotranslator.service.DevicesConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DevicesConfiguration.class)
public class IIoTranslatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(IIoTranslatorApplication.class, args);
    }
}
