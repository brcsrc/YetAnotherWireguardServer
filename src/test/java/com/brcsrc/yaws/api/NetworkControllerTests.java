package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkStatus;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.requests.UpdateNetworkRequest;
import com.brcsrc.yaws.model.wireguard.NetworkConfig;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.service.NetworkService;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.utility.WireguardConfigReaderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // required for using @BeforeAll/@AfterAll outside of static context
public class NetworkControllerTests {

    @LocalServerPort
    private int port;

    @Autowired
    private NetworkService networkService;
    @Autowired
    private NetworkRepository networkRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    private String baseUrl;
    private final String testNetworkName = "Network1";
    private final String testNetworkCidr = "10.100.0.1/24";
    private final String testNetworkTag = "network_1_tag";
    private final int testNetworkListenPort = 51820;

    private final String testUserName = "admin";
    private final String testPassword = "changeme";
    private String jwt;

    private final RestClient restClient = RestClient.create();

    private static final Logger logger = LoggerFactory.getLogger(NetworkControllerTests.class);

    @BeforeAll
    public void setupAll() {
        logger.info("creating test admin user");
        User testAdminUser = new User();
        testAdminUser.setUserName(testUserName);
        testAdminUser.setPassword(testPassword);
        userService.createAdminUser(testAdminUser);

        logger.info("authenticating as test admin user");
        jwt = userService.authenticateAndIssueToken(testAdminUser);
    }

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port + "/api/v1/networks";
    }

    @AfterEach
    public void teardown() {
        logger.info("cleaning up existing test networks");
        List<Network> networks = this.networkRepository.findAll();
        for (Network network : networks) {
            this.networkService.deleteNetwork(network.getNetworkName());
        }
    }

    @AfterAll
    public void teardownAll() {
        Optional<User> testAdminUserOpt = userRepository.findByUserName(testUserName);
        if (testAdminUserOpt.isPresent()) {
            User testAdminUser = testAdminUserOpt.get();
            logger.info("cleaning up test admin user");
            userRepository.delete(testAdminUser);
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
                    .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
                    .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
        networkWithListenPortAlreadyInUse.setNetworkTag("not_test_network_tag");

        ResponseEntity<String> failedCreateNetworkResponse = restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
        networkWithNameAlreadyInUse.setNetworkTag("not_test_network_tag");

        ResponseEntity<String> failedCreateNetworkResponse = restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
                .retrieve()
                .toEntity(String.class);

        assertEquals(HttpStatus.OK, listNetworkResponse.getStatusCode());
        ObjectMapper objectMapper = new ObjectMapper();
        List<Network> networks = objectMapper.readValue(
                listNetworkResponse.getBody(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Network>>() {
        }
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
    public void testUpdateNetworkTagOnly() {
        // Step 1: Create a test network
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);
        networkService.createNetwork(network);

        // Step 2: Update the networkTag with valid input
        String newTag = "updated-tag";
        UpdateNetworkRequest updateRequest = new UpdateNetworkRequest();
        updateRequest.setNetworkTag(newTag);

        String updateNetworkUrl = String.format("%s/%s", baseUrl, testNetworkName);

        ResponseEntity<Network> response = restClient.patch()
                .uri(updateNetworkUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(updateRequest)
                .retrieve()
                .toEntity(Network.class);

        // Step 3: Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Network updatedNetwork = response.getBody();
        assert updatedNetwork != null;
        assertEquals(newTag, updatedNetwork.getNetworkTag());
        assertEquals(NetworkStatus.ACTIVE, updatedNetwork.getNetworkStatus()); // Status should remain unchanged

        // Step 4: Test trimming of spaces
        String tagWithSpaces = "   trimmed-tag   ";
        updateRequest.setNetworkTag(tagWithSpaces);

        response = restClient.patch()
                .uri(updateNetworkUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(updateRequest)
                .retrieve()
                .toEntity(Network.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        updatedNetwork = response.getBody();
        assert updatedNetwork != null;
        assertEquals("trimmed-tag", updatedNetwork.getNetworkTag()); // Spaces should be trimmed

        // Step 5: Test invalid characters
        String invalidTag = "invalid@tag!";
        updateRequest.setNetworkTag(invalidTag);

        HttpClientErrorException.BadRequest exception = assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            restClient.patch()
                    .uri(updateNetworkUrl)
                    .header("Authorization", String.format("Bearer %s", jwt))
                    .body(updateRequest)
                    .retrieve()
                    .toEntity(Network.class);
        });

        // Check if the response body contains the expected error message
        String responseBody = exception.getResponseBodyAsString();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.contains("Invalid networkTag")); // Ensure the error message matches
    }

    @Test
    public void testUpdateNetworkStatusOnly() {
        // Step 1: Create a test network
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);
        networkService.createNetwork(network);

        // Step 2: Update the networkStatus
        UpdateNetworkRequest updateRequest = new UpdateNetworkRequest();
        updateRequest.setNetworkStatus(NetworkStatus.INACTIVE);

        String updateNetworkUrl = String.format("%s/%s", baseUrl, testNetworkName);

        ResponseEntity<Network> response = restClient.patch()
                .uri(updateNetworkUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(updateRequest)
                .retrieve()
                .toEntity(Network.class);

        // Step 3: Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Network updatedNetwork = response.getBody();
        assert updatedNetwork != null;
        assertEquals(NetworkStatus.INACTIVE, updatedNetwork.getNetworkStatus());
        assertEquals(testNetworkTag, updatedNetwork.getNetworkTag()); // Tag should remain unchanged
    }

    @Test
    public void testUpdateNetworkTagAndStatus() {
        // Step 1: Create a test network
        Network network = new Network();
        network.setNetworkName(testNetworkName);
        network.setNetworkCidr(testNetworkCidr);
        network.setNetworkListenPort(testNetworkListenPort);
        network.setNetworkTag(testNetworkTag);
        networkService.createNetwork(network);

        // Step 2: Update both networkTag and networkStatus
        String newTag = "updated_tag";
        UpdateNetworkRequest updateRequest = new UpdateNetworkRequest();
        updateRequest.setNetworkTag(newTag);
        updateRequest.setNetworkStatus(NetworkStatus.INACTIVE);

        String updateNetworkUrl = String.format("%s/%s", baseUrl, testNetworkName);

        ResponseEntity<Network> response = restClient.patch()
                .uri(updateNetworkUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(updateRequest)
                .retrieve()
                .toEntity(Network.class);

        // Step 3: Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Network updatedNetwork = response.getBody();
        assert updatedNetwork != null;
        assertEquals(newTag, updatedNetwork.getNetworkTag());
        assertEquals(NetworkStatus.INACTIVE, updatedNetwork.getNetworkStatus());
    }
}
