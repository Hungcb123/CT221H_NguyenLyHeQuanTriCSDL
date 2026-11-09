package com.ctu.freshgrocery;

import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.service.impl.QdrantServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class QdrantSearchFailureTest {
    @Test
    void httpErrorIsNotTreatedAsNoMatches() throws Exception {
        assertResponse(503, "{\"status\":\"error\"}", false);
    }

    @Test
    void invalidResponseIsNotTreatedAsNoMatches() throws Exception {
        assertResponse(200, "{}", false);
    }

    @Test
    void legitimateEmptySearchReturnsNoMatches() throws Exception {
        assertResponse(200, "{\"result\":[]}", true);
    }

    @Test
    void missingCollectionIsActuallyCreated() throws Exception {
        var created = new java.util.concurrent.atomic.AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/collections/products", exchange -> {
            boolean put = exchange.getRequestMethod().equals("PUT");
            if (put) created.set(true);
            byte[] bytes = (put ? "{\"result\":true}" : "{\"status\":{\"error\":\"Not found\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(put ? 200 : 404, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            QdrantServiceImpl service = new QdrantServiceImpl();
            ReflectionTestUtils.setField(service, "qdrantHost", "127.0.0.1");
            ReflectionTestUtils.setField(service, "qdrantPort", server.getAddress().getPort());
            assertTrue(service.createCollectionIfNotExists("products", 384));
            assertTrue(created.get(), "HTTP 404 must trigger collection creation");
        } finally {
            server.stop(0);
        }
    }

    private void assertResponse(int status, String body, boolean validEmpty) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/collections/products/points/search", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        try {
            QdrantServiceImpl service = new QdrantServiceImpl();
            ReflectionTestUtils.setField(service, "qdrantHost", "127.0.0.1");
            ReflectionTestUtils.setField(service, "qdrantPort", server.getAddress().getPort());
            if (validEmpty) {
                assertTrue(service.search("products", new float[]{1f}, 12, 0.3f).isEmpty());
            } else {
                AppException error = assertThrows(AppException.class,
                        () -> service.search("products", new float[]{1f}, 12, 0.3f));
                assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
            }
        } finally {
            server.stop(0);
        }
    }
}
