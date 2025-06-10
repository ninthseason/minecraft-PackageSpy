package cc.loveloli.packagespy.client;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

public final class SSEHttpServer {
    private final HttpServer server;
    private final CopyOnWriteArrayList<SseClient> subscribers = new CopyOnWriteArrayList<>();

    public SSEHttpServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/events", this::handleSSE);
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SSE-HTTP"); t.setDaemon(true); return t;
        }));
        server.start();
        System.out.println("[SSE] Listening on http://localhost:" + port + "/events");
    }

    private void handleSSE(HttpExchange ex) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "text/event-stream; charset=utf-8");
        h.add("Cache-Control", "no-cache");
        h.add("Connection", "keep-alive");
        ex.sendResponseHeaders(200, 0);              // 0 → chunked transfer
        OutputStream os = ex.getResponseBody();
        SseClient c = new SseClient(os);
        subscribers.add(c);
        // 发送欢迎消息，触发浏览器 EventSource onopen
        c.sendComment("connected " + System.currentTimeMillis());
    }

    public void broadcast(String event, String json) {
        // 失活就移除
        subscribers.removeIf(c -> !c.send(event, json));
    }

    public void stop() { server.stop(0); }

    /* ----- 内部类：单个客户端连接 ----- */
    private static final class SseClient {
        private final BufferedWriter out;
        SseClient(OutputStream os) { this.out = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)); }
        boolean send(String evt, String data) {
            try {
                out.write(evt + "\n" + data + "\n");
                out.flush();
                return true;
            } catch (IOException e) { return false; }
        }
        void sendComment(String c) { send("", ":" + c); }
    }
}