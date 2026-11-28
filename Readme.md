# NexusVital

Plataforma de gestión de documentos médicos entre hospitales, desarrollada mediante una arquitectura distribuida de microservicios y desplegada en AWS.

## Sobre el proyecto

NexusVital tiene como objetivo facilitar la gestión de documentos médicos, exámenes e historiales clínicos, permitiendo centralizar la información y facilitar su consulta.

El sistema está compuesto por cinco microservicios con responsabilidades independientes, desplegados mediante Docker Swarm e integrados con servicios de AWS.

## Microservicios

| Microservicio | Puerto | Endpoint principal |
|---|---|---|
| ms-usuarios | 8081 | /api/usuarios |
| ms-historial | 8082 | /api/historiales |
| ms-autenticacion | 8083 | /api/consentimientos |
| ms-documentos | 8084 | /api/documentos |
| ms-busqueda | 8085 | /api/busqueda |

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring Data JPA
- Maven
- MySQL 8.4
- Docker
- Docker Compose
- Docker Swarm
- Docker Hub
- GitHub Actions
- Amazon EC2
- Amazon SQS
- AWS Lambda con Python 3.12
- Amazon API Gateway
- Amazon CloudWatch

## Arquitectura del sistema

La infraestructura cloud está compuesta por tres instancias EC2:

- Un nodo Manager encargado de administrar el clúster y alojar MySQL.
- Dos nodos Worker encargados de ejecutar los microservicios.
- Dos réplicas por cada microservicio Java.
- Una réplica de MySQL con almacenamiento persistente.
- Una red overlay para la comunicación entre los servicios.

Los microservicios utilizan actualmente una base de datos compartida denominada `nexusvital_db`.

### Diagrama de arquitectura original

<img width="1127" height="622" alt="Diagrama de arquitectura NexusVital" src="https://github.com/user-attachments/assets/456efb20-a264-48b5-87a4-409d2fde38a4" />

## Integración asíncrona

Cuando se registra un documento médico, el microservicio Documentos publica un evento en Amazon SQS.

AWS Lambda consume el mensaje y realiza una petición al microservicio Historial, generando automáticamente un registro asociado al documento.

El flujo implementado es:

Cliente → API Gateway → Documentos → Amazon SQS → AWS Lambda → Historial → MySQL.

La respuesta final puede consultarse mediante API Gateway.

## Integración y despliegue continuo (CI/CD)

El proyecto cuenta con un pipeline implementado mediante GitHub Actions, compuesto por las siguientes etapas:

1. Ejecución de pruebas automatizadas.
2. Construcción de imágenes Docker.
3. Publicación de imágenes en Docker Hub.
4. Aprovisionamiento y configuración de servicios AWS.
5. Despliegue de microservicios en Docker Swarm.
6. Verificación de funcionamiento mediante pruebas de acceso.

Se utilizan etiquetas de imágenes asociadas al commit para facilitar la trazabilidad de los despliegues.

## Seguridad

API Gateway controla el acceso a sus rutas mediante API Key.

Las credenciales utilizadas por el pipeline se administran mediante GitHub Secrets, evitando incluirlas directamente en el código fuente.

Actualmente, los endpoints directos de los microservicios permanecen accesibles para las pruebas académicas. Como mejora futura, se considera restringir el acceso directo al backend.

## Documentación

- [Instalación y ejecución local](Intrucciones.md)
- [Configuración de Docker Swarm](docs/SWARM.md)
- [Pipeline CI/CD](docs/CI-CD.md)
- [Configuración de servicios AWS](docs/AWS.md)
- [Pruebas funcionales e integración E2E](docs/PRUEBAS.md)
- [Función serverless AWS Lambda](lambda/README.md)

## Estado del proyecto

El sistema fue desplegado y validado mediante pruebas locales, pruebas de escalabilidad, ejecución del pipeline CI/CD y pruebas de integración de extremo a extremo.

Proyecto académico desarrollado para la asignatura Java: Diseño y Construcción de Soluciones Nativas en Nube.
