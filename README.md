# NexusVital - Plataforma de Microservicios Médicos

Este proyecto contiene la arquitectura base de microservicios desarrollada con **Spring Boot 3**, **Java 17/21** y **MySQL**.

## Microservicios y Puertos
- **ms-usuarios** (Puerto 8081): /api/usuarios`n- **ms-historial** (Puerto 8082): /api/historial`n- **ms-autenticacion** (Puerto 8083): /api/consentimientos`n- **ms-documentos** (Puerto 8084): /api/documentos`n- **ms-busqueda** (Puerto 8085): /api/busqueda`n
## Requisitos Previos
- Java JDK 17 o 21
- MySQL Server
- Maven

## Instrucciones de Ejecución
1. Clonar el repositorio: \git clone https://github.com/carlosVasquezGonzalez/Nexus_Vital.git\`n2. Crear la BD \
exusvital_db\ en MySQL.
3. Entrar a cada carpeta de microservicio, compilar con \./mvnw clean package -DskipTests\ y ejecutar el archivo JAR de la carpeta target con \java -jar target/<nombre-microservicio>.jar\.