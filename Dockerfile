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