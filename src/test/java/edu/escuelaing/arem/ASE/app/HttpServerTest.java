package edu.escuelaing.arem.ASE.app;

import edu.escuelaing.arem.ASE.app.http.HttpServer;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas para HttpServer con servicios básicos y funcionalidades principales
 */
class HttpServerTest {

    /*    private static final int TEST_PORT = 35002;
    private static Thread serverThread;
    
    @BeforeAll
    static void setUpClass() throws Exception {
    HttpServer.port = TEST_PORT;
    
    // Crear archivos estáticos de prueba
    createTestStaticFiles();
    HttpServer.staticfiles("/test-static");
    
    // Registrar servicios de prueba
    registerTestServices();
    
    // Levantar el servidor en un hilo aparte
    serverThread = new Thread(() -> {
    try {
    HttpServer.getInstance().start();
    } catch (IOException e) {
    throw new RuntimeException(e);
    }
    });
    serverThread.setDaemon(true);
    serverThread.start();
    
    // Esperar a que el servidor esté listo
    Thread.sleep(500);
    }
    
    @BeforeEach
    void setUp() {
    HttpServer.getUsers().clear();
    HttpServer.loadInitialData();
    }
    
    @AfterAll
    static void tearDownClass() {
    cleanupTestFiles();
    }
    
    // ================== CREACIÓN DE ARCHIVOS DE PRUEBA ==================
    private static void createTestStaticFiles() throws IOException {
    Path testDir = Paths.get("target/classes/test-static");
    Files.createDirectories(testDir);
    
    Files.writeString(testDir.resolve("index.html"),
    "<html><body><h1>Test Index</h1></body></html>");
    Files.writeString(testDir.resolve("style.css"),
    "body { background-color: #f0f0f0; }");
    Files.writeString(testDir.resolve("data.json"),
    "{\"message\": \"Hello World\", \"status\": \"ok\"}");
    
    Path subDir = testDir.resolve("subdir");
    Files.createDirectories(subDir);
    Files.writeString(subDir.resolve("nested.txt"),
    "This is a nested file");
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
    } catch (IOException ignored) {}
    }
    
    // ================== REGISTRO DE SERVICIOS ==================
    private static void registerTestServices() {
    HttpServer.get("/api/hello", (req, res) -> {
    String name = req.getQueryParam("name");
    String msg = name != null ? "Hello " + name + "!" : "Hello World!";
    return res.ok("{\"message\":\"" + msg + "\"}");
    });
    
    HttpServer.get("/api/users", (req, res) -> {
    StringBuilder json = new StringBuilder("{\"users\":[");
    var users = HttpServer.getUsers();
    int i = 0;
    for (var entry : users.entrySet()) {
    json.append("{\"id\":\"").append(entry.getKey())
    .append("\",\"name\":\"").append(entry.getValue()).append("\"}");
    if (i < users.size() - 1) json.append(",");
    i++;
    }
    json.append("]}");
    return res.ok(json.toString());
    });
    
    HttpServer.post("/api/users", (req, res) -> {
    String name = req.getJsonValue("name");
    if (name == null || name.isBlank()) {
    return res.badRequest("{\"error\":\"Name is required\"}");
    }
    HttpServer.addUser(name);
    return res.created("{\"message\":\"User created\",\"name\":\"" + name + "\"}");
    });
    }
    
    // ================== PRUEBAS ==================
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
    int initialSize = HttpServer.getUsers().size();
    HttpServer.addUser("TestUser");
    assertEquals(initialSize + 1, HttpServer.getUsers().size());
    }
    
    @Test
    void testGetHelloWithParams() throws Exception {
    String response = sendHttpRequest("GET", "/api/hello?name=Juan", "");
    assertTrue(response.contains("200 OK"));
    assertTrue(response.contains("Hello Juan!"));
    }
    
    @Test
    void testGetUsers() throws Exception {
    String response = sendHttpRequest("GET", "/api/users", "");
    assertTrue(response.contains("200 OK"));
    assertTrue(response.contains("Andres"));
    assertTrue(response.contains("Maria"));
    assertTrue(response.contains("Carlos"));
    }
    
    @Test
    void testPostCreateUser() throws Exception {
    String response = sendHttpRequest("POST", "/api/users", "{\"name\":\"NewUser\"}");
    assertTrue(response.contains("201"));
    assertTrue(response.contains("User created"));
    assertTrue(HttpServer.getUsers().containsValue("NewUser"));
    }
    
    @Test
    void testStaticFileIndex() throws Exception {
    String response = sendHttpRequest("GET", "/", "");
    assertTrue(response.contains("200 OK"));
    assertTrue(response.contains("Test Index"));
    }
    
    @Test
    void testStaticFileCss() throws Exception {
    String response = sendHttpRequest("GET", "/style.css", "");
    assertTrue(response.contains("200 OK"));
    assertTrue(response.contains("background-color"));
    }
    
    @Test
    void testPathTraversalBlocked() throws Exception {
    String response = sendHttpRequest("GET", "/../../../etc/passwd", "");
    assertTrue(response.contains("404"));
    }
    
    @Test
    void testPathTraversalWithEncoding() throws Exception {
    String response = sendHttpRequest("GET", "/%2E%2E%2Fetc%2Fpasswd", "");
    assertTrue(response.contains("404"));
    }
    
    @Test
    void testDirectoryAccessBlocked() throws Exception {
    String response = sendHttpRequest("GET", "/subdir/", "");
    assertTrue(response.contains("404"));
    }
    
    // ================== AUXILIAR ==================
    private String sendHttpRequest(String method, String path, String body) throws IOException {
    try (Socket socket = new Socket("localhost", TEST_PORT);
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
    
    out.println(method + " " + path + " HTTP/1.1");
    out.println("Host: localhost:" + TEST_PORT);
    if (!body.isEmpty()) {
    out.println("Content-Type: application/json");
    out.println("Content-Length: " + body.length());
    out.println();
    out.print(body);
    } else {
    out.println();
    }
    out.flush();
    
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) {
    response.append(line).append("\n");
    if (line.isEmpty()) break;
    }
    
    return response.toString();
    }
    }*/
}
