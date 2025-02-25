package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.persistence.ClientRepository;
import com.brcsrc.yaws.persistence.NetworkClientRepository;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.service.NetworkClientService;
import com.brcsrc.yaws.service.NetworkService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NetworkClientControllerTests {

    @LocalServerPort
    private int port;

    private final RestClient restClient = RestClient.create();

    @Autowired
    private NetworkService networkService;
    @Autowired
    private NetworkClientService networkClientService;

    @Autowired
    private NetworkRepository networkRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private NetworkClientRepository netClientRepository;

    private static final Logger logger = LoggerFactory.getLogger(NetworkClientControllerTests.class);

    private String baseUrl;

    private final String testNetworkName = "Network1";
    private final String testNetworkCidr = "10.100.0.1/24";
    private final String testNetworkTag = "network 1 tag";
    private final int testNetworkListenPort = 51820;

    private final String testClientName = "Client1";
    private final String testClientCidr = "10.100.0.2/24";
    private final String testClientDns = "1.1.1.1";
    private final String testAllowedIps = "0.0.0.0/0";
    private final String testNetworkEndpoint = "127.0.0.1:51820";
    private final String testClientTag = "client 1 tag";


    public NetworkClientControllerTests() {
    }

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port + "/api/v1/clients";

        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        if (networkFromDb.isEmpty()) {
            logger.info("creating test network");
            Network network = new Network();
            network.setNetworkName(testNetworkName);
            network.setNetworkCidr(testNetworkCidr);
            network.setNetworkListenPort(testNetworkListenPort);
            network.setNetworkTag(testNetworkTag);
            networkService.createNetwork(network);
        }

    }

    @AfterEach
    public void teardown() {
        NetworkClient networkClient = netClientRepository.findNetworkClientByNetwork_NetworkNameAndClient_ClientName(
                testNetworkName,
                testClientName
        );
        if (networkClient != null) {
            logger.info("cleaning up existing network client");
            networkClientService.deleteNetworkClient(testNetworkName, testClientName);
        }
        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        if (networkFromDb.isPresent()) {
            logger.info("cleaning up existing network");
            networkService.deleteNetwork(testNetworkName);
        }

    }

    @Test
    public void testCreateNetworkClientCreatesClient() throws JsonProcessingException {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientCidr(testClientCidr);
        createNetworkClientRequest.setClientDns(testClientDns);
        createNetworkClientRequest.setAllowedIps(testAllowedIps);
        createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);
        createNetworkClientRequest.setClientTag(testClientTag);

        ResponseEntity<NetworkClient> createNetworkClientResponse = restClient.post()
                .uri(baseUrl)
                .body(createNetworkClientRequest)
                .retrieve()
                .toEntity(NetworkClient.class);

        // assert the response status is 200
        assertEquals(HttpStatus.OK, createNetworkClientResponse.getStatusCode());

        // assert the database has the record
        NetworkClient savedNetworkClient = netClientRepository.findNetworkClientByNetwork_NetworkNameAndClient_ClientName(
                testNetworkName,
                testClientName
        );
        assertNotEquals(savedNetworkClient, null);
        assertEquals(savedNetworkClient.getClient().getClientName(), testClientName);
        assertEquals(savedNetworkClient.getClient().getClientCidr(), testClientCidr);
        assertEquals(savedNetworkClient.getClient().getClientDns(), testClientDns);
        assertEquals(savedNetworkClient.getClient().getNetworkListenPort(), testNetworkListenPort);
        assertEquals(savedNetworkClient.getClient().getAllowedIps(), testAllowedIps);
        assertEquals(savedNetworkClient.getClient().getNetworkEndpoint(), testNetworkEndpoint);

        String expectedClientPrivateKeyFileName = String.format("%s-private-key", testClientName);
        String expectedClientPublicKeyFileName = String.format("%s-public-key", testClientName);
        assertEquals(savedNetworkClient.getClient().getClientPrivateKeyName(), expectedClientPrivateKeyFileName);
        assertEquals(savedNetworkClient.getClient().getClientPublicKeyName(), expectedClientPublicKeyFileName);

        // assert the client keys and config exist
        // TODO this will likely break when migrating to network specific directories for file storage
        String clientPublicKeyAbsPath = String.format("/etc/wireguard/%s", expectedClientPublicKeyFileName);
        String clientPrivateKeyAbsPath = String.format("/etc/wireguard/%s", expectedClientPrivateKeyFileName);
        String clientConfigAbsPath = String.format("/etc/wireguard/%s.conf", testClientName);
        var filePaths = List.of(
                clientPrivateKeyAbsPath,
                clientPublicKeyAbsPath
        );
        for (String path : filePaths) {
            assertTrue(Files.exists(Paths.get(path)));
        }

        // assert the network config has a new entry
        // TODO add network peers to NetworkConfig class
    }

}
