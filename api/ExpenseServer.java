import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

public class ExpenseServer {
    private static Connection conn;

    public static void main(String[] args) throws Exception {
        initDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/health", ExpenseServer::handleHealth);
        server.createContext("/api/transactions", ExpenseServer::handleTransactions);
        server.start();
        System.out.println("Expense Server started on http://localhost:8080");
    }

    private static void initDatabase() {
        String host = getEnv("DB_HOST", "localhost");
        String port = getEnv("DB_PORT", "5432");
        String dbName = getEnv("DB_NAME", "app");
        String user = getEnv("DB_USER", "postgres");
        String pass = getEnv("DB_PASSWORD", "1234");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

        System.out.println("Connecting to database at " + url + "...");
        int attempts = 15;
        while (attempts > 0) {
            try {
                conn = DriverManager.getConnection(url, user, pass);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                            "id SERIAL PRIMARY KEY, " +
                            "type VARCHAR(20) NOT NULL, " +
                            "amount NUMERIC(10, 2) NOT NULL, " +
                            "category_id INT, " +
                            "date VARCHAR(20) NOT NULL, " +
                            "comment VARCHAR(255)" +
                            ");");
                }
                System.out.println("Connected to PostgreSQL and transactions table initialized.");
                return;
            } catch (Exception e) {
                attempts--;
                System.out.println("Waiting for PostgreSQL (" + e.getMessage() + "), attempts left: " + attempts);
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
        System.err.println("Could not connect to PostgreSQL after multiple attempts.");
    }

    private static void handleHealth(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendResponse(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        boolean dbOk = false;
        try {
            if (conn != null && !conn.isClosed()) {
                try (Statement s = conn.createStatement()) {
                    s.execute("SELECT 1");
                    dbOk = true;
                }
            }
        } catch (Exception ignored) {}
        String json = "{\"status\":\"ok\",\"database\":\"" + (dbOk ? "connected" : "disconnected") + "\"}";
        sendResponse(ex, 200, json);
    }

    private static void handleTransactions(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String path = ex.getRequestURI().getPath(); // e.g. /api/transactions or /api/transactions/1
        String[] parts = path.split("/");

        try {
            if (parts.length <= 3) {
                // /api/transactions
                if ("GET".equals(method)) {
                    listTransactions(ex);
                } else if ("POST".equals(method)) {
                    createTransaction(ex);
                } else {
                    sendResponse(ex, 405, "{\"error\":\"Method not allowed\"}");
                }
            } else if (parts.length == 4) {
                // /api/transactions/{id}
                int id = Integer.parseInt(parts[3]);
                if ("GET".equals(method)) {
                    getTransaction(ex, id);
                } else if ("PUT".equals(method)) {
                    updateTransaction(ex, id);
                } else if ("DELETE".equals(method)) {
                    deleteTransaction(ex, id);
                } else {
                    sendResponse(ex, 405, "{\"error\":\"Method not allowed\"}");
                }
            } else {
                sendResponse(ex, 404, "{\"error\":\"Not found\"}");
            }
        } catch (NumberFormatException e) {
            sendResponse(ex, 400, "{\"error\":\"Invalid ID format\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void listTransactions(HttpExchange ex) throws Exception {
        ensureConnection();
        List<String> items = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, type, amount, category_id, date, comment FROM transactions ORDER BY id ASC")) {
            while (rs.next()) {
                items.add(formatTransaction(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getDouble(3),
                        rs.getInt(4),
                        rs.getString(5),
                        rs.getString(6)
                ));
            }
        }
        sendResponse(ex, 200, "[" + String.join(",", items) + "]");
    }

    private static void createTransaction(HttpExchange ex) throws Exception {
        ensureConnection();
        String body = readBody(ex);
        String type = extractJsonField(body, "type");
        double amount = extractJsonDouble(body, "amount");
        int categoryId = extractJsonInt(body, "category_id");
        String date = extractJsonField(body, "date");
        String comment = extractJsonField(body, "comment");

        if (type == null || date == null) {
            sendResponse(ex, 400, "{\"error\":\"Missing required fields: type, amount, date\"}");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO transactions (type, amount, category_id, date, comment) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            ps.setString(1, type);
            ps.setDouble(2, amount);
            ps.setInt(3, categoryId);
            ps.setString(4, date);
            ps.setString(5, comment != null ? comment : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    sendResponse(ex, 201, formatTransaction(id, type, amount, categoryId, date, comment));
                    return;
                }
            }
        }
        sendResponse(ex, 500, "{\"error\":\"Failed to create transaction\"}");
    }

    private static void getTransaction(HttpExchange ex, int id) throws Exception {
        ensureConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, type, amount, category_id, date, comment FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendResponse(ex, 200, formatTransaction(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getDouble(3),
                            rs.getInt(4),
                            rs.getString(5),
                            rs.getString(6)
                    ));
                    return;
                }
            }
        }
        sendResponse(ex, 404, "{\"error\":\"Transaction not found\"}");
    }

    private static void updateTransaction(HttpExchange ex, int id) throws Exception {
        ensureConnection();
        String body = readBody(ex);
        String type = extractJsonField(body, "type");
        double amount = extractJsonDouble(body, "amount");
        int categoryId = extractJsonInt(body, "category_id");
        String date = extractJsonField(body, "date");
        String comment = extractJsonField(body, "comment");

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE transactions SET type = ?, amount = ?, category_id = ?, date = ?, comment = ? WHERE id = ?")) {
            ps.setString(1, type);
            ps.setDouble(2, amount);
            ps.setInt(3, categoryId);
            ps.setString(4, date);
            ps.setString(5, comment != null ? comment : "");
            ps.setInt(6, id);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                sendResponse(ex, 200, formatTransaction(id, type, amount, categoryId, date, comment));
                return;
            }
        }
        sendResponse(ex, 404, "{\"error\":\"Transaction not found\"}");
    }

    private static void deleteTransaction(HttpExchange ex, int id) throws Exception {
        ensureConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                sendResponse(ex, 200, "{\"status\":\"deleted\",\"id\":" + id + "}");
                return;
            }
        }
        sendResponse(ex, 404, "{\"error\":\"Transaction not found\"}");
    }

    private static void ensureConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            initDatabase();
        }
    }

    private static String formatTransaction(int id, String type, double amount, int categoryId, String date, String comment) {
        return "{\"id\":" + id +
                ",\"type\":\"" + escapeJson(type) + "\"" +
                ",\"amount\":" + amount +
                ",\"category_id\":" + categoryId +
                ",\"date\":\"" + escapeJson(date) + "\"" +
                ",\"comment\":\"" + escapeJson(comment != null ? comment : "") + "\"}";
    }

    private static String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static double extractJsonDouble(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private static int extractJsonInt(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*([0-9]+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendResponse(HttpExchange ex, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String getEnv(String key, String def) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : def;
    }
}
