# Infraestructura AWS - NexusVital

## 1. Descripción general

NexusVital utiliza servicios de Amazon Web Services para implementar una arquitectura distribuida, escalable y con comunicación asíncrona entre microservicios.

La infraestructura se encuentra desplegada en la región us-east-1 (Norte de Virginia), mediante AWS Academy Learner Lab.

## 2. Servicios utilizados

| Servicio AWS | Responsabilidad |
|---|---|
| Amazon EC2 | Alojamiento de los nodos Docker Swarm |
| Amazon SQS | Comunicación asíncrona mediante mensajes |
| AWS Lambda | Procesamiento automático de eventos |
| Amazon API Gateway | Punto de entrada a los microservicios |
| Amazon CloudWatch | Registro y monitoreo de ejecuciones |
| AWS IAM | Administración de permisos y roles |
| Security Groups | Control del tráfico de red |

## 3. Amazon EC2

Se utilizan tres instancias EC2 de tipo t3.small con Amazon Linux 2023.

| Instancia | Rol |
|---|---|
| nexus-manager | Administración del clúster y alojamiento de MySQL |
| nexus-worker-1 | Ejecución de microservicios |
| nexus-worker-2 | Ejecución de microservicios |

Los nodos se comunican mediante sus direcciones IP privadas y una red overlay de Docker Swarm.

Cada microservicio Java dispone de dos réplicas distribuidas entre los workers.

La configuración detallada se encuentra en [SWARM.md](SWARM.md).

## 4. Amazon SQS

Amazon Simple Queue Service permite desacoplar el microservicio Documentos del procesamiento posterior del historial clínico.

Se utilizan dos colas:

| Cola | Finalidad |
|---|---|
| nexus-historial-queue | Recepción de eventos generados por Documentos |
| nexus-historial-dlq | Almacenamiento de mensajes que no pudieron procesarse correctamente |

### Configuración de la cola principal

- Tipo: Standard.
- Tiempo de visibilidad: 60 segundos.
- Retención de mensajes: 4 días.
- Cifrado: SSE-SQS.
- Máximo de recepciones antes de enviar a DLQ: 3.

### Productor de mensajes

El microservicio ms-documentos publica un evento cuando se registra un documento médico.

La clase DocumentoEventPublisher utiliza AWS SDK para enviar mensajes a SQS.

Ejemplo de evento:

```json
{
  "evento": "DOCUMENTO_CREADO",
  "documentoId": 5,
  "usuarioId": 994,
  "nombreArchivo": "prueba_final_cicd.pdf",
  "tipoDocumento": "Examen de prueba CI/CD",
  "fechaSubida": "2026-11-28"
}
```

### Variables de configuración

| Variable | Finalidad |
|---|---|
| SQS_QUEUE_URL | Dirección de la cola principal |
| AWS_REGION | Región de AWS |

## 5. AWS Lambda

Se implementó una función serverless denominada `registra-historial`.

| Configuración | Valor |
|---|---|
| Runtime | Python 3.12 |
| Arquitectura | x86_64 |
| Handler | lambda_function.handler |
| Rol de ejecución | LabRole |
| Disparador | Amazon SQS |
| Memoria | 128 MB |

La función consume los mensajes de la cola principal, interpreta su contenido y realiza una petición HTTP POST al microservicio Historial.

Como resultado, se genera automáticamente un registro asociado al documento ingresado.

### Variable de entorno

`HISTORIAL_URL`

Esta variable contiene la dirección del endpoint del microservicio Historial:

```text
http://IP_PUBLICA_MANAGER:8082/api/historiales
```

La dirección debe actualizarse si cambia la IP pública del Manager.

El código fuente se encuentra en:

`lambda/lambda_function.py`

## 6. Amazon API Gateway

Se utiliza una API REST denominada `nexus-api`, desplegada en la etapa `prod`.

API Gateway proporciona un punto de entrada centralizado para los cinco microservicios.

### Rutas configuradas

| Ruta pública | Endpoint del backend | Puerto |
|---|---|---|
| /api/usuarios | /api/usuarios | 8081 |
| /api/historiales | /api/historiales | 8082 |
| /api/autenticacion | /api/consentimientos | 8083 |
| /api/documentos | /api/documentos | 8084 |
| /api/busqueda | /api/busqueda | 8085 |

Cada recurso dispone de una ruta principal y una ruta proxy para acceder a sus recursos secundarios.

Se utiliza una integración HTTP_PROXY para redirigir las peticiones hacia los microservicios desplegados en Docker Swarm.

### Protección mediante API Key

La API utiliza:

- API Key: nexus-key.
- Usage Plan: nexus-plan.
- Stage: prod.

Las peticiones realizadas a través de API Gateway deben incluir el encabezado `x-api-key`.

Durante las pruebas se verificaron los siguientes resultados:

| Petición | Resultado |
|---|---|
| Sin API Key | HTTP 403 |
| Con API Key válida | HTTP 200 |

La API Key controla el acceso a las rutas del Gateway, pero no constituye un mecanismo de autenticación individual de pacientes o profesionales.

## 7. Amazon CloudWatch

CloudWatch permite consultar los registros generados durante las ejecuciones de Lambda.

Grupo de logs:

```text
/aws/lambda/registra-historial
```

Para consultar las ejecuciones recientes mediante AWS CLI:

```bash
aws logs tail /aws/lambda/registra-historial \
  --since 15m \
  --region us-east-1
```

Durante la prueba de integración posterior al despliegue CI/CD se verificó:

- Ejecución automática de Lambda.
- Respuesta HTTP 200 del microservicio Historial.
- Duración aproximada de 475 milisegundos.
- Consumo máximo de memoria de 50 MB.

## 8. Integración de los servicios

El flujo de comunicación implementado es el siguiente:

1. El cliente envía una petición POST a API Gateway.
2. API Gateway verifica la API Key.
3. La petición se redirige al microservicio Documentos.
4. Documentos almacena el registro en MySQL.
5. Documentos publica un evento en Amazon SQS.
6. AWS Lambda consume el mensaje.
7. Lambda realiza una petición POST al microservicio Historial.
8. Historial almacena el nuevo registro en MySQL.
9. El cliente consulta el historial actualizado mediante API Gateway.

Este mecanismo permite realizar el procesamiento del historial de manera asíncrona.

## 9. Aprovisionamiento automatizado

El archivo `scripts/provision-aws.sh` permite automatizar parte de la configuración de los servicios cloud.

Sus principales responsabilidades son:

- Crear o reutilizar las colas SQS.
- Configurar la Dead Letter Queue.
- Crear o actualizar AWS Lambda.
- Configurar el disparador SQS.
- Actualizar las variables de entorno.
- Verificar los requisitos de API Key.
- Desplegar la etapa prod de API Gateway.

El script se ejecuta automáticamente como parte del pipeline GitHub Actions.

Las instancias EC2, la configuración inicial de red y la creación original de API Gateway requieren aprovisionamiento previo.

## 10. Verificación de servicios AWS

### Verificar las credenciales activas

```bash
aws sts get-caller-identity
```

### Listar las colas SQS

```bash
aws sqs list-queues --region us-east-1
```

### Verificar el disparador de Lambda

```bash
aws lambda list-event-source-mappings \
  --function-name registra-historial \
  --region us-east-1 \
  --query 'EventSourceMappings[].{UUID:UUID,Estado:State}' \
  --output table
```

El estado esperado del disparador es `Enabled`.

### Listar las API REST

```bash
aws apigateway get-rest-apis \
  --region us-east-1 \
  --query 'items[].{Nombre:name,ID:id}' \
  --output table
```

## 11. Consideraciones de AWS Academy

AWS Academy utiliza credenciales temporales y recursos asociados a la duración del laboratorio.

Después de reiniciar el laboratorio, se recomienda verificar:

- Estado de las instancias EC2.
- Dirección pública del Manager.
- Vigencia de las credenciales AWS.
- Estado del clúster Docker Swarm.
- Integraciones de API Gateway.
- Variables de entorno de Lambda.
- Estado del disparador SQS.
- Configuración de GitHub Secrets.

## 12. Limitaciones y mejoras futuras

La implementación corresponde a un entorno académico y utiliza datos ficticios.

Se identifican las siguientes oportunidades de mejora:

- Restringir el acceso público directo a los microservicios.
- Implementar comunicaciones privadas entre los componentes.
- Incorporar autenticación y autorización mediante tokens.
- Utilizar permisos IAM de mínimo privilegio.
- Automatizar el aprovisionamiento completo de la infraestructura.
- Implementar mecanismos de idempotencia para evitar registros duplicados.
- Incorporar alarmas de CloudWatch para detectar errores.
- Mejorar la gestión de secretos y credenciales.
- Implementar mecanismos de alta disponibilidad para MySQL.

## 13. Conclusión

La integración de Amazon EC2, Amazon SQS, AWS Lambda, API Gateway y Cloud