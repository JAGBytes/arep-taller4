package edu.escuelaing.arem.ASE.app;

import edu.escuelaing.arem.ASE.app.http.HttpServer;
import edu.escuelaing.arem.ASE.app.http.Request;
import edu.escuelaing.arem.ASE.app.http.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class HttpServerTest {

    @BeforeAll
    static void setupAll() throws Exception {
        // Archivos estáticos de prueba
        createTestStaticFiles();
        HttpServer.staticfiles("/test-static");

        // Registrar servicios
        registerTestServices();
    }

    @BeforeEach
    void setupEach() {
        HttpServer.getUsers().clear();
        HttpServer.loadInitialData();
    }

    @AfterAll
    static void tearDownAll() {
        cleanupTestFiles();
    }

    // ================== Archivos de prueba ==================
    private static void createTestStaticFiles() throws Exception {
        Path testDir = Paths.get("target/classes/test-static");
        Files.createDirectories(testDir);
        Files.writeString(testDir.resolve("index.html"), "<html><body><h1>Test Index</h1></body></html>");
        Files.writeString(testDir.resolve("style.css"), "body { background-color: #f0f0f0; }");
        Files.writeString(testDir.resolve("data.json"), "{\"message\": \"Hello World\", \"status\": \"ok\"}");
        Path subDir = testDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("nested.txt"), "This is a nested file");
    }

    private static void cleanupTestFiles() {
        try {
            Path testDir = Paths.get("target/classes/test-static");
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException ignored) {
        }
    }

    // ================== Servicios de prueba ==================
    private static void registerTestServices() {
        HttpServer.get("/api/hello", (req, res) -> {
            String name = req.getQueryParam("name");
            String msg = name != null ? "Hello " + name + "!" : "Hello World!";
            return new Response.Builder().withStatus(200).withBody("{\"message\":\"" + msg + "\"}").build();
        });

        HttpServer.get("/api/users", (req, res) -> {
            var users = HttpServer.getUsers();
            StringBuilder sb = new StringBuilder("{\"users\":[");
            int i = 0;
            for (var entry : users.entrySet()) {
                sb.append("{\"id\":\"").append(entry.getKey()).append("\",\"name\":\"").append(entry.getValue()).append("\"}");
                if (i++ < users.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]}");
            return new Response.Builder().withStatus(200).withBody(sb.toString()).build();
        });

        HttpServer.post("/api/users", (req, res) -> {
            String name = req.getJsonValue("name");
            if (name == null || name.isBlank()) {
                return new Response.Builder().withStatus(400).withBody("{\"error\":\"Name is required\"}").build();
            }
            HttpServer.addUser(name);
            return new Response.Builder().withStatus(201).withBody("{\"message\":\"User created\",\"name\":\"" + name + "\"}").build();
        });
    }

    // ================== Helpers ==================
    private String doGet(String path) throws Exception {
        URI uri = new URI(path);
        return new String(HttpServer.handleGetRequest(uri));
    }

    private String doPost(String path, String body) throws Exception {
        // Construir una petición HTTP POST completa simulada
        String rawRequest = "POST " + path + " HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n"
                + "\r\n"
                + body;

        // BufferedReader para simular el flujo de entrada del socket
        BufferedReader in = new BufferedReader(new StringReader(rawRequest));

        // Crear URI del path
        URI uri = new URI(path);

        // Llamar al método handlePostRequest del servidor y devolver la respuesta
        return new String(HttpServer.handlePostRequest(uri, in), StandardCharsets.UTF_8);
    }

    // ================== Tests ==================
    @Test
    void testLoadInitialData() {
        var users = HttpServer.getUsers();
        assertEquals(3, users.size());
        assertTrue(users.containsValue("Andres"));
        assertTrue(users.containsValue("Maria"));
        assertTrue(users.containsValue("Carlos"));
    }

    @Test
    void testAddUser() {
        int initial = HttpServer.getUsers().size();
        HttpServer.addUser("TestUser");
        assertEquals(initial + 1, HttpServer.getUsers().size());
    }

    @Test
    void testGetHelloWithParams() throws Exception {
        String resp = doGet("/api/hello?name=Juan");
        assertTrue(resp.contains("Hello Juan!"));
    }

    @Test
    void testGetUsers() throws Exception {
        String resp = doGet("/api/users");
        assertTrue(resp.contains("Andres"));
        assertTrue(resp.contains("Maria"));
        assertTrue(resp.contains("Carlos"));
    }

    @Test
    void testPostCreateUser() throws Exception {
        String resp = doPost("/api/users", "{\"name\":\"NewUser\"}");
        assertTrue(resp.contains("User created"));
        assertTrue(HttpServer.getUsers().containsValue("NewUser"));
    }

    @Test
    void testStaticFileIndex() throws Exception {
        String resp = doGet("/");
        assertTrue(resp.contains("Test Index"));
    }

    @Test
    void testStaticFileCss() throws Exception {
        String resp = doGet("/style.css");
        assertTrue(resp.contains("background-color"));
    }

    @Test
    void testPathTraversalBlocked() throws Exception {
        String resp = doGet("/../../../etc/passwd");
        assertTrue(resp.contains("404") || resp.contains("Forbidden"));
    }

    @Test
    void testPathTraversalWithEncoding() throws Exception {
        String resp = doGet("/%2E%2E%2Fetc%2Fpasswd");
        assertTrue(resp.contains("404") || resp.contains("Forbidden"));
    }
}
