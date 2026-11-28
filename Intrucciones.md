# NexusVital - Instrucciones de instalación y ejecución local

## 1. Requisitos previos

- Git.
- Docker Desktop.
- Docker Compose.
- Java JDK 21 y Maven, si se desea compilar directamente el código fuente.

## 2. Clonar el repositorio

```bash
git clone https://github.com/carlosVasquezGonzalez/Nexus_Vital.git
cd Nexus_Vital
```

## 3. Configurar las variables de entorno

Antes de iniciar los contenedores, configurar las credenciales de MySQL.

Desde PowerShell:

```powershell
$env:MYSQL_ROOT_PASSWORD = [System.Net.NetworkCredential]::new("", (Read-Host "Contraseña root MySQL" -AsSecureString)).Password

$env:MYSQL_PASSWORD = [System.Net.NetworkCredential]::new("", (Read-Host "Contraseña usuario MySQL" -AsSecureString)).Password
```

Las contraseñas no deben almacenarse directamente en el repositorio. Si existe un volumen previamente inicializado, se deben utilizar las credenciales correspondientes a esa base de datos.

## 4. Construcción de imágenes Docker

Desde la raíz del proyecto:

```bash
docker build -t carlos0801/ms-usuarios:latest ./ms-usuarios
docker build -t carlos0801/ms-historial:latest ./ms-historial
docker build -t carlos0801/ms-autenticacion:latest ./ms-autenticacion
docker build -t carlos0801/ms-documentos:latest ./ms-documentos
docker build -t carlos0801/ms-busqueda:latest ./ms-busqueda
```

Verificar las imágenes generadas:

```bash
docker images
```

## 5. Iniciar los contenedores

El archivo `docker-compose.local.yml` permite iniciar los cinco microservicios y una instancia de MySQL 8.4 en un entorno local.

```bash
docker compose -p nexusvital-local -f docker-compose.local.yml up -d
```

Verificar el estado de los contenedores:

```bash
docker compose -p nexusvital-local -f docker-compose.local.yml ps
```

## 6. Acceso a los microservicios

| Microservicio | Puerto local | Endpoint |
|---|---|---|
| Usuarios | 9081 | http://localhost:9081/api/usuarios |
| Historial | 9082 | http://localhost:9082/api/historiales |
| Autenticación | 9083 | http://localhost:9083/api/consentimientos |
| Documentos | 9084 | http://localhost:9084/api/documentos |
| Búsqueda | 9085 | http://localhost:9085/api/busqueda |

Los endpoints pueden consultarse mediante un navegador, Postman o curl.

## 7. Verificación del estado de salud

Cada microservicio cuenta con un endpoint de Spring Boot Actuator.

Desde PowerShell:

```powershell
9081..9085 | ForEach-Object {
    Write-Host "Puerto $_"
    curl.exe --max-time 10 "http://localhost:$_/actuator/health"
    Write-Host ""
}
```

El resultado esperado es un estado `UP` para cada microservicio.

## 8. Base de datos

El entorno local utiliza MySQL 8.4 con las siguientes características:

- Base de datos: `nexusvital_db`.
- Usuario de aplicación: `nexususer`.
- Red Docker: `nexus-local`.
- Almacenamiento persistente mediante volumen Docker.

Los datos se conservan al detener y volver a iniciar los contenedores, siempre que no se elimine el volumen.

## 9. Detener los servicios

```bash
docker compose -p nexusvital-local -f docker-compose.local.yml down
```

No utilizar la opción `-v` si se desea conservar la información almacenada en MySQL.

## 10. Despliegue en AWS

Para el despliegue distribuido en la nube se utiliza el archivo `docker-compose.yml`, compatible con Docker Swarm.

La configuración de Amazon SQS, AWS Lambda y API Gateway corresponde al entorno cloud. Las pruebas locales no requieren publicar mensajes en SQS.

Consultar la documentación complementaria:

- [Configuración de Docker Swarm](docs/SWARM.md)
- [Pipeline CI/CD](docs/CI-CD.md)
- [Servicios AWS](docs/AWS.md)
- [Pruebas funcionales](docs/PRUEBAS.md)