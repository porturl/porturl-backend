package org.friesoft.porturl.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlloyHealthServiceTest {

    private static HttpServer mockServer;
    private static int mockServerPort;
    private static int currentResponseCode = 200;

    private AlloyHealthService alloyHealthService;

    @BeforeAll
    static void startServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/", exchange -> {
            String response = "OK";
            exchange.sendResponseHeaders(currentResponseCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        mockServer.setExecutor(null);
        mockServer.start();
        mockServerPort = mockServer.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        alloyHealthService = new AlloyHealthService();
        currentResponseCode = 200; // default to OK
    }

    @Test
    void testHealthCheck_Success_WithV1Path() {
        String otlpEndpoint = "http://localhost:" + mockServerPort + "/v1/metrics";
        ReflectionTestUtils.setField(alloyHealthService, "otlpEndpoint", otlpEndpoint);

        // Given the server returns 200 OK (default in setUp)
        
        // Initial state is true
        assertTrue(alloyHealthService.isUp());
        
        alloyHealthService.checkHealth();
        
        // Still true
        assertTrue(alloyHealthService.isUp());
    }

    @Test
    void testHealthCheck_Success_WithoutV1Path() {
        String otlpEndpoint = "http://localhost:" + mockServerPort + "/metrics";
        ReflectionTestUtils.setField(alloyHealthService, "otlpEndpoint", otlpEndpoint);

        alloyHealthService.checkHealth();
        
        assertTrue(alloyHealthService.isUp());
    }

    @Test
    void testHealthCheck_Failure_500() {
        String otlpEndpoint = "http://localhost:" + mockServerPort + "/v1/metrics";
        ReflectionTestUtils.setField(alloyHealthService, "otlpEndpoint", otlpEndpoint);

        // Server returns 500 Internal Server Error
        currentResponseCode = 500;

        alloyHealthService.checkHealth();
        
        assertFalse(alloyHealthService.isUp());
    }

    @Test
    void testHealthCheck_Failure_ConnectionRefused() {
        // Point to a port that's not listening
        String otlpEndpoint = "http://localhost:" + (mockServerPort + 10) + "/v1/metrics";
        ReflectionTestUtils.setField(alloyHealthService, "otlpEndpoint", otlpEndpoint);

        alloyHealthService.checkHealth();
        
        assertFalse(alloyHealthService.isUp());
    }

    @Test
    void testHealthCheck_Recovery_DownToUp() {
        String otlpEndpoint = "http://localhost:" + mockServerPort + "/v1/metrics";
        ReflectionTestUtils.setField(alloyHealthService, "otlpEndpoint", otlpEndpoint);

        // First, it goes down
        currentResponseCode = 500;
        alloyHealthService.checkHealth();
        assertFalse(alloyHealthService.isUp());

        // Then, it recovers
        currentResponseCode = 200;
        alloyHealthService.checkHealth();
        assertTrue(alloyHealthService.isUp());
    }
}
