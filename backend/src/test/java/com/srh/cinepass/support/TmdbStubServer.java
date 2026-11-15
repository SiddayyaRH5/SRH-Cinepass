package com.srh.cinepass.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A tiny HTTP server that stands in for the TMDB API.
 *
 * <p>It is used instead of a mocked HTTP client so the tests exercise the
 * real request building, the real timeouts and the real JSON parsing. The
 * real TMDB service is never contacted from a test.</p>
 */
public class TmdbStubServer implements AutoCloseable {

    private final HttpServer server;

    private final Map<String, String> bodies = new HashMap<>();
    private final Map<String, Integer> statuses = new HashMap<>();
    private final Map<String, Long> delays = new HashMap<>();

    private final List<String> requests =
            Collections.synchronizedList(new ArrayList<>());

    public TmdbStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);

        // Daemon threads, so a forgotten server never keeps the test JVM alive.
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tmdb-stub-server");
            thread.setDaemon(true);
            return thread;
        }));

        server.start();
    }

    /**
     * The base URL the TMDB client should be pointed at. It mirrors the real
     * API base ({@code https://api.themoviedb.org/3}), so the stub paths look
     * like the production paths.
     */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/3";
    }

    /**
     * Registers a JSON body for a path, for example {@code /3/movie/upcoming}.
     */
    public void respond(String path, String json) {
        bodies.put(path, json);
        statuses.put(path, 200);
    }

    /**
     * Registers a failing response for a path.
     */
    public void respondWithStatus(String path, int status, String json) {
        bodies.put(path, json);
        statuses.put(path, status);
    }

    /**
     * Makes a path answer very slowly, so a read timeout can be exercised.
     */
    public void respondSlowly(String path, long delayMillis, String json) {
        bodies.put(path, json);
        statuses.put(path, 200);
        delays.put(path, delayMillis);
    }

    /**
     * @return every requested path including its query string, in order
     */
    public List<String> requests() {
        return List.copyOf(requests);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        requests.add(query == null ? path : path + "?" + query);

        Long delay = delays.get(path);

        if (delay != null) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        String body = bodies.get(path);
        int status = statuses.getOrDefault(path, body == null ? 404 : 200);

        byte[] payload = body == null
                ? new byte[0]
                : body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }
}
