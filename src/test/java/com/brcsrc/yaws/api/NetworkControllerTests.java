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
    public void testCreateNetworkCreatesNetwork() {
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
    public void testCreateNetworkRejectsInvalidNetworkNames() {
        var badNames = List.of(
                "name with spaces",
                "name-with-%@$-chars",
                "name-with-too-many-characters-123456789101112131415161718192021"
        );

        for (String s : badNames) {
            Network network = new Network();
            network.setNetworkName(s);
            network.setNetworkCidr(testNetworkCidr);
            network.setNetworkListenPort(testNetworkListenPort);
            network.setNetworkTag(testNetworkTag);

            ResponseEntity<String> response = restClient.post()
                    .uri(baseUrl)
                    .body(network)
                    .exchange((request, response2) -> {
                        String responseBody = response2.bodyTo(String.class);
                        return ResponseEntity.status(response2.getStatusCode()).body(responseBody);
                    });

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            String responseBody = response.getBody();
            assert responseBody != null;

            assertTrue(responseBody.contains("networkName must be alphanumeric without spaces and no more than 64 characters"));
            Optional<Network> networkFromDb = networkRepository.findByNetworkName(s);
            assert networkFromDb.isEmpty();
        }
    }

    @Test
    public void testCreateNetworkRejectsInvalidNetworkCidr() {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr("300.100.0.1/24");
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);

        ResponseEntity<String> response = restClient.post()
                .uri(baseUrl)
                .body(network)
                .exchange((request, response2) -> {
                    String responseBody = response2.bodyTo(String.class);
                    return ResponseEntity.status(response2.getStatusCode()).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        String responseBody = response.getBody();
        assert responseBody != null;
        assertTrue(responseBody.contains("networkCidr is invalid"));

        Optional<Network> networkFromDb = networkRepository.findByNetworkName("Network1");
        assert networkFromDb.isEmpty();
    }

    @Test
    public void testCreateNetworkRejectsInvalidNetworkInterfaceAddress() {
        var badNetworkInterfaceAddresses = List.of(
                "10.100.0.255/24",
                "10.0.0.0/24"
        );

        for (String badAddress : badNetworkInterfaceAddresses) {
            Network network = new Network();
            network.setNetworkName(testNetworkName);
            network.setNetworkCidr(badAddress);
            network.setNetworkListenPort(testNetworkListenPort);
            network.setNetworkTag(testNetworkTag);

            ResponseEntity<String> response = restClient.post()
                    .uri(baseUrl)
                    .body(network)
                    .exchange((request, response2) -> {
                        String responseBody = response2.bodyTo(String.class);
                        return ResponseEntity.status(response2.getStatusCode()).body(responseBody);
                    });

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            String responseBody = response.getBody();
            assert responseBody != null;
            assertTrue(responseBody.contains("network interface address must be between .1 and .254"));

            Optional<Network> networkFromDb = networkRepository.findByNetworkName("Network1");
            assert networkFromDb.isEmpty();
        }
    }

    @Test
    public void testCreateNetworkRejectsAddressInUse() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);

        ResponseEntity<Network> createNetworkResponse = restClient.post()
                .uri(baseUrl)
                .body(network)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(HttpStatus.OK, createNetworkResponse.getStatusCode());

        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        assert networkFromDb.isPresent();

        Network networkWithAddressAlreadyInUse = new Network();
        networkWithAddressAlreadyInUse.setNetworkName("NotTestNetworkName");
        networkWithAddressAlreadyInUse.setNetworkCidr(testNetworkCidr);
        networkWithAddressAlreadyInUse.setNetworkListenPort(51821);
        networkWithAddressAlreadyInUse.setNetworkTag("not test network tag");

        ResponseEntity<String> failedCreateNetworkResponse = restClient.post()
                .uri(baseUrl)
                .body(networkWithAddressAlreadyInUse)
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    String responseBody = response.bodyTo(String.class);
                    return ResponseEntity.status(statusCode).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, failedCreateNetworkResponse.getStatusCode());

        String errorMsg = new ObjectMapper()
                .readTree(failedCreateNetworkResponse.getBody())
                .get("message")
                .asText();

        assertTrue(errorMsg.contains("network already exists by requested networkName or networkCidr or networkListenPort"));

        networkFromDb = networkRepository.findByNetworkName(networkWithAddressAlreadyInUse.getNetworkName());
        assert networkFromDb.isEmpty();
    }

    @Test
    public void testCreateNetworkRejectsListenPortAlreadyInUse() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);

        ResponseEntity<Network> createNetworkResponse = restClient.post()
                .uri(baseUrl)
                .body(network)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(HttpStatus.OK, createNetworkResponse.getStatusCode());
        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        assert networkFromDb.isPresent();

        Network networkWithListenPortAlreadyInUse = new Network();
        networkWithListenPortAlreadyInUse.setNetworkName("NotTestNetworkName");
        networkWithListenPortAlreadyInUse.setNetworkCidr("192.168.0.1/24");
        networkWithListenPortAlreadyInUse.setNetworkListenPort(testNetworkListenPort); // Same listen port
        networkWithListenPortAlreadyInUse.setNetworkTag("not test network tag");

        ResponseEntity<String> failedCreateNetworkResponse = restClient.post()
                .uri(baseUrl)
                .body(networkWithListenPortAlreadyInUse)
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    String responseBody = response.bodyTo(String.class);
                    return ResponseEntity.status(statusCode).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, failedCreateNetworkResponse.getStatusCode());

        String errorMsg = new ObjectMapper()
                .readTree(failedCreateNetworkResponse.getBody())
                .get("message")
                .asText();

        assertTrue(errorMsg.contains("network already exists by requested networkName or networkCidr or networkListenPort"));

        networkFromDb = networkRepository.findByNetworkName(networkWithListenPortAlreadyInUse.getNetworkName());
        assert networkFromDb.isEmpty();
    }

    @Test
    public void testCreateNetworkRejectsNetworkNameAlreadyInUse() throws Exception {
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);

        ResponseEntity<Network> createNetworkResponse = restClient.post()
                .uri(baseUrl)
                .body(network)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(HttpStatus.OK, createNetworkResponse.getStatusCode());
        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        assert networkFromDb.isPresent();

        Network networkWithNameAlreadyInUse = new Network();
        networkWithNameAlreadyInUse.setNetworkName(testNetworkName);
        networkWithNameAlreadyInUse.setNetworkCidr("192.168.0.1/24");
        networkWithNameAlreadyInUse.setNetworkListenPort(51820);
        networkWithNameAlreadyInUse.setNetworkTag("not test network tag");

        ResponseEntity<String> failedCreateNetworkResponse = restClient.post()
                .uri(baseUrl)
                .body(networkWithNameAlreadyInUse)
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    String responseBody = response.bodyTo(String.class);
                    return ResponseEntity.status(statusCode).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, failedCreateNetworkResponse.getStatusCode());

        String errorMsg = new ObjectMapper()
                .readTree(failedCreateNetworkResponse.getBody())
                .get("message")
                .asText();

        assertTrue(errorMsg.contains("network already exists by requested networkName or networkCidr or networkListenPort"));
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
    public void testDescribeNetworkDescribesNetwork() {
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
    public void testDeleteNetworkDeletesNetwork() {
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
    public void testUpdateNetworkUpdatesNetwork() {
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

