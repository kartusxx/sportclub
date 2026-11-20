package ru.teamname.sportclub.booking.resilience;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Крошечный HTTP-сервер вместо member-service и catalog-service (на базе JDK, без внешних библиотек).
 * Для пути можно задать статус, тело и задержку ответа; сервер считает обращения и запоминает query-строку.
 * Незарегистрированные пути отвечают 404.
 */
public final class StubServer {

    private record Route(int status, String body, long delayMillis) {
    }

    private final HttpServer server;
    private final Map<String, Route> routes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();

    private StubServer(HttpServer server) {
        this.server = server;
    }

    public static StubServer start() {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            StubServer stub = new StubServer(httpServer);
            httpServer.createContext("/", stub::handle);
            httpServer.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "stub-server");
                thread.setDaemon(true);
                return thread;
            }));
            httpServer.start();
            return stub;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void respond(String path, int status, String jsonBody) {
        routes.put(path, new Route(status, jsonBody, 0));
    }

    public void respondWithDelay(String path, int status, String jsonBody, long delayMillis) {
        routes.put(path, new Route(status, jsonBody, delayMillis));
    }

    public int hits(String path) {
        AtomicInteger counter = hits.get(path);
        return counter == null ? 0 : counter.get();
    }

    public String lastQuery() {
        return lastQuery.get();
    }

    public void reset() {
        routes.clear();
        hits.clear();
        lastQuery.set(null);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        hits.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();
        lastQuery.set(exchange.getRequestURI().getQuery());

        Route route = routes.getOrDefault(path, new Route(404, "{}", 0));
        if (route.delayMillis() > 0) {
            try {
                Thread.sleep(route.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] body = route.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(route.status(), body.length == 0 ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } catch (IOException ignored) {
            // клиент мог уже оборвать соединение по таймауту
        } finally {
            exchange.close();
        }
    }
}
