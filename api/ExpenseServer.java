import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;

public class ExpenseServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/health", ex -> send(ex, "{\"status\":\"ok\"}"));
        server.start();
        System.out.println("http://localhost:8080");
    }

    static void send(HttpExchange ex, String body) throws IOException {
        byte[] b = body.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, b.length);
        ex.getResponseBody().write(b);
        ex.getResponseBody().close();
    }
}
