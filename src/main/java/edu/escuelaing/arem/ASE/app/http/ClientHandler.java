package edu.escuelaing.arem.ASE.app.http;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Manejador de clientes para el servidor HTTP multihilo.
 * 
 * Esta clase implementa Runnable y se ejecuta en un hilo separado para
 * manejar cada conexión de cliente de forma concurrente. Procesa las
 * peticiones HTTP, determina el método (GET/POST) y delega el procesamiento
 * al HttpServer apropiado.
 * 
 * @author jgamb
 * @version 2.0
 * @since 2.0
 */
public class ClientHandler implements Runnable {
    
    private final Socket clientSocket;
    private final String clientId;
    
    /**
     * Constructor que inicializa el manejador de cliente.
     * 
     * @param clientSocket Socket de conexión con el cliente
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientId = generateClientId();
    }
    
    /**
     * Método principal que se ejecuta cuando el hilo inicia.
     * 
     * Maneja el ciclo completo de procesamiento del cliente:
     * 1. Procesa la petición HTTP
     * 2. Genera la respuesta apropiada 
     * 3. Cierra la conexión limpiamente
     * 4. Maneja errores y logging
     */
    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        
        logInfo(threadName, "Iniciando procesamiento de cliente: " + 
            clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
        
        try {
            handleClient(clientSocket);
            
        } catch (Exception e) {
            logError(threadName, "Error procesando cliente: " + e.getMessage());
            // Opcional: enviar respuesta de error 500 si aún es posible
            sendErrorResponse(500, "Internal Server Error");
            
        } finally {
            closeClientConnection(threadName);
            
            long processingTime = System.currentTimeMillis() - startTime;
            logInfo(threadName, "Cliente desconectado - Tiempo de procesamiento: " + processingTime + "ms");
        }
    }
    
    /**
     * Maneja la conexión HTTP del cliente.
     * 
     * Lee la petición HTTP línea por línea, extrae el método y URI,
     * y delega el procesamiento al método apropiado del HttpServer.
     * 
     * @param clientSocket Socket de conexión con el cliente
     * @throws IOException Si ocurre un error de E/S
     * @throws URISyntaxException Si la URI es inválida
     */
    public void handleClient(Socket clientSocket) throws IOException, URISyntaxException {
        String threadName = Thread.currentThread().getName();
        
        try (OutputStream out = clientSocket.getOutputStream(); 
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String inputLine;
            boolean isFirstLine = true;
            byte[] responseBytes = createBadRequestResponse();
            
            // Timeout para evitar conexiones colgadas
            clientSocket.setSoTimeout(30000); // 30 segundos
            
            while ((inputLine = in.readLine()) != null) {
                logDebug(threadName, "Received: " + inputLine);

                if (isFirstLine) {
                    responseBytes = processFirstLine(inputLine, in, threadName);
                    isFirstLine = false;
                }
                
                // Si no hay más datos disponibles, salir del bucle
                if (!in.ready()) {
                    break;
                }
            }

            // Enviar respuesta al cliente
            out.write(responseBytes);
            out.flush();
            
            logInfo(threadName, "Respuesta enviada exitosamente");
            
        } catch (SocketTimeoutException e) {
            logError(threadName, "Timeout del cliente: " + e.getMessage());
            throw new IOException("Client timeout", e);
        }
    }
    
    /**
     * Procesa la primera línea de la petición HTTP (request line).
     * 
     * Ejemplo: "GET /index.html HTTP/1.1"
     * Extrae el método HTTP y la URI, luego delega el procesamiento.
     * 
     * @param requestLine Primera línea de la petición HTTP
     * @param in BufferedReader para leer el resto de la petición
     * @param threadName Nombre del hilo para logging
     * @return Array de bytes con la respuesta HTTP completa
     */
    private byte[] processFirstLine(String requestLine, BufferedReader in, String threadName) {
        try {
            // Validar formato de la línea de petición
            if (requestLine == null || requestLine.trim().isEmpty()) {
                logError(threadName, "Línea de petición vacía");
                return createBadRequestResponse();
            }
            
            String[] requestParts = requestLine.split(" ");
            
            // Validar que tenga al menos método y URI
            if (requestParts.length < 2) {
                logError(threadName, "Formato de petición inválido: " + requestLine);
                return createBadRequestResponse();
            }
            
            String method = requestParts[0].toUpperCase();
            String uriString = requestParts[1];
            
            // Validar URI
            URI requestUri;
            try {
                requestUri = new URI(uriString);
            } catch (URISyntaxException e) {
                logError(threadName, "URI inválida: " + uriString);
                return createBadRequestResponse();
            }
            
            logInfo(threadName, "Procesando: " + method + " " + requestUri.getPath());
            
            // Delegar según el método HTTP
            return switch (method) {
                case "GET" -> {
                    logDebug(threadName, "Delegando a handleGetRequest");
                    yield HttpServer.handleGetRequest(requestUri);
                }
                case "POST" -> {
                    logDebug(threadName, "Delegando a handlePostRequest");
                    yield HttpServer.handlePostRequest(requestUri, in);
                }
                case "HEAD" -> {
                    // HEAD es como GET pero sin body
                    logDebug(threadName, "Procesando HEAD request");
                    byte[] getResponse = HttpServer.handleGetRequest(requestUri);
                    yield removeBodyFromResponse(getResponse);
                }
                case "OPTIONS" -> {
                    logDebug(threadName, "Procesando OPTIONS request");
                    yield createOptionsResponse();
                }
                default -> {
                    logError(threadName, "Método HTTP no soportado: " + method);
                    yield createMethodNotAllowedResponse(method);
                }
            };
            
        } catch (Exception e) {
            logError(threadName, "Error procesando primera línea: " + e.getMessage());
            return createInternalServerErrorResponse();
        }
    }
    
    /**
     * Cierra la conexión del cliente de forma segura.
     */
    private void closeClientConnection(String threadName) {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logError(threadName, "Error cerrando socket del cliente: " + e.getMessage());
        }
    }
    
    /**
     * Envía una respuesta de error al cliente si es posible.
     */
    private void sendErrorResponse(int statusCode, String message) {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                OutputStream out = clientSocket.getOutputStream();
                String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n\r\n" + message;
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException e) {
            // No se puede hacer nada más, ya estamos en manejo de errores
            System.err.println("No se pudo enviar respuesta de error: " + e.getMessage());
        }
    }
    
    // ==================== MÉTODOS DE RESPUESTAS HTTP ====================
    
    private byte[] createBadRequestResponse() {
        return ("HTTP/1.1 400 Bad Request\r\n" +
               "Content-Type: application/json\r\n" +
               "Connection: close\r\n\r\n" +
               "{\"error\": \"Bad Request\", \"message\": \"Invalid HTTP request format\"}")
               .getBytes(StandardCharsets.UTF_8);
    }
    
    private byte[] createMethodNotAllowedResponse(String method) {
        return ("HTTP/1.1 405 Method Not Allowed\r\n" +
                "Content-Type: application/json\r\n" +
                "Allow: GET, POST, HEAD, OPTIONS\r\n" +
                "Connection: close\r\n\r\n" +
                "{\"error\": \"Method Not Allowed\", \"method\": \"" + method + "\"}")
                .getBytes(StandardCharsets.UTF_8);
    }
    
    private byte[] createInternalServerErrorResponse() {
        return ("HTTP/1.1 500 Internal Server Error\r\n" +
               "Content-Type: application/json\r\n" +
               "Connection: close\r\n\r\n" +
               "{\"error\": \"Internal Server Error\"}")
               .getBytes(StandardCharsets.UTF_8);
    }
    
    private byte[] createOptionsResponse() {
        return ("HTTP/1.1 200 OK\r\n" +
               "Allow: GET, POST, HEAD, OPTIONS\r\n" +
               "Access-Control-Allow-Origin: *\r\n" +
               "Access-Control-Allow-Methods: GET, POST, HEAD, OPTIONS\r\n" +
               "Access-Control-Allow-Headers: Content-Type\r\n" +
               "Connection: close\r\n\r\n")
               .getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Remueve el body de una respuesta HTTP (para peticiones HEAD).
     */
    private byte[] removeBodyFromResponse(byte[] response) {
        String responseStr = new String(response, StandardCharsets.UTF_8);
        int bodyStart = responseStr.indexOf("\r\n\r\n");
        if (bodyStart != -1) {
            return (responseStr.substring(0, bodyStart + 4)).getBytes(StandardCharsets.UTF_8);
        }
        return response;
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Genera un ID único para identificar este cliente en los logs.
     */
    private String generateClientId() {
        return "Client-" + System.currentTimeMillis() % 10000;
    }
    
    /**
     * Obtiene información detallada del cliente para logging.
     */
    public String getClientInfo() {
        if (clientSocket != null) {
            return String.format("Client[%s] %s:%d", 
                clientId,
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort());
        }
        return "Client[" + clientId + "] - disconnected";
    }
    
    // ==================== MÉTODOS DE LOGGING ====================
    
    private void logInfo(String threadName, String message) {
        System.out.println(String.format("[%s][%s] INFO: %s", threadName, clientId, message));
    }
    
    private void logError(String threadName, String message) {
        System.err.println(String.format("[%s][%s] ERROR: %s", threadName, clientId, message));
    }
    
    private void logDebug(String threadName, String message) {
        // Solo mostrar logs de debug si está habilitado
        if (isDebugEnabled()) {
            System.out.println(String.format("[%s][%s] DEBUG: %s", threadName, clientId, message));
        }
    }
    
    /**
     * Determina si el logging de debug está habilitado.
     * Puede ser configurado via system property: -Dhttp.debug=true
     */
    private boolean isDebugEnabled() {
        return Boolean.getBoolean("http.debug");
    }
}