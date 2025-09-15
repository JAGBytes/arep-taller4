package edu.escuelaing.arem.ASE.app;

import edu.escuelaing.arem.ASE.app.http.HttpServer;
import edu.escuelaing.arem.ASE.app.http.Response;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultithreadedServerTest {

    private static final int TEST_PORT = 35003;
    private Thread serverThread;

    @BeforeAll
    void startServer() throws Exception {
        HttpServer.port = TEST_PORT;

        // Registrar endpoint simple usando Response.Builder
        HttpServer.get("/api/echo", (req, res) -> {
            String msg = req.getQueryParam("msg");
            return new Response.Builder()
                    .withStatus(200)
                    .withContentType("application/json")
                    .withBody("{\"echo\":\"" + msg + "\"}")
                    .build();
        });

        // Iniciar servidor en hilo aparte
        serverThread = new Thread(() -> {
            try {
                HttpServer.startServer(new String[]{});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(500); // Esperar que el servidor esté listo
    }

    @AfterAll
    void stopServer() {
        // Podrías agregar lógica para cerrar el servidor si tu HttpServer tiene shutdown
    }

    @Test
    void testConcurrentGetRequests() throws InterruptedException {
        int clientCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(clientCount);

        for (int i = 0; i < clientCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    String msg = "msg" + id;
                    String response = sendHttpRequest("GET", "/api/echo?msg=" + msg, "");
                    if (response.contains("{\"echo\":\"" + msg + "\"}")) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(clientCount, successCount.get(), "Todos los clientes deberían recibir la respuesta correcta");
    }

    // ================== MÉTODO AUXILIAR ==================
    private String sendHttpRequest(String method, String path, String body) throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Construir la solicitud HTTP
            out.println(method + " " + path + " HTTP/1.1");
            out.println("Host: localhost:" + TEST_PORT);
            if (!body.isEmpty()) {
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + body.getBytes().length);
                out.println();
                out.print(body);
            } else {
                out.println();
            }
            out.flush();

            // Leer la respuesta HTTP (solo el body)
            StringBuilder response = new StringBuilder();
            String line;
            boolean bodyStarted = false;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) {
                    bodyStarted = true; // siguiente línea es body
                    continue;
                }
                if (bodyStarted) {
                    response.append(line);
                }
            }
            return response.toString();
        }
    }
}
