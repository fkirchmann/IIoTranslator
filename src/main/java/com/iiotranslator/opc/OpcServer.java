/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.opc;

import lombok.NonNull;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.collect.Lists.newArrayList;

public class OpcServer {
    private static final String PRODUCT_URI = "urn:com:iiotranslator",
            APPLICATION_URI = "urn:com:iiotranslator:opcua:server",
            MANUFACTURER_NAME = "IIoTranslator",
            PRODUCT_NAME = "IIoTranslator",
            APPLICATION_NAME = "IIoTranslator OPC UA Server";

    private static final UserTokenPolicy USER_TOKEN_POLICY_USERNAME_UNENCRYPTED = new UserTokenPolicy(
            "username", UserTokenType.UserName, null, null, null);

    private final OpcUaServer server;
    private final OpcNamespace opcNamespace;

    private final Set<String> hostnames;
    private final String bindAddress;
    private final int tcpBindPort;
    private final VariableNodeAccessor variableNodeAccessor;

    public OpcServer(@NonNull Set<String> hostnames, @NonNull String bindAddress, int tcpBindPort,
                     @NonNull String username, @NonNull String password, @NonNull RootNode rootNode,
                     @NonNull VariableNodeAccessor variableNodeAccessor) {
        this.hostnames = hostnames;
        this.bindAddress = bindAddress;
        this.tcpBindPort = tcpBindPort;
        this.variableNodeAccessor = variableNodeAccessor;

        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(false,
            authChallenge -> authChallenge.getUsername().equals(username)
                    && authChallenge.getPassword().equals(password)
        );

        Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations();

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationUri(APPLICATION_URI)
            .setApplicationName(LocalizedText.english(APPLICATION_NAME))
            .setEndpoints(endpointConfigurations)
            .setBuildInfo(
                new BuildInfo(
                    PRODUCT_URI,
                    MANUFACTURER_NAME,
                    PRODUCT_NAME,
                    "using-milo-" + OpcUaServer.SDK_VERSION,
                    "", DateTime.now()))
            .setIdentityValidator(identityValidator)
            .setProductUri(PRODUCT_URI)
            .build();

        server = new OpcUaServer(serverConfig);

        opcNamespace = new OpcNamespace(server, rootNode, variableNodeAccessor);
        opcNamespace.startup();
    }

    private Set<EndpointConfiguration> createEndpointConfigurations() {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add(bindAddress);

        for (String currentBindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                    .setBindAddress(currentBindAddress)
                    .setHostname(hostname)
                    .setPath("/")
                    .addTokenPolicies(
                            USER_TOKEN_POLICY_USERNAME_UNENCRYPTED);

                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                    .setSecurityPolicy(SecurityPolicy.None)
                    .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));

                /*
                 * It's good practice to provide a discovery-specific endpoint with no security.
                 * It's required practice if all regular endpoints have security configured.
                 *
                 * Usage of the  "/discovery" suffix is defined by OPC UA Part 6:
                 *
                 * Each OPC UA Server Application implements the Discovery Service Set. If the OPC UA Server requires a
                 * different address for this Endpoint it shall create the address by appending the path "/discovery" to
                 * its base address.
                 */
                EndpointConfiguration.Builder discoveryBuilder = builder.copy()
                    .setPath("/discovery")
                    .setSecurityPolicy(SecurityPolicy.None)
                    .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
            }
        }

        return endpointConfigurations;
    }

    private EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
        return base.copy()
            .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
            .setBindPort(tcpBindPort)
            .build();
    }

    public CompletableFuture<OpcUaServer> startup() {
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        opcNamespace.shutdown();

        return server.shutdown();
    }
}
