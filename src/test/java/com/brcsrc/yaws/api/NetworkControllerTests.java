package com.brcsrc.yaws.api;

import aj.org.objectweb.asm.TypeReference;
import com.brcsrc.yaws.model.Network;
import com.brcsrc.yaws.persistence.NetworkRepository;
import com.brcsrc.yaws.service.NetworkService;
import com.brcsrc.yaws.utility.TestHelpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NetworkControllerTests {

    // this will inject the random port the app is running on
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private NetworkRepository networkRepository;

    private String baseUrl;

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port + "/api/v1/networks";
    }

    @Test
    public void testCreateNetworkCreatesNetwork() throws Exception {
        // setup
        Network network = new Network();
        network.setNetworkName("Network1");
        network.setNetworkCidr("10.100.0.0/24");
        network.setNetworkListenPort(51820);
        network.setNetworkTag("net1");

        HttpEntity<String> entity = TestHelpers.createTestEntity(network);

        // execute
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);

        // assert
        assertEquals(200, response.getStatusCode().value());
        Optional<Network> networkFromDb = networkRepository.findByNetworkName("Network1");
        assert networkFromDb.isPresent();

        // teardown
        networkService.deleteNetwork("Network1");
    }

    @Test
    public void testListNetworkListsNetworks() throws Exception {
        // setup
        Network network = new Network();
        network.setNetworkName("Network2");
        network.setNetworkCidr("10.101.0.0/24");
        network.setNetworkListenPort(51821);
        network.setNetworkTag("net2");

        HttpEntity<String> createNetworkRequest = TestHelpers.createTestEntity(network);

        ResponseEntity<String> createNetworkResponse = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                createNetworkRequest,
                String.class
        );

        // execute
        ResponseEntity<String> listNetworkResponse = restTemplate.getForEntity(baseUrl, String.class);

        // assert
        assertEquals(HttpStatus.OK, listNetworkResponse.getStatusCode());
        ObjectMapper objectMapper = new ObjectMapper();
        List<Network> networks = objectMapper.readValue(
                listNetworkResponse.getBody(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Network>>() {}
        );
        assertEquals(1, networks.size()); // Expecting one network to be in the list
        assertEquals("Network2", networks.get(0).getNetworkName());
        assertEquals("10.101.0.0/24", networks.get(0).getNetworkCidr());
        assertEquals(51821, networks.get(0).getNetworkListenPort());
        assertEquals("net2", networks.get(0).getNetworkTag());

        // teardown
        networkService.deleteNetwork("Network2");
    }
}

