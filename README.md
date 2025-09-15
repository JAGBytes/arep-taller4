Framework Web en Java para Servicios REST con Anotaciones, Reflexión y Multihilo
Este proyecto implementa un framework web completo en Java que evoluciona desde un servidor web básico hacia una plataforma robusta para el desarrollo de aplicaciones web con servicios REST backend. El framework incluye sistema de anotaciones, carga automática de componentes mediante reflexión, arquitectura MVC moderna, y procesamiento multihilo concurrente avanzado.

🚀 Características Principales
Sistema de Anotaciones Personalizado: @RestController, @GetMapping, @RequestParam

Carga Automática de Componentes: Descubrimiento automático de controladores mediante reflexión

Arquitectura Multihilo Avanzada: Pool de hasta 50 hilos con ClientHandler dedicado

Protocolo HTTP Completo: Soporte para GET, POST, HEAD, OPTIONS

Sistema de Logging Mejorado: Información detallada de hilos y clientes

Manejo Robust de Errores: Timeouts de 30 segundos y captura de excepciones

Despliegue en AWS EC2: Instancia cloud con Docker preconfigurado

📦 Instalación y Ejecución
Prerrequisitos
Java 21 (Descargar)

Apache Maven 3.8+ (Instalar)

Docker (opcional, para despliegue en contenedores)

Ejecución Local
bash
# 1. Clonar el repositorio
git clone https://github.com/JAGBytes/arep-taller3.git
cd arep-taller3

# 2. Compilar el proyecto
mvn clean compile

# 3. Ejecutar el servidor
java -cp target/classes edu.escuelaing.arem.ASE.app.App

# 4. Acceder a la aplicación
# http://localhost:35000
Ejecución con Maven
bash
mvn exec:java -Dexec.mainClass="edu.escuelaing.arem.ASE.app.App"
Ejecución con Docker (Local)
bash
# Construir la imagen
docker build -t arep-taller4 .

# Ejecutar con Docker Compose
docker-compose up

# O ejecutar directamente
docker run -p 35000:35000 arep-taller4
Ejecución desde Docker Hub
bash
# Descargar y ejecutar la imagen desde Docker Hub
docker pull jorggg/arep-taller4
docker run -p 35000:35000 jorggg/arep-taller4

# O usar docker-compose con imagen remota
# Editar docker-compose.yml y cambiar image: jorggg/arep-taller4
docker-compose up
Despliegue en AWS EC2
bash
# 1. Conectarse a la instancia EC2
ssh -i "tu-key.pem" ubuntu@ec2-tu-instancia.amazonaws.com

# 2. Instalar Docker (si no está instalado)
sudo apt-get update
sudo apt-get install docker.io docker-compose

# 3. Descargar y ejecutar la imagen desde Docker Hub
docker pull jorggg/arep-taller4
docker run -d -p 35000:35000 --name arep-server jorggg/arep-taller4

# 4. Verificar que el contenedor esté corriendo
docker ps

# 5. Probar la aplicación
curl http://localhost:35000/greeting
Ejecutar Pruebas
bash
# Todas las pruebas
mvn test

# Pruebas específicas
mvn test -Dtest=HttpServerTest
mvn test -Dtest=SimpleControllerTest
mvn test -Dtest=MultithreadedServerTest

# Con logging debug
mvn test -Dhttp.debug=true
🏗️ Arquitectura Multihilo Avanzada
ClientHandler Dedicado
Cada cliente es manejado por una instancia especializada de ClientHandler que implementa Runnable:

java
// Pool de 50 hilos para manejo concurrente
private static ExecutorService threadPool = Executors.newFixedThreadPool(50);

// Cada cliente en hilo separado
threadPool.submit(new ClientHandler(clientSocket));
Características del Sistema Multihilo
🔄 Pool Configurable: Hasta 50 hilos concurrentes

⏱️ Timeout Management: 30 segundos por conexión

📊 Logging Detallado: IDs únicos de cliente y métricas de tiempo

🛡️ Manejo Robust de Errores: Captura completa de excepciones

🔧 Soporte HTTP Completo: GET, POST, HEAD, OPTIONS

🌐 Endpoints Disponibles
Servicios con Anotaciones
bash
# Saludo básico
curl "http://localhost:35000/greeting"

# Saludo personalizado  
curl "http://localhost:35000/hello?name=Juan"

# Operación matemática
curl "http://localhost:35000/add?a=5&b=3"
Servicios Legacy
bash
# Constantes matemáticas
curl "http://localhost:35000/pi"
curl "http://localhost:35000/e"

# Registro de usuarios
curl -X POST -H "Content-Type: application/json" \
     -d '{"name":"NuevoUsuario"}' \
     "http://localhost:35000/app/hello"
Pruebas de Concurrencia
bash
# Múltiples peticiones simultáneas
curl "http://localhost:35000/pi" &
curl "http://localhost:35000/e" & 
curl "http://localhost:35000/greeting?name=Usuario1" &
curl "http://localhost:35000/add?a=2&b=3" &
wait
🐳 Docker Hub Deployment
Imagen Disponible Públicamente
La imagen Docker está disponible en Docker Hub bajo:

Repositorio: jorggg/arep-taller4

URL: https://hub.docker.com/r/jorggg/arep-taller4

Uso Directo desde Docker Hub
bash
# Ejecutar directamente desde Docker Hub
docker run -p 35000:35000 jorggg/arep-taller4

# Ejecutar en segundo plano
docker run -d -p 35000:35000 --name my-app jorggg/arep-taller4

# Ver logs del contenedor
docker logs my-app

# Detener el contenedor
docker stop my-app
Configuración para AWS EC2
bash
# En instancia EC2 con Docker instalado
docker pull jorggg/arep-taller4
docker run -d -p 35000:35000 --restart always --name arep-web jorggg/arep-taller4

# Exponer el puerto en el security group de AWS
# Permitir tráfico HTTP en el puerto 35000
🧪 Suite de Pruebas
HttpServerTest
testLoadInitialData: Verifica carga inicial de 3 usuarios

testAddUser: Valida registro de nuevos usuarios

testGetHelloWithParams: Prueba parámetros en endpoints

testStaticFileIndex: Verifica servidor de archivos estáticos

testPathTraversalBlocked: Valida seguridad contra path traversal

SimpleControllerTest
testHelloEndpoint: Prueba endpoint básico /greeting

testRequestParam: Valida parámetros con @RequestParam

testMultipleControllers: Verifica múltiples controladores

testMathInvalidNumbers: Prueba manejo de errores matemáticos

MultithreadedServerTest
testConcurrentGetRequests: Valida 20 clientes concurrentes

🚀 Despliegue Rápido en AWS EC2
Opción 1: Script Automático
bash
# En la instancia EC2
curl -sSL https://raw.githubusercontent.com/JAGBytes/arep-taller3/main/deploy-ec2.sh | bash
Opción 2: Comandos Manuales
bash
# 1. Conectar a EC2
ssh -i "key.pem" ubuntu@ec2-tu-instancia.com

# 2. Instalar Docker
sudo apt update && sudo apt install -y docker.io

# 3. Ejecutar contenedor
sudo docker run -d -p 35000:35000 --name arep-app jorggg/arep-taller4

# 4. Verificar
curl http://localhost:35000/greeting
Opción 3: Con Docker Compose en EC2
bash
# Crear docker-compose.yml en EC2
cat > docker-compose.yml << EOF
version: '3'
services:
  web:
    image: jorggg/arep-taller4
    container_name: web
    ports:
      - "35000:35000"
    restart: unless-stopped
EOF

# Ejecutar
docker-compose up -d
📊 Estructura del Proyecto
text
arep-taller3/
├── src/main/
│   ├── java/edu/escuelaing/arem/ASE/app/
│   │   ├── http/           # Servidor multihilo y handlers
│   │   ├── annotation/     # Sistema de anotaciones
│   │   └── controller/     # Controladores REST
│   └── resources/          # Archivos estáticos
├── src/test/               # Suite completa de pruebas
├── target/                 # Archivos compilados
├── Dockerfile             # Configuración Docker
├── docker-compose.yml     # Orquestación de contenedores
└── pom.xml               # Configuración Maven
📈 Características Técnicas
Java 21: Runtime optimizado

Maven: Gestión de dependencias y build

Reflection: Carga automática de componentes

Concurrencia: Pool de 50 hilos con ClientHandler

HTTP Completo: GET, POST, HEAD, OPTIONS

Docker: Contenerización completa

Docker Hub: Imagen pública disponible

AWS EC2: Despliegue en cloud

Testing: Suite completa con JUnit 5

👨‍💻 Autor
Jorge Andrés Gamboa Sierra
Docker Hub: jorggg/arep-taller4
GitHub: JAGBytes