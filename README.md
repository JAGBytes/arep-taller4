# Framework Web en Java para Servicios REST con Anotaciones, Reflexión y Multihilo

Este proyecto implementa un framework web completo en Java que evoluciona desde un servidor web básico hacia una plataforma robusta para el desarrollo de aplicaciones web con servicios REST backend. El framework incluye sistema de anotaciones, carga automática de componentes mediante reflexión, arquitectura MVC moderna, y procesamiento multihilo concurrente avanzado.

## Video de despliegue de la aplicación

```
https://pruebacorreoescuelaingeduco-my.sharepoint.com/:v:/g/personal/jorge_gamboa-s_mail_escuelaing_edu_co/EWjJ6ntz6YdAn6acxX-ZZ50B5sY_l2IjybeMqecTDbaBsA?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0NvcHkifX0&e=EZDOio
```

## Características Principales

- **Sistema de Anotaciones Personalizado**  
  Implementa anotaciones como `@RestController`, `@GetMapping` y `@RequestParam` para simplificar la creación de controladores.

- **Carga Automática de Componentes**  
  Descubrimiento automático de controladores mediante **reflexión**, evitando configuraciones manuales.

- **Arquitectura Multihilo Avanzada**  
  Uso de un **pool de hasta 50 hilos** con un `ClientHandler` dedicado para cada conexión.

- **Protocolo HTTP Completo**  
  Soporte para los métodos **GET, POST, HEAD y OPTIONS**.

- **Sistema de Logging Mejorado**  
  Registro detallado de **eventos, hilos y clientes**, útil para depuración y monitoreo.

- **Manejo Robusto de Errores**  
   Incluye **timeouts de 30 segundos** y captura centralizada de excepciones para mayor estabilidad.

## Instalación y Ejecución

### Prerrequisitos

- **Java 21** → [Descargar](https://jdk.java.net/21/)
- **Apache Maven 3.8+** → [Instalar](https://maven.apache.org/install.html)
- **Docker** → [Descargar](https://www.docker.com/get-started/)

### Ejecución Local

```bash
# Clonar el repositorio
git clone https://github.com/JAGBytes/arep-taller4.git
cd arep-taller4

# Compilar el proyecto
mvn clean compile

# Ejecutar el servidor
java -cp target/classes edu.escuelaing.arem.ASE.app.App

# Acceder a la aplicación
http://localhost:35000

```

### Descargar y ejecutar la imagen desde Docker Hub

```bash

# Descargar la imagen
docker pull jorggg/arep-taller4

# Ejecutar el contenedor en el puerto 35000
docker run -p 35000:35000 jorggg/arep-taller4

# Acceder a la aplicación
http://localhost:35000

```

## Ejecutar Pruebas

```bash

# Todas las pruebas

mvn test

# Pruebas específicas

mvn test -Dtest=HttpServerTest
mvn test -Dtest=SimpleControllerTest
mvn test -Dtest=MultithreadedServerTest
```

## Despliegue en AWS EC2

Antes de continuar con la instalación en la VM creamos los dos siguientes archivos

El archivo docker-compose.yml levanta el servicio web con la imagen arep-taller4, expone el puerto 35000 y define la variable de entorno PORT=35000 para ejecutar la aplicación.

```bash
version: '3'

services:
  web:
    image: arep-taller4
    container_name: web
    ports:
      - "35000:35000"
    environment:
      - PORT=35000

```

El Dockerfile construye la imagen partiendo de eclipse-temurin:21-jre, copia el JAR compilado, expone el puerto 35000 y ejecuta la aplicación con java -jar app.jar.

```bash
# Usamos JDK 21
FROM eclipse-temurin:21-jre

# Directorio de trabajo
WORKDIR /usrapp/bin

# Exponer el puerto del servidor
ENV PORT 35000
EXPOSE 35000

# Copiar el JAR compilado
COPY target/arep-taller4-1.0-SNAPSHOT.jar ./app.jar

# Ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]
```

En la máquina virtual de AWS ejecutamos los siguientes comandos.

```bash
# 1. Actualizar paquetes
sudo yum update -y

# 2. Instalar Docker
sudo yum install docker -y

# 3. Iniciar el servicio de Docker
sudo service docker start

# 4. Agregar el usuario ec2-user al grupo docker (para no usar sudo en cada comando)
sudo usermod -a -G docker ec2-user

# 5. Desconectarse y volver a conectarse a la instancia EC2

# 6. Descargar y ejecutar el contenedor en segundo plano
docker pull jorggg/arep-taller4
docker run -d -p 35000:35000 --name arep-server jorggg/arep-taller4
```

## Arquitectura Multihilo Avanzada

ClientHandler Dedicado
Cada cliente es manejado por una instancia especializada de ClientHandler que implementa Runnable:

```java
// Pool de 50 hilos para manejo concurrente
private static ExecutorService threadPool = Executors.newFixedThreadPool(50);

// Cada cliente en hilo separado
threadPool.submit(new ClientHandler(clientSocket));
Características del Sistema Multihilo
```

- Pool Configurable: Hasta 50 hilos concurrentes

- Timeout Management: 30 segundos por conexión

- Logging Detallado: IDs únicos de cliente y métricas de tiempo

- Manejo Robust de Errores: Captura completa de excepciones

- Soporte HTTP Completo: GET, POST, HEAD, OPTIONS

## Endpoints Disponibles

Servicios con Anotaciones

```bash
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

```

Las pruebas están organizadas en tres clases principales, cada una validando distintos aspectos del servidor, a continuación se muestran algunas de estas:

---

### HttpServerTest

Pruebas de funcionalidades básicas del servidor HTTP, manejo de usuarios y archivos estáticos.

- **testLoadInitialData** → Verifica que se carguen **3 usuarios iniciales** correctamente.
- **testAddUser** → Valida el registro de **nuevos usuarios** en memoria.
- **testStaticFileIndex** → Confirma que se sirvan archivos estáticos (`index.html`).

---

### SimpleControllerTest

Pruebas de controladores cargados con anotaciones y reflexión.

- **testRequestParam** → Valida el uso de parámetros con `@RequestParam` en `/hello`.
- **testMultipleControllers** → Prueba la ejecución de múltiples controladores (ej: `MathController`).
- **testLoadComponentsWorks** → Comprueba que `loadComponents()` registre correctamente todos los controladores.

---

### MultithreadedServerTest

Pruebas de concurrencia y rendimiento del servidor.

- **testConcurrentGetRequests** → Valida que el servidor maneje correctamente **20 clientes concurrentes** sin perder respuestas.

### Estructura del Proyecto

```text
arep-taller3/
├── src/main/
│ ├── java/edu/escuelaing/arem/ASE/app/
│ │ ├── http/ # Servidor multihilo y handlers
│ │ ├── annotation/ # Sistema de anotaciones
│ │ └── controller/ # Controladores REST
│ └── resources/ # Archivos estáticos
├── src/test/ # Suite completa de pruebas
├── target/ # Archivos compilados
├── Dockerfile # Configuración Docker
├── docker-compose.yml # Orquestación de contenedores
└── pom.xml # Configuración Maven

```

## Autor

- Jorge Andrés Gamboa Sierra
- Docker Hub: jorggg/arep-taller4
