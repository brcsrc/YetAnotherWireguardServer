package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Client;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.model.NetworkClient;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.requests.CreateNetworkClientRequest;
import com.brcsrc.yaws.model.requests.ListNetworkClientsRequest;
import com.brcsrc.yaws.model.wireguard.ClientConfig;
import com.brcsrc.yaws.persistence.NetworkClientRepository;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.service.NetworkClientService;
import com.brcsrc.yaws.service.NetworkService;
import com.brcsrc.yaws.service.UserService;
import com.brcsrc.yaws.utility.FilepathUtils;
import com.brcsrc.yaws.utility.WireguardConfigReaderUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    private NetworkClientRepository netClientRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

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
    private final String testNetworkEndpoint = "127.0.0.1";
    private final String testClientTag = "client 1 tag";

    private final String testUserName = "admin";
    private final String testPassword = "gH1@#oKl2ff1";
    private String jwt;

    public NetworkClientControllerTests() {
    }

    @BeforeAll
    public void setupAll() {
        logger.info("creating test admin user");
        User testAdminUser = new User();
        testAdminUser.setUserName(testUserName);
        testAdminUser.setPassword(testPassword);
        userService.createAdminUser(testAdminUser);

        logger.info("authenticating as test admin user");
        jwt = userService.authenticateAndIssueToken(testAdminUser).getToken();
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
        List<NetworkClient> networkClients = this.netClientRepository.findAllByNetwork_NetworkName(testNetworkName);
        for (NetworkClient nc : networkClients) {
            logger.info(String.format("cleaning up existing network client '%s'", nc.getClient().getClientName()));
            networkClientService.deleteNetworkClient(testNetworkName, nc.getClient().getClientName());
        }
        Optional<Network> networkFromDb = networkRepository.findByNetworkName(testNetworkName);
        if (networkFromDb.isPresent()) {
            logger.info("cleaning up existing network");
            networkService.deleteNetwork(testNetworkName);
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
    public void testCreateNetworkClientCreatesClient() {
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
                .header("Authorization", String.format("Bearer %s", jwt))
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
        String clientPublicKeyAbsPath = FilepathUtils.getClientKeyPath(testNetworkName, expectedClientPublicKeyFileName);
        String clientPrivateKeyAbsPath = FilepathUtils.getClientKeyPath(testNetworkName, expectedClientPrivateKeyFileName);
        String clientConfigAbsPath = FilepathUtils.getClientConfigPath(testNetworkName, testClientName);
        var filePaths = List.of(
                clientConfigAbsPath,
                clientPrivateKeyAbsPath,
                clientPublicKeyAbsPath
        );
        for (String path : filePaths) {
            assertTrue(Files.exists(Paths.get(path)));
        }

        // assert the network config has a new entry
        // TODO add network peers to NetworkConfig class

        // assert the client config exists
        ClientConfig clientConfig = WireguardConfigReaderUtils.readClientConfig(testNetworkName, testClientName);
        assertEquals(testClientCidr, clientConfig.getNetworkInterface().getAddress());
        assertEquals(testClientDns, clientConfig.getDns());
        assertEquals(testAllowedIps, clientConfig.getPeerConfig().getAllowedIps());
        assertEquals(String.format("%s:%s", testNetworkEndpoint, testNetworkListenPort), clientConfig.getPeerConfig().getEndpoint());

    }

    @Test
    public void testAddClientToNetworkThrowsExceptionForInvalidCidr() {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientDns(testClientDns);
        createNetworkClientRequest.setAllowedIps(testAllowedIps);
        createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);
        createNetworkClientRequest.setClientTag(testClientTag);

        Map<String, String> badCidrsAndAddresses = Map.of(
                "1000.100.0.2/24", "client cidr is not a valid address or cidr block",
                "10.200.0.2/24", "client cidr is outside of corresponding network cidr block"
        );
        badCidrsAndAddresses.forEach((cidr, exceptionMsg) -> {
            createNetworkClientRequest.setClientCidr(cidr);
            ResponseEntity<String> responseEntity = restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", String.format("Bearer %s", jwt))
                    .body(createNetworkClientRequest)
                    .exchange((request, response) -> {
                        String responseBody = response.bodyTo(String.class);
                        return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                    });

            assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
            assertTrue(responseEntity.getBody().contains(exceptionMsg));
        });
    }

    @Test
    public void testAddClientToNetworkThrowsExceptionForInvalidDns() {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientCidr(testClientCidr);
        createNetworkClientRequest.setAllowedIps(testAllowedIps);
        createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);
        createNetworkClientRequest.setClientTag(testClientTag);

        List<String> invalidDnsAddresses = List.of("300.300.300.300", "invalid_dns", "1234", "");

        for (String dns : invalidDnsAddresses) {
            createNetworkClientRequest.setClientDns(dns);
            ResponseEntity<String> responseEntity = restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", String.format("Bearer %s", jwt))
                    .body(createNetworkClientRequest)
                    .exchange((request, response) -> {
                        String responseBody = response.bodyTo(String.class);
                        return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                    });

            assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
            assertTrue(responseEntity.getBody().contains("client dns is not a valid ip address"));
        }
    }

    @Test
    public void testAddClientToNetworkThrowsExceptionForInvalidAllowedIps() {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientCidr(testClientCidr);
        createNetworkClientRequest.setClientDns(testClientDns);
        createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);
        createNetworkClientRequest.setClientTag(testClientTag);

        List<String> invalidAllowedIps = List.of("500.500.500.500/24", "invalid_ip", "1234");

        for (String allowedIp : invalidAllowedIps) {
            createNetworkClientRequest.setAllowedIps(allowedIp);
            ResponseEntity<String> responseEntity = restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", String.format("Bearer %s", jwt))
                    .body(createNetworkClientRequest)
                    .exchange((request, response) -> {
                        String responseBody = response.bodyTo(String.class);
                        return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                    });

            assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
            assertTrue(responseEntity.getBody().contains("allowed ips is not a valid address or cidr block"));
        }
    }

    @Test
    public void testAddClientToNetworkThrowsExceptionForInvalidNetworkEndpoint() {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientCidr(testClientCidr);
        createNetworkClientRequest.setClientDns(testClientDns);
        createNetworkClientRequest.setAllowedIps(testAllowedIps);
        createNetworkClientRequest.setClientTag(testClientTag);

        List<String> invalidEndpoints = List.of("invalid:endpoint", "256.168.1.1", "1234.567.89.10");

        for (String endpoint : invalidEndpoints) {
            createNetworkClientRequest.setNetworkEndpoint(endpoint);
            ResponseEntity<String> responseEntity = restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", String.format("Bearer %s", jwt))
                    .body(createNetworkClientRequest)
                    .exchange((request, response) -> {
                        String responseBody = response.bodyTo(String.class);
                        return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                    });

            assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
            assertTrue(responseEntity.getBody().contains("network endpoint is not valid"));
        }
    }

    @Test
    public void testAddClientToNetworkThrowsExceptionWhenAddressAlreadyInUse() {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientCidr(testClientCidr);
        createNetworkClientRequest.setClientDns(testClientDns);
        createNetworkClientRequest.setAllowedIps(testAllowedIps);
        createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);
        createNetworkClientRequest.setClientTag(testClientTag);

        restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(createNetworkClientRequest)
                .retrieve()
                .toEntity(NetworkClient.class);

        CreateNetworkClientRequest duplicateRequest = new CreateNetworkClientRequest();
        duplicateRequest.setNetworkName(testNetworkName);
        duplicateRequest.setClientName("Client2");
        duplicateRequest.setClientCidr(testClientCidr);
        duplicateRequest.setClientDns(testClientDns);
        duplicateRequest.setAllowedIps(testAllowedIps);
        duplicateRequest.setNetworkEndpoint(testNetworkEndpoint);
        duplicateRequest.setClientTag("test client 2 tag");

        ResponseEntity<String> responseEntity = restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(duplicateRequest)
                .exchange((request, response) -> {
                    String responseBody = response.bodyTo(String.class);
                    return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        String expectedErrMsg = String.format(
                "network %s already has a client with requested name or address",
                testNetworkName
        );
        assertTrue(responseEntity.getBody().contains(expectedErrMsg));
    }

    @Test
    public void testAddClientToNetworkThrowsExceptionWhenNameAlreadyInUse() {
        CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
        createNetworkClientRequest.setNetworkName(testNetworkName);
        createNetworkClientRequest.setClientName(testClientName);
        createNetworkClientRequest.setClientCidr(testClientCidr);
        createNetworkClientRequest.setClientDns(testClientDns);
        createNetworkClientRequest.setAllowedIps(testAllowedIps);
        createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);
        createNetworkClientRequest.setClientTag(testClientTag);

        restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(createNetworkClientRequest)
                .retrieve()
                .toEntity(NetworkClient.class);

        CreateNetworkClientRequest duplicateRequest = new CreateNetworkClientRequest();
        duplicateRequest.setNetworkName(testNetworkName);
        duplicateRequest.setClientName(testClientName);
        duplicateRequest.setClientCidr("10.100.0.3/24");
        duplicateRequest.setClientDns(testClientDns);
        duplicateRequest.setAllowedIps(testAllowedIps);
        duplicateRequest.setNetworkEndpoint(testNetworkEndpoint);
        duplicateRequest.setClientTag("test client 2 tag");

        ResponseEntity<String> responseEntity = restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .body(duplicateRequest)
                .exchange((request, response) -> {
                    String responseBody = response.bodyTo(String.class);
                    return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                });

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        String expectedErrMsg = String.format(
                "network %s already has a client with requested name or address",
                testNetworkName
        );
        assertTrue(responseEntity.getBody().contains(expectedErrMsg));
    }


    @Test
    public void testListNetworkClientsListsNetworkClients() throws IOException, InterruptedException, URISyntaxException {
        String networkOctects = String.join(".", Arrays.copyOfRange(testNetworkCidr.split("\\."), 0, 3));
        Map<String, String> clientNamesCidrsMap = Map.of(
                "client1", String.format("%s.2/24", networkOctects),
                "client2", String.format("%s.3/24", networkOctects),
                "client3", String.format("%s.4/24", networkOctects),
                "client4", String.format("%s.5/24", networkOctects),
                "client5", String.format("%s.6/24", networkOctects)
        );

        clientNamesCidrsMap.forEach((clientName, clientCidr) -> {
            CreateNetworkClientRequest createNetworkClientRequest = new CreateNetworkClientRequest();
            createNetworkClientRequest.setNetworkName(testNetworkName);
            createNetworkClientRequest.setClientName(clientName);
            createNetworkClientRequest.setClientCidr(clientCidr);
            createNetworkClientRequest.setClientDns(testClientDns);
            createNetworkClientRequest.setAllowedIps(testAllowedIps);
            createNetworkClientRequest.setNetworkEndpoint(testNetworkEndpoint);

            restClient.post()
                    .uri(baseUrl)
                    .header("Authorization", String.format("Bearer %s", jwt))
                    .body(createNetworkClientRequest)
                    .retrieve()
                    .toEntity(NetworkClient.class);
        });

        ListNetworkClientsRequest listNetworkClientsRequest = new ListNetworkClientsRequest();
        listNetworkClientsRequest.setNetworkName(testNetworkName);

        // ListNetworkClient is a GET with a request body for page options which is not RFC 7231
        // compliant and we cant use RestClient https://www.rfc-editor.org/rfc/rfc7231#section-4.3.1
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(new URI(baseUrl))
                .header("Authorization", String.format("Bearer %s", jwt))
                .header("Content-Type", "application/json")
                .method(HttpMethod.GET.name(), HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(listNetworkClientsRequest)))
                .build();

        HttpResponse<String> response = HttpClient
                .newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        List<Client> clients = objectMapper.readValue(
                response.body(),
                new TypeReference<List<Client>>(){}
        );

        assertEquals(clientNamesCidrsMap.size(), clients.size());
        for (Client client : clients) {
            assertEquals(clientNamesCidrsMap.get(client.getClientName()), client.getClientCidr());
        }
    }

    @Test
    public void testGetNetworkClientConfigFile() throws Exception {
        // Create a network client
        CreateNetworkClientRequest request = new CreateNetworkClientRequest();
        request.setNetworkName(testNetworkName);
        request.setClientName(testClientName);
        request.setClientCidr(testClientCidr);
        request.setClientDns(testClientDns);
        request.setAllowedIps(testAllowedIps);
        request.setNetworkEndpoint(testNetworkEndpoint);
        request.setClientTag(testClientTag);

        ResponseEntity<NetworkClient> createNetworkClientResponse = restClient.post()
                .uri(baseUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(NetworkClient.class);

        // Assert the response status is 200
        assertEquals(HttpStatus.OK, createNetworkClientResponse.getStatusCode());

        // Verify the client configuration file exists
        String clientConfigAbsPath = FilepathUtils.getClientConfigPath(testNetworkName, testClientName);
        assertTrue(Files.exists(Paths.get(clientConfigAbsPath)), "Client configuration file should exist");

        // Get the config file
        String configUrl = String.format("%s/%s/%s/config", baseUrl, testNetworkName, testClientName);
        ResponseEntity<Resource> response = restClient.get()
                .uri(configUrl)
                .header("Authorization", String.format("Bearer %s", jwt))
                .retrieve()
                .toEntity(Resource.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

    }
}
