/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator;

import com.iiotranslator.service.DevicesConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations="classpath:test.properties")
@Slf4j
class IIoTranslatorApplicationTests {
    @Autowired
    private DevicesConfiguration config;

    @Test
    void contextLoads() {
        log.debug(config.toString());
    }
}
