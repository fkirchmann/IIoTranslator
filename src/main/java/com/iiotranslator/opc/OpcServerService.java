/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.iiotranslator.opc;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for starting the OPC UA server, using the configuration provided by Spring Boot.
 */
@Service
@Slf4j
public class OpcServerService {
    @Value("${iiot.opcua.bindAddress}")
    private String bindAddress;

    @Value("${iiot.opcua.bindPortTcp}")
    private int bindPortTcp;

    @Value("${iiot.opcua.hostnames}")
    private Set<String> hostnames;

    @Value("${iiot.opcua.username}")
    private String username;

    @Value("${iiot.opcua.password}")
    private String password;

    @Getter
    private OpcServer server;

    @PostConstruct
    @SneakyThrows
    private void initialize() {
        log.info("Starting OPC UA server");
        this.server = new OpcServer(hostnames, bindAddress, bindPortTcp, username, password);
        server.startup().get();
        server.getRootNode().get();
        log.info("OPC UA server started");
    }
}
