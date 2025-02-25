package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.model.requests.UpdateNetworkRequest;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.service.NetworkService;
import com.brcsrc.yaws.utility.WireguardConfigReaderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NetworkControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private NetworkRepository networkRepository;

    private String baseUrl;
    private final String testNetworkName = "Network1";
    private final String testNetworkCidr = "10.100.0.1/24";
    private final String testNetworkTag = "network 1 tag";
    private final int testNetworkListenPort = 51820;

    private final RestClient restClient = RestClient.create();

    private static final Logger logger = LoggerFactory.getLogger(NetworkControllerTests.class);

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port + "/api/v1/networks";
    }

    @AfterEach
    public void teardown() {
        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        if (networkFromDb.isPresent()) {
            logger.info("cleaning up existing test network");
            networkService.deleteNetwork(testNetworkName);
        }
    }

    @Test
    public void testCreateNetworkCreatesNetwork() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);

        String createNetworkUrl = baseUrl;

        ResponseEntity<Network> response = restClient.post()
                .uri(createNetworkUrl)
                .body(network)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(200, response.getStatusCode().value());
        Network createdNetwork = response.getBody();
        assert createdNetwork != null;

        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        assert networkFromDb.isPresent();

        Network readNetworkFromDb = networkFromDb.get();

        assertEquals(testNetworkName, readNetworkFromDb.getNetworkName());
        assertEquals(testNetworkCidr, readNetworkFromDb.getNetworkCidr());
        assertEquals(testNetworkListenPort, readNetworkFromDb.getNetworkListenPort());
        assertEquals(testNetworkTag, readNetworkFromDb.getNetworkTag());
        assertEquals(NetworkStatus.ACTIVE, readNetworkFromDb.getNetworkStatus());

        NetworkConfig networkConfig = WireguardConfigReaderUtils.readNetworkConfig(String.format("%s.conf", network.getNetworkName()));
        assertEquals(networkConfig.networkInterface.address, "10.100.0.1/24");
    }

    @Test
    public void testListNetworkListsNetworks() throws Exception {
        Network network = new Network();
        network.setNetworkName("Network2");
        network.setNetworkCidr("10.101.0.1/24");
        network.setNetworkListenPort(51821);
        network.setNetworkTag("net2");
        networkService.createNetwork(network);

        ResponseEntity<String> listNetworkResponse = restClient.get()
                .uri(baseUrl)
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, listNetworkResponse.getStatusCode());
        ObjectMapper objectMapper = new ObjectMapper();
        List<Network> networks = objectMapper.readValue(
                listNetworkResponse.getBody(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Network>>() {}
        );
        assertEquals(1, networks.size());
        assertEquals("Network2", networks.get(0).getNetworkName());
        assertEquals("10.101.0.1/24", networks.get(0).getNetworkCidr());
        assertEquals(51821, networks.get(0).getNetworkListenPort());
        assertEquals("net2", networks.get(0).getNetworkTag());
    }

    @Test
    public void testDescribeNetworkDescribesNetwork() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);
        networkService.createNetwork(network);

        String describeNetworkUrl = String.format("%s/%s", baseUrl, testNetworkName);

        ResponseEntity<Network> describeNetworkResponse = restClient.get()
                .uri(describeNetworkUrl)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(200, describeNetworkResponse.getStatusCode().value());
        Network describedNetwork = describeNetworkResponse.getBody();
        assert describedNetwork != null;

        assertEquals(testNetworkName, describedNetwork.getNetworkName());
        assertEquals(testNetworkCidr, describedNetwork.getNetworkCidr());
        assertEquals(testNetworkListenPort, describedNetwork.getNetworkListenPort());
        assertEquals(testNetworkTag, describedNetwork.getNetworkTag());
    }

    @Test
    public void testDeleteNetworkDeletesNetwork() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);
        networkService.createNetwork(network);

        String deleteNetworkUrl = String.format("%s/%s", baseUrl, testNetworkName);

        ResponseEntity<Network> deleteNetworkResponse = restClient.delete()
                .uri(deleteNetworkUrl)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(200, deleteNetworkResponse.getStatusCode().value());
        Network deletedNetwork = deleteNetworkResponse.getBody();
        assert deletedNetwork != null;

        assertEquals(testNetworkName, deletedNetwork.getNetworkName());
        assertEquals(testNetworkCidr, deletedNetwork.getNetworkCidr());
        assertEquals(testNetworkListenPort, deletedNetwork.getNetworkListenPort());
        assertEquals(testNetworkTag, deletedNetwork.getNetworkTag());
        assertEquals(NetworkStatus.DELETED, deletedNetwork.getNetworkStatus());
    }

    @Test
    public void testUpdateNetworkUpdatesNetwork() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);
        networkService.createNetwork(network);

        String newTag = "this is an updated tag";

        UpdateNetworkRequest updateNetworkRequest = new UpdateNetworkRequest();
        updateNetworkRequest.setNewTag(newTag);

        String updateNetworkUrl = String.format("%s/%s/tag", baseUrl, testNetworkName);

        ResponseEntity<Network> response = restClient.patch()
                .uri(updateNetworkUrl)
                .body(updateNetworkRequest)
                .retrieve()
                .toEntity(Network.class);

        Optional<Network> updatedNetwork = networkRepository.findByNetworkName(testNetworkName);
        assertTrue(updatedNetwork.isPresent());
        assertEquals(newTag, updatedNetwork.get().getNetworkTag());
    }
}

