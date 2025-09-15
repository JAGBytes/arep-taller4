Framework Web en Java para Servicios REST con Anotaciones, Reflexión y Multihilo
Este proyecto implementa un framework web completo en Java que evoluciona desde un servidor web básico hacia una plataforma robusta para el desarrollo de aplicaciones web con servicios REST backend. El framework ahora incluye sistema de anotaciones, carga automática de componentes mediante reflexión, arquitectura MVC moderna, y procesamiento multihilo concurrente.

Nuevas Características Agregadas
Sistema de Procesamiento Multihilo Mejorado
ClientHandler dedicado: Clase especializada que implementa Runnable para manejar cada cliente en hilos separados

Timeout configurable: Timeout de 30 segundos para evitar conexiones colgadas

Manejo robusto de errores: Captura y registro detallado de excepciones

Logging mejorado: Sistema de logging con información de hilos, clientes y tiempos de procesamiento

Soporte para métodos HTTP adicionales: Implementación completa de HEAD y OPTIONS

Mejoras en el Manejo de Clientes
Identificación única de clientes: Generación de IDs únicos para tracking

Estadísticas de procesamiento: Medición de tiempos de procesamiento por cliente

Cierre seguro de conexiones: Manejo adecuado de recursos y cierre de sockets

Respuestas de error específicas: Respuestas HTTP apropiadas para diferentes errores

Optimizaciones de Rendimiento
Pool de hilos configurable: Hasta 50 hilos concurrentes (configurable)

Manejo eficiente de memoria: Uso de buffers optimizados para lectura/escritura

Procesamiento no bloqueante: Timeouts en sockets para evitar bloqueos

Gestión de recursos: Cierre automático de streams y sockets

Pruebas y Validación
Suite Completa de Pruebas Implementadas
El framework incluye una suite completa de pruebas que validan todas las funcionalidades:

Pruebas Unitarias (HttpServerTest)
java
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
void testStaticFileIndex() throws Exception {
    String resp = doGet("/");
    assertTrue(resp.contains("Test Index"));
}

@Test
void testPathTraversalBlocked() throws Exception {
    String resp = doGet("/../../../etc/passwd");
    assertTrue(resp.contains("404") || resp.contains("Forbidden"));
}
Pruebas de Controladores con Anotaciones (SimpleControllerTest)
java
@Test
@DisplayName("Test endpoint /greeting")
void testHelloEndpoint() throws Exception {
    URI testUri = new URI("/greeting");
    byte[] response = HttpServer.handleGetRequest(testUri);
    
    String responseStr = new String(response);
    assertTrue(responseStr.contains("200 OK"));
    assertTrue(responseStr.contains("Hola Mundo!"));
}

@Test
@DisplayName("Test endpoint /hello con parámetro")
void testRequestParam() throws Exception {
    URI testUri = new URI("/hello?name=Jorge");
    byte[] response = HttpServer.handleGetRequest(testUri);
    
    String responseStr = new String(response);
    assertTrue(responseStr.contains("200 OK"));
    assertTrue(responseStr.contains("Hola, Jorge!"));
}

@Test
@DisplayName("Test MathController - /add suma")
void testMultipleControllers() throws Exception {
    URI testUri = new URI("/add?a=3&b=7");
    byte[] response = HttpServer.handleGetRequest(testUri);
    
    String responseStr = new String(response);
    assertTrue(responseStr.contains("200 OK"));
    assertTrue(responseStr.contains("Result: 10"));
}

@Test
@DisplayName("Test MathController - números inválidos")
void testMathInvalidNumbers() throws Exception {
    URI testUri = new URI("/add?a=abc&b=5");
    byte[] response = HttpServer.handleGetRequest(testUri);
    
    String responseStr = new String(response);
    assertTrue(responseStr.contains("200 OK"));
    assertTrue(responseStr.contains("Error: Invalid numbers"));
}
Pruebas de Concurrencia (MultithreadedServerTest)
java
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

    assertEquals(clientCount, successCount.get(), 
        "Todos los clientes deberían recibir la respuesta correcta");
}
Cobertura de Pruebas
Pruebas unitarias: Validación de componentes individuales

Pruebas de integración: Verificación de la interacción entre componentes

Pruebas de anotaciones: Validación del sistema de reflexión y anotaciones

Pruebas de concurrencia: Evaluación del rendimiento bajo carga

Pruebas de seguridad: Validación contra path traversal attacks

Pruebas de errores: Manejo de casos de error y excepciones

Ejecución de Pruebas
bash
# Ejecutar todas las pruebas
mvn test

# Ejecutar pruebas específicas
mvn test -Dtest=HttpServerTest
mvn test -Dtest=SimpleControllerTest
mvn test -Dtest=MultithreadedServerTest

# Ejecutar con logging detallado
mvn test -Dhttp.debug=true
Resultados de las Pruebas
Las pruebas demuestran que el framework:

Es robusto: Maneja correctamente errores y entradas inválidas

Escala bien: Soporta múltiples clientes concurrentes sin degradación del servicio

Es seguro: Previene accesos no autorizados a archivos del sistema

Mantiene la consistencia: Las estructuras de datos thread-safe garantizan la integridad de los datos bajo concurrencia

Características Principales
Framework de Servicios REST con Anotaciones
Anotaciones personalizadas: Sistema completo de anotaciones tipo Spring

Reflection-based routing: Enrutamiento automático basado en anotaciones

Auto-discovery: Descubrimiento automático de controladores

Parameter injection: Inyección automática de parámetros de consulta

Funcionalidades Implementadas
Sistema de Anotaciones

@RestController para marcar controladores

@GetMapping para definir rutas GET

@RequestParam para extraer parámetros

Controladores con Anotaciones

GreetingController: Endpoints de saludo

MathController: Operaciones matemáticas

Carga Automática de Componentes

Scanning automático del classpath

Registro automático de endpoints

Inicialización automática del framework

Sistema Multihilo Avanzado

ClientHandler: Manejo especializado de clientes en hilos separados

Thread Pool: Pool configurable de hasta 50 hilos

Timeout management: Timeouts de 30 segundos para conexiones

Error handling: Manejo robusto de excepciones y errores

Resource management: Gestión eficiente de recursos de red

Testing Completo

Tests de reflexión y anotaciones

Tests de integración del sistema completo

Tests de concurrencia y rendimiento

Validación de carga automática

📋 Requisitos Previos
Java 21 Descargar Java

Apache Maven 3.8+ Instalar Maven

Git Instalar Git

🛠️ Instalación y Ejecución
Pasos para ejecutar el proyecto:
Clonar el repositorio:

bash
git clone https://github.com/JAGBytes/arep-taller3.git
cd arep-taller3
Compilar el proyecto:

bash
mvn clean compile
Ejecutar el servidor:

bash
java -cp target/classes edu.escuelaing.arem.ASE.app.App
Acceder a la aplicación:

text
http://localhost:35000
Alternativas de ejecución:
Usando Maven Exec Plugin:

bash
mvn exec:java -Dexec.mainClass="edu.escuelaing.arem.ASE.app.App"
Ejecutar tests:

bash
mvn test
Ejecutar con logging debug:

bash
java -Dhttp.debug=true -cp target/classes edu.escuelaing.arem.ASE.app.App
Arquitectura del Framework
Componentes Principales:
Procesamiento Multihilo Mejorado
El framework implementa un sistema de procesamiento multihilo mejorado con la clase ClientHandler que maneja cada cliente de forma concurrente:

java
// Pool de hilos para manejo concurrente (hasta 50 hilos)
private static ExecutorService threadPool = Executors.newFixedThreadPool(50);

// Cada cliente se maneja en un hilo separado con ClientHandler
threadPool.submit(new ClientHandler(clientSocket));
Características del sistema multihilo mejorado:

ClientHandler dedicado: Clase especializada que implementa Runnable

Timeout management: 30 segundos de timeout para evitar conexiones colgadas

Logging detallado: Información de hilos, clientes y tiempos de procesamiento

Manejo robusto de errores: Captura y registro detallado de excepciones

Soporte para métodos HTTP: GET, POST, HEAD, OPTIONS

Sistema de Anotaciones
java
@RestController
public class GreetingController {

    @GetMapping("/greeting")
    public static String greeting(@RequestParam String name) {
        return "Hola Mundo!";
    }

    @GetMapping("/hello")
    public static String sayHello(@RequestParam("name") String name) {
        return "Hola, " + name + "!";
    }
}
HttpServer con Reflexión
Auto-discovery: Escaneo automático de controladores

Reflection-based routing: Enrutamiento basado en anotaciones

Parameter injection: Inyección automática de parámetros

Error handling: Manejo robusto de errores de reflexión

Métodos del Framework
loadComponents(String[] args)

Carga automática de controladores usando Reflections

Registro automático de endpoints anotados

Procesamiento de parámetros con @RequestParam

get(String path, Function<Request, Response> handler)

Define servicios REST GET con funciones lambda (legacy)

Compatible con el sistema anterior

post(String path, Function<Request, Response> handler)

Define servicios REST POST (legacy)

Procesamiento de cuerpos JSON

staticfiles(String directory)

Configura directorio de archivos estáticos

Búsqueda en target/classes + directory

Clases de Soporte
Request: Acceso a parámetros, headers, body JSON

Response: Constructor de respuestas HTTP con Builder Pattern

Annotations: Sistema completo de anotaciones personalizadas

Ejemplos de Uso
Controladores con Anotaciones:
GreetingController.java:
java
@RestController
public class GreetingController {

    @GetMapping("/greeting")
    public static String greeting(@RequestParam String name) {
        return "Hola Mundo!";
    }

    @GetMapping("/hello")
    public static String sayHello(@RequestParam("name") String name) {
        return "Hola, " + name + "!";
    }
}
MathController.java:
java
@RestController
public class MathController {

    @GetMapping("/add")
    public static String add(@RequestParam("a") String a, @RequestParam("b") String b) {
        try {
            int numA = Integer.parseInt(a);
            int numB = Integer.parseInt(b);
            return "Result: " + (numA + numB);
        } catch (NumberFormatException e) {
            return "Error: Invalid numbers";
        }
    }
}
Aplicación Principal (App.java):
java
public class App {
    public static void main(String[] args) throws Exception {
        // Configurar archivos estáticos
        HttpServer.staticfiles("/");

        // Los controladores se cargan automáticamente mediante reflexión
        // No es necesario registrar manualmente los endpoints

        // Servicios legacy (compatibilidad)
        HttpServer.get("/pi", (req, res) -> {
            return new Response.Builder()
                .withContentType("text/plain")
                .withBody(String.valueOf(Math.PI))
                .build();
        });

        HttpServer.get("/e", (req, res) -> {
            return new Response.Builder()
                .withContentType("text/plain")
                .withBody(String.valueOf(Math.E))
                .build();
        });

        // Iniciar el servidor
        HttpServer.startServer(args);
    }
}
🌐 Endpoints Disponibles
Servicios con Anotaciones:
GET /greeting → Saludo básico

GET /hello?name=X → Saludo personalizado

GET /add?a=X&b=Y → Suma de dos números

Servicios Legacy (compatibilidad):
GET /pi → Constante matemática PI

GET /e → Número de Euler

POST /app/hello → Registro de usuarios

Archivos Estáticos
GET / → index.html

GET /styles.css → Archivos CSS

GET /scripts.js → Archivos JavaScript

GET /servicio-web.jpg → Imagen del proyecto

Ejemplos de Peticiones
Endpoints con Anotaciones:
bash
# Saludo básico
curl "http://localhost:35000/greeting"
# Respuesta: Hola Mundo!

# Saludo personalizado
curl "http://localhost:35000/hello?name=Juan"
# Respuesta: Hola, Juan!

# Operación matemática
curl "http://localhost:35000/add?a=5&b=3"
# Respuesta: Result: 8

# Números inválidos
curl "http://localhost:35000/add?a=abc&b=5"
# Respuesta: Error: Invalid numbers
Servicios Legacy:
bash
# Constante PI
curl "http://localhost:35000/pi"
# Respuesta: 3.141592653589793

# Número de Euler
curl "http://localhost:35000/e"
# Respuesta: 2.718281828459045

# Verificar usuario registrado
curl "http://localhost:35000/app/hello?name=Juan"
# Respuesta: {"message": "No estás registrado en el sistema."}

# Registrar usuario
curl -X POST -H "Content-Type: application/json" \
     -d '{"name":"NuevoUsuario"}' \
     "http://localhost:35000/app/hello"
# Respuesta: {"message": "Hola NuevoUsuario fuiste registrado exitosamente!"}

# Probar controladores automáticos (cargados por reflexión)
curl "http://localhost:35000/greeting?name=Pedro"
# Respuesta: Hola Mundo!

curl "http://localhost:35000/add?a=5&b=3"
# Respuesta: 8
Pruebas de Concurrencia
El servidor multihilo puede manejar múltiples peticiones simultáneas. Puedes probar la concurrencia ejecutando múltiples peticiones en paralelo:

bash
# Ejecutar múltiples peticiones simultáneas
curl "http://localhost:35000/pi" &
curl "http://localhost:35000/e" &
curl "http://localhost:35000/app/hello?name=Usuario1" &
curl "http://localhost:35000/greeting?name=Usuario2" &
wait
Características observables:

Procesamiento concurrente: Cada petición se maneja en un hilo separado

Logs de concurrencia: El servidor muestra "Cliente conectado, manejado en hilo separado"

Respuestas independientes: Cada cliente recibe su respuesta sin interferencia

Gestión de recursos: Conexiones se cierran automáticamente después del procesamiento

Estructura del Proyecto
text
arep-taller3/
│
├── src/main/
│   ├── java/edu/escuelaing/arem/ASE/app/
│   │   ├── App.java                    # Aplicación principal
│   │   ├── http/
│   │   │   ├── HttpServer.java         # Servidor multihilo con reflexión
│   │   │   ├── ClientHandler.java      # Manejo de clientes en hilos separados
│   │   │   ├── Request.java            # Manejo de peticiones
│   │   │   └── Response.java           # Constructor de respuestas
│   │   ├── annotation/                 # Sistema de anotaciones
│   │   │   ├── GetMapping.java         # Anotación para GET
│   │   │   ├── RequestParam.java       # Anotación para parámetros
│   │   │   └── RestController.java     # Anotación para controladores
│   │   └── Controller/                 # Controladores con anotaciones
│   │       ├── GreetingController.java # Controlador de saludos
│   │       └── MathController.java     # Controlador matemático
│   │
│   └── resources/                      # Archivos estáticos
│       ├── index.html
│       ├── styles.css
│       ├── scripts.js
│       └── servicio-web.jpg
│
├── src/test/java/edu/escuelaing/arem/ASE/app/
│   ├── HttpServerTest.java             # Tests del servidor HTTP
│   ├── SimpleControllerTest.java       # Tests de anotaciones y reflexión
│   └── MultithreadedServerTest.java    # Tests de concurrencia
│
├── target/classes/                     # Archivos compilados
├── pom.xml                            # Configuración Maven con nuevas dependencias
├── README.md                          # Documentación actualizada
└── .gitignore
Características Técnicas
Sistema de Reflexión Implementado:
Runtime annotation processing: Procesamiento de anotaciones en tiempo de ejecución

Method invocation: Invocación dinámica de métodos

Parameter extraction: Extracción automática de parámetros

Protocolo HTTP Implementado:
Headers completos (Content-Type, Content-Length)

Status codes apropiados (200, 400, 404, 500)

Métodos GET, POST, HEAD, OPTIONS

JSON parsing y generación

Sistema Multihilo Avanzado:
Pool de hilos configurable: Hasta 50 hilos concurrentes

ClientHandler especializado: Manejo dedicado por cliente

Timeout management: 30 segundos de timeout por conexión

Logging detallado: Información completa de procesamiento

Manejo robusto de errores: Captura y registro de excepciones

Funcionalidades Destacadas
1. Sistema de Anotaciones Personalizado
Anotaciones tipo Spring Framework

Procesamiento en tiempo de ejecución

Inyección automática de parámetros

2. Carga Automática de Componentes
Descubrimiento automático de controladores

Registro automático de endpoints

Inicialización sin configuración manual

3. Procesamiento Multihilo Avanzado
Concurrencia: Manejo simultáneo de múltiples clientes (hasta 50)

Escalabilidad: Pool de hilos configurable

Aislamiento: Cada cliente se procesa independientemente

Rendimiento: Mejor throughput y latencia reducida

Gestión de recursos: Cierre automático de conexiones y manejo de timeouts

4. Testing Completo
Tests unitarios: Validación de componentes individuales

Tests de integración: Verificación de interacción entre componentes

Tests de anotaciones: Validación del sistema de reflexión

Tests de concurrencia: Evaluación del rendimiento bajo carga

Tests de seguridad: Prevención de path traversal attacks