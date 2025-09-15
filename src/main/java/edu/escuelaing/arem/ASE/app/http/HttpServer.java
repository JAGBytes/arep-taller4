package edu.escuelaing.arem.ASE.app.http;

/**
 *
 * @author jgamb
 */
import edu.escuelaing.arem.ASE.app.annotation.GetMapping;
import edu.escuelaing.arem.ASE.app.annotation.RequestParam;
import edu.escuelaing.arem.ASE.app.annotation.RestController;
import java.net.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.reflections.Reflections;

/**
 * Servidor HTTP multihilo que maneja peticiones GET y POST de forma
 * concurrente.
 *
 * Este servidor implementa funcionalidades básicas de HTTP incluyendo: - Servir
 * archivos estáticos desde el directorio "resources" - API REST para manejo de
 * usuarios en el endpoint "/app/hello" - Soporte para peticiones GET y POST -
 * Manejo concurrente de múltiples clientes usando un pool de hilos
 *
 * El servidor mantiene un registro de usuarios en memoria y proporciona
 * servicios de registro y saludo personalizado de forma thread-safe.
 *
 * @author jgamb
 * @version 2.0
 * @since 1.0
 */
public class HttpServer {

    static public int port = 35000;
    private static ServerSocket serverSocket = null;

    // Estructuras de datos thread-safe
    private static final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BiFunction<Request, Response, Response>> getServices = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BiFunction<Request, Response, Response>> postServices = new ConcurrentHashMap<>();

    private static String staticFilesDirectory = "";

    // Pool de hilos para manejo concurrente de clientes
    private static ExecutorService threadPool;
    private static final int MAX_THREADS = 50; // Número máximo de hilos concurrentes
    private static volatile boolean serverRunning = true;

    /**
     * Método principal que inicia el servidor HTTP multihilo.
     *
     * Carga los datos iniciales, crea un pool de hilos, crea un ServerSocket en
     * el puerto especificado y comienza a escuchar peticiones de clientes de
     * forma concurrente.
     *
     * @param args Argumentos de línea de comandos
     * @throws IOException Si ocurre un error de E/S al crear el socket
     * @throws Exception Si ocurre cualquier otro error inesperado
     */
    public static void startServer(String[] args) throws IOException, Exception {
        loadInitialData();
        loadComponents(args);

        // Inicializar el pool de hilos
        threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        // Agregar shutdown hook para cerrar el pool de hilos correctamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando servidor...");
            shutdownServer();
        }));

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // evita bloqueo indefinido en accept()

            System.out.println("Servidor multihilo escuchando en el puerto " + port);
            System.out.println("Pool de hilos inicializado con " + MAX_THREADS + " hilos");

            runServer();

        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor en el puerto: " + port);
            shutdownServer();
            System.exit(1);
        }
    }

    /**
     * Cierra el servidor de forma ordenada.
     */
    private static void shutdownServer() {
        serverRunning = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando ServerSocket: " + e.getMessage());
            }
        }

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Servidor cerrado correctamente");
    }

    /**
     * Carga y registra automáticamente todos los controladores anotados con
     *
     * @RestController. Usa reflexión para encontrar métodos anotados con
     * @GetMapping y registrarlos.
     */
    public static void loadComponents(String args[]) {
        try {
            // Buscar todas las clases anotadas con @RestController
            Set<Class<?>> controllers = findRestControllers("edu.escuelaing.arem.ASE.app");

            for (Class<?> c : controllers) {
                System.out.println("Cargando controlador: " + c.getName());
                Method[] methods = c.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.isAnnotationPresent(GetMapping.class)) {
                        String mapping = m.getAnnotation(GetMapping.class).value();
                        System.out.println("Registrando endpoint GET: " + mapping + " -> " + m.getName());

                        get(mapping, (req, res) -> {
                            try {
                                Parameter[] parameters = m.getParameters();
                                Object[] methodArgs = new Object[parameters.length];

                                // Procesar parámetros anotados con @RequestParam
                                for (int i = 0; i < parameters.length; i++) {
                                    Parameter param = parameters[i];

                                    if (param.isAnnotationPresent(RequestParam.class)) {
                                        String paramName = param.getAnnotation(RequestParam.class).value();
                                        String paramValue = req.getQueryParam(paramName);
                                        methodArgs[i] = paramValue;
                                    } else {
                                        return new Response.Builder()
                                                .withStatus(400)
                                                .withBody("Parámetro no soportado: " + param.getName())
                                                .build();
                                    }
                                }

                                Object result = m.invoke(null, methodArgs);
                                return new Response.Builder()
                                        .withStatus(200)
                                        .withBody(result != null ? result.toString() : "")
                                        .build();

                            } catch (IllegalAccessException | InvocationTargetException e) {
                                return new Response.Builder()
                                        .withStatus(500)
                                        .withBody("Error interno del servidor: " + e.getMessage())
                                        .build();
                            }
                        });
                    }
                }
            }
        } catch (SecurityException ex) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Carga datos iniciales de usuarios en el sistema.
     */
    public static void loadInitialData() {
        System.out.println("Cargando datos iniciales...");
        addUser("Andres");
        addUser("Maria");
        addUser("Carlos");
        System.out.println("Datos iniciales cargados: " + users.size() + " usuarios");
    }

    /**
     * Registra un nuevo usuario en el sistema usando UUID random thread-safe.
     *
     * @param name Nombre del usuario a registrar
     * @return String con el ID único generado para el usuario
     */
    public static String addUser(String name) {
        String id = UUID.randomUUID().toString();
        users.put(id, name);
        System.out.println("Usuario registrado: " + name + " con ID: " + id);
        return id;
    }

    /**
     * Ejecuta el bucle principal del servidor multihilo.
     */
    public static void runServer() {
        while (serverRunning) {
            try {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool;
                System.out.println("Esperando conexiones... (Hilos activos: "
                        + executor.getActiveCount() + "/" + MAX_THREADS
                        + ", Cola: " + executor.getQueue().size() + ")");

                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket));

            } catch (SocketTimeoutException ste) {
                // timeout esperado, revisa si seguimos corriendo
            } catch (IOException e) {
                if (serverRunning) {
                    System.err.println("Error al aceptar cliente: " + e.getMessage());
                }
            }
        }
        shutdownServer(); // cuando sales del bucle, cierras todo
    }

    /**
     * Maneja las peticiones HTTP GET usando estructuras thread-safe.
     */
    public static byte[] handleGetRequest(URI uriReq) {
        String path = uriReq.getPath();

        // Verificar si es un servicio registrado
        BiFunction<Request, Response, Response> service = getServices.get(path);

        if (service != null) {
            Response res = new Response.Builder().build();
            Request req = new Request.Builder().withUri(uriReq).build();
            Response response = service.apply(req, res);
            return response.toBytes();
        }

        // Manejar archivos estáticos
        try {
            return handleStaticFile(path);
        } catch (IOException e) {
            Response res = new Response.Builder()
                    .withStatus(500)
                    .withBody("500 - Server Error: " + e.getMessage())
                    .build();
            return res.toBytes();
        }
    }

    // Nuevo método para manejar archivos estáticos sin usar File
    private static byte[] handleStaticFile(String requestPath) throws IOException {
        String decoded = java.net.URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name());

        if (decoded.equals("/") || decoded.isEmpty()) {
            decoded = "/index.html";
        }

        // Normalizar la ruta del recurso
        String resourcePath = (staticFilesDirectory + decoded).replaceFirst("^/", "");

        // Validación básica de seguridad
        if (resourcePath.contains("..") || resourcePath.contains("~")) {
            Response res = new Response.Builder()
                    .withStatus(403)
                    .withBody("{\"error\": \"Forbidden - Invalid path\"}")
                    .build();
            return res.toBytes();
        }

        // Obtener el recurso como InputStream
        InputStream resourceStream = HttpServer.class.getClassLoader().getResourceAsStream(resourcePath);

        if (resourceStream == null) {
            Response res = new Response.Builder()
                    .withStatus(404)
                    .withBody("{\"error\": \"File not found\"}")
                    .build();
            return res.toBytes();
        }

        try {
            // Leer el contenido completo del archivo
            byte[] fileBytes = readAllBytes(resourceStream);

            // Determinar el Content-Type basado en la extensión
            String contentType = determineContentType(resourcePath);

            Response res = new Response.Builder()
                    .withContentType(contentType)
                    .withBodyBytes(fileBytes)
                    .build();

            return res.toBytes();

        } finally {
            resourceStream.close();
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }

        return buffer.toByteArray();
    }

// Método para determinar el Content-Type basado en la extensión del archivo
    private static String determineContentType(String resourcePath) {
        String extension = "";
        int lastDot = resourcePath.lastIndexOf('.');
        if (lastDot > 0) {
            extension = resourcePath.substring(lastDot + 1).toLowerCase();
        }

        return switch (extension) {
            case "html", "htm" ->
                "text/html; charset=utf-8";
            case "css" ->
                "text/css; charset=utf-8";
            case "js" ->
                "application/javascript; charset=utf-8";
            case "json" ->
                "application/json; charset=utf-8";
            case "png" ->
                "image/png";
            case "jpg", "jpeg" ->
                "image/jpeg";
            case "gif" ->
                "image/gif";
            case "svg" ->
                "image/svg+xml";
            case "ico" ->
                "image/x-icon";
            case "txt" ->
                "text/plain; charset=utf-8";
            case "pdf" ->
                "application/pdf";
            default ->
                "application/octet-stream";
        };
    }

    /**
     * Maneja las peticiones HTTP POST usando estructuras thread-safe.
     */
    public static byte[] handlePostRequest(URI uriReq, BufferedReader in) {
        try {
            Map<String, String> headers = new HashMap<>();
            String line;
            int contentLength = 0;

            // Leer headers
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String headerName = parts[0].trim().toLowerCase();
                    String headerValue = parts[1].trim();
                    headers.put(headerName, headerValue);

                    if (headerName.equals("content-length")) {
                        contentLength = Integer.parseInt(headerValue);
                    }
                }
            }

            String body = "";
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            }

            Request req = new Request.Builder()
                    .withUri(uriReq)
                    .withBody(body)
                    .withHeaders(headers)
                    .build();

            String path = uriReq.getPath();

            // Acceso directo - ConcurrentHashMap es thread-safe
            BiFunction<Request, Response, Response> service = postServices.get(path);

            if (service != null) {
                Response res = new Response.Builder().build();
                Response response = service.apply(req, res);
                return response.toBytes();
            }

            return new Response.Builder()
                    .withStatus(404)
                    .withBody("{\"error\": \"Endpoint POST not found\"}")
                    .build().toBytes();

        } catch (IOException e) {
            return new Response.Builder()
                    .withStatus(500)
                    .withBody("{\"error\": \"Server Error: " + e.getMessage() + "\"}")
                    .build().toBytes();
        } catch (NumberFormatException e) {
            return new Response.Builder()
                    .withStatus(400)
                    .withBody("{\"error\": \"Invalid Content-Length header\"}")
                    .build().toBytes();
        }
    }

    // ==================== MÉTODOS PÚBLICOS ====================
    /**
     * Registra un handler para peticiones GET.
     */
    public static void get(String path, BiFunction<Request, Response, Response> handler) {
        getServices.put(path, handler);
        System.out.println("Endpoint GET registrado: " + path);
    }

    /**
     * Registra un handler para peticiones POST.
     */
    public static void post(String path, BiFunction<Request, Response, Response> handler) {
        postServices.put(path, handler);
        System.out.println("Endpoint POST registrado: " + path);
    }

    /**
     * Configura el directorio de archivos estáticos.
     */
    public static void staticfiles(String dir) {
        if (dir == null || dir.isBlank()) {
            staticFilesDirectory = "";
            return;
        }
        String d = dir.startsWith("/") ? dir : "/" + dir;
        if (d.endsWith("/")) {
            d = d.substring(0, d.length() - 1);
        }
        staticFilesDirectory = d;
        System.out.println("Directorio de archivos estáticos configurado: " + staticFilesDirectory);
    }

    /**
     * Obtiene el mapa de usuarios registrados.
     */
    public static ConcurrentHashMap<String, String> getUsers() {
        return users;
    }

    /**
     * Obtiene estadísticas del pool de hilos.
     */
    public static String getThreadPoolStats() {
        if (threadPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool;
            return String.format("Pool Stats - Active: %d, Pool Size: %d, Queue Size: %d, Completed: %d",
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount());
        }
        return "Thread pool stats not available";
    }

    public ConcurrentHashMap<String, BiFunction<Request, Response, Response>> getGetServices() {
        return getServices;
    }

    public ConcurrentHashMap<String, BiFunction<Request, Response, Response>> getPostServices() {
        return postServices;
    }

    // ==================== MÉTODOS PRIVADOS ====================
    /**
     * Resuelve la ruta de un archivo estático solicitado.
     */
    private static File resolveStaticFile(String requestPath) throws IOException {
        String decoded = java.net.URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name());

        if (decoded.equals("/") || decoded.isEmpty()) {
            decoded = "/index.html";
        }

        String resourcePath = (staticFilesDirectory + decoded).replaceFirst("^/", "");
        java.net.URL resourceUrl = HttpServer.class.getClassLoader().getResource(resourcePath);

        if (resourceUrl == null) {
            return null;
        }

        return new File(resourceUrl.getFile());
    }

    /**
     * Busca todas las clases con @RestController dentro de un paquete.
     */
    public static Set<Class<?>> findRestControllers(String packageName) {
        Set<Class<?>> controllers = new HashSet<>();
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if ("file".equals(resource.getProtocol())) {
                    File directory = new File(resource.toURI());
                    findControllersInDirectory(directory, packageName, controllers);
                } else if ("jar".equals(resource.getProtocol())) {
                    findControllersInJar(resource, path, controllers);
                }
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Error buscando controladores: " + e.getMessage());
        }
        return controllers;
    }

    private static void findControllersInDirectory(File directory, String packageName, Set<Class<?>> controllers) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findControllersInDirectory(file, packageName + "." + file.getName(), controllers);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                loadAndCheckClass(className, controllers);
            }
        }
    }

    private static void findControllersInJar(URL jarUrl, String packagePath, Set<Class<?>> controllers) {
        String jarPath = jarUrl.getPath();
        int exclamationIndex = jarPath.indexOf("!");
        if (exclamationIndex != -1) {
            jarPath = jarPath.substring(5, exclamationIndex);
        }

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !entry.isDirectory()) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    loadAndCheckClass(className, controllers);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo JAR: " + e.getMessage());
        }
    }

    private static void loadAndCheckClass(String className, Set<Class<?>> controllers) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(RestController.class)) {
                controllers.add(clazz);
            }
        } catch (ClassNotFoundException e) {
            // Ignora clases no cargables
        }
    }
    
    private static boolean isPathSecure(String requestPath) {
    try {
        // Decodificar y normalizar
        String decoded = java.net.URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name());
        Path normalized = Paths.get(decoded).normalize();

        // Directorio base (resources)
        Path base = Paths.get("src/main/resources").toAbsolutePath().normalize();

        // Resuelve la ruta solicitada contra el base
        Path resolved = base.resolve(normalized).normalize();

        // Si el resolved no empieza con base, es intento de escape
        return resolved.startsWith(base);
    } catch (UnsupportedEncodingException e) {
        return false;
    }
}
}
