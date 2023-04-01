/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.service;

import com.iiotranslator.device.DevicesService;
import com.iiotranslator.opc.OpcServer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class OpcDeviceServerService {
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

    private final DevicesService devicesService;

    public OpcDeviceServerService(DevicesService devicesService) {
        this.devicesService = devicesService;
    }

    @PostConstruct
    @SneakyThrows
    private void initialize() {
        this.server = new OpcServer(hostnames, bindAddress, bindPortTcp, username, password,
                devicesService.getRootNode(), devicesService);
        server.startup().get();

        log.debug("OPC UA server started");
    }
}
