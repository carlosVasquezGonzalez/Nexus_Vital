# Pruebas funcionales y de integración - NexusVital

## 1. Objetivo

Verificar el funcionamiento de los microservicios, la infraestructura distribuida, el pipeline CI/CD y la integración de los servicios AWS utilizados en NexusVital.

Todas las pruebas fueron realizadas utilizando datos ficticios en un entorno académico.

## 2. Pruebas locales de Docker

Se verificó la ejecución individual de los cinco microservicios mediante contenedores Docker y su conexión con MySQL.

| Microservicio | Puerto | Estado de salud | Resultado |
|---|---|---|---|
| Usuarios | 8081 | UP | HTTP 200 |
| Historial | 8082 | UP | HTTP 200 |
| Autenticación | 8083 | UP | HTTP 200 |
| Documentos | 8084 | UP | HTTP 200 |
| Búsqueda | 8085 | UP | HTTP 200 |

Se comprobó la conectividad con la base de datos y el funcionamiento de los endpoints.

## 3. Pruebas funcionales

| Microservicio | Operaciones verificadas |
|---|---|
| Usuarios | Creación, consulta, actualización y eliminación |
| Historial | Creación, consulta general, consulta por usuario y eliminación |
| Autenticación | Registro y consulta de consentimientos |
| Documentos | Registro y consulta de documentos |
| Búsqueda | Registro de índices y búsqueda por nombre y RUT |

Las operaciones verificadas respondieron correctamente según los códigos HTTP correspondientes.

## 4. Pruebas de Docker Compose

Se utilizó el archivo `docker-compose.local.yml` para ejecutar conjuntamente los cinco microservicios y MySQL.

```bash
docker compose -p nexusvital-local -f docker-compose.local.yml up -d
docker compose -p nexusvital-local -f docker-compose.local.yml ps
```

Resultados:

- Seis contenedores operativos.
- Cinco microservicios disponibles.
- Conexión correcta con MySQL.
- Consultas funcionales exitosas.
- Persistencia de datos después de detener y reiniciar los contenedores.

## 5. Pruebas de Docker Swarm

Desde el nodo Manager se verificó la disponibilidad del clúster:

```bash
docker node ls
docker stack services nexus
docker stack ps nexus
```

Resultados:

| Componente | Resultado |
|---|---|
| Manager | Ready / Active |
| Worker 1 | Ready / Active |
| Worker 2 | Ready / Active |
| Microservicios Java | 10 de 10 réplicas |
| MySQL | 1 de 1 réplica |
| Total | 11 de 11 réplicas |

## 6. Pruebas de escalabilidad

Se modificó dinámicamente el número de réplicas del microservicio Usuarios.

```bash
docker service scale nexus_ms-usuarios=1
docker service ps nexus_ms-usuarios
```

Posteriormente, se restauraron las dos réplicas:

```bash
docker service scale nexus_ms-usuarios=2
docker service ps nexus_ms-usuarios
```

Resultados:

| Prueba | Resultado |
|---|---|
| Reducción de réplicas | 2 a 1 |
| Aumento de réplicas | 1 a 2 |
| Disponibilidad del endpoint después del escalado | HTTP 200 |
| Distribución de réplicas | Dos nodos Worker |

Se comprobó el escalado manual de servicios sin necesidad de reconstruir las imágenes Docker.

## 7. Pruebas de API Gateway

Se verificó el acceso a las rutas principales mediante API Key.

| Ruta | Sin API Key | Con API Key |
|---|---|---|
| /api/usuarios | HTTP 403 | HTTP 200 |
| /api/historiales | HTTP 403 | HTTP 200 |
| /api/autenticacion | HTTP 403 | HTTP 200 |
| /api/documentos | HTTP 403 | HTTP 200 |
| /api/busqueda | HTTP 403 | HTTP 200 |

Las pruebas confirmaron que API Gateway exige una API Key válida.

También se identificó que los endpoints directos de los microservicios permanecen accesibles públicamente, lo que constituye una limitación de seguridad pendiente de mejora.

## 8. Pruebas de Amazon SQS

Se verificó la existencia y configuración de las siguientes colas:

- nexus-historial-queue.
- nexus-historial-dlq.

Se realizaron pruebas de envío manual de mensajes y posteriormente pruebas de publicación automática desde el microservicio Documentos.

Se comprobó que los mensajes fueron consumidos por Lambda y que, después del procesamiento exitoso, la cola principal no mantenía mensajes pendientes.

## 9. Pruebas de AWS Lambda

Se realizaron las siguientes verificaciones:

- Ejecución manual mediante un evento de prueba.
- Ejecución automática mediante mensajes enviados a SQS.
- Procesamiento de eventos generados por Documentos.
- Registro automático en el microservicio Historial.
- Consulta de registros de ejecución en CloudWatch.

Resultado esperado en CloudWatch:

```text
historial respondio 200
```

## 10. Pruebas del pipeline CI/CD

Se verificó la ejecución de las etapas automatizadas mediante GitHub Actions.

| Etapa | Resultado |
|---|---|
| Pruebas Maven de los cinco microservicios | Success |
| Construcción de imágenes Docker | Success |
| Publicación en Docker Hub | Success |
| Aprovisionamiento AWS | Success |
| Despliegue en Docker Swarm | Success |
| Verificación de API Gateway | Success |

Evidencia de ejecución:

- Workflow: CI/CD NexusVital.
- Ejecución: número 7, segundo intento.
- Commit: 12b6d54.
- Duración: 2 minutos y 59 segundos.
- Trabajos completados: 13.
- Artefactos generados: 10.

## 11. Prueba de integración de extremo a extremo

### Objetivo

Comprobar el flujo completo desde el ingreso de un documento mediante API Gateway hasta la generación automática de un registro en Historial.

### Preparación

Desde el nodo Manager, configurar la URL de API Gateway y obtener la API Key mediante AWS CLI:

```bash
read -rp "URL base de API Gateway: " BASE_URL

API_KEY_ID=$(aws apigateway get-api-keys \
  --name-query nexus-key \
  --region us-east-1 \
  --query "items[?name=='nexus-key'].id | [0]" \
  --output text)

API_KEY=$(aws apigateway get-api-key \
  --api-key "$API_KEY_ID" \
  --include-value \
  --region us-east-1 \
  --query 'value' \
  --output text)
```

No mostrar el valor de la API Key durante las pruebas o grabaciones.

### Paso 1: Consultar el historial inicial

```bash
curl --max-time 15 -sS \
  -H "x-api-key: $API_KEY" \
  -w '\nHTTP %{http_code}\n' \
  "$BASE_URL/api/historiales/usuario/994"
```

Resultado obtenido:

```json
[]
```

Código HTTP: 200.

### Paso 2: Registrar un documento

```bash
curl --max-time 15 -sS \
  -X POST "$BASE_URL/api/documentos" \
  -H "x-api-key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": 994,
    "nombreArchivo": "prueba_final_cicd.pdf",
    "tipoDocumento": "Examen de prueba CI/CD",
    "urlAlmacenamiento": "https://example.com/prueba_final_cicd.pdf"
  }' \
  -w '\nHTTP %{http_code}\n'
```

Resultado obtenido:

```json
{
  "fechaSubida": "2026-11-28",
  "id": 5,
  "nombreArchivo": "prueba_final_cicd.pdf",
  "tipoDocumento": "Examen de prueba CI/CD",
  "urlAlmacenamiento": "https://example.com/prueba_final_cicd.pdf",
  "usuarioId": 994
}
```

Código HTTP: 200.

### Paso 3: Procesamiento asíncrono

El microservicio Documentos publica un evento en Amazon SQS.

AWS Lambda consume el mensaje y realiza una petición HTTP POST al microservicio Historial.

### Paso 4: Consultar el historial actualizado

```bash
curl --max-time 15 -sS \
  -H "x-api-key: $API_KEY" \
  -w '\nHTTP %{http_code}\n' \
  "$BASE_URL/api/historiales/usuario/994"
```

Resultado obtenido:

```json
[
  {
    "diagnostico": "Documento cargado: prueba_final_cicd.pdf",
    "fechaAtencion": "2026-11-28",
    "id": 8,
    "tratamiento": "Tipo: Examen de prueba CI/CD",
    "usuarioId": 994
  }
]
```

Código HTTP: 200.

### Paso 5: Verificar CloudWatch

```bash
aws logs tail /aws/lambda/registra-historial \
  --since 15m \
  --region us-east-1
```

Resultados obtenidos:

| Indicador | Resultado |
|---|---|
| Ejecución de Lambda | Exitosa |
| Respuesta del microservicio Historial | HTTP 200 |
| Duración | 475 ms aproximadamente |
| Memoria asignada | 128 MB |
| Memoria máxima utilizada | 50 MB |

### Resultado final

La prueba E2E fue exitosa y permitió comprobar el siguiente flujo:

Cliente → API Gateway → Documentos → Amazon SQS → AWS Lambda → Historial → MySQL.

El registro del historial fue generado automáticamente, sin necesidad de enviar manualmente un mensaje a SQS ni ejecutar directamente la función Lambda.

Para repetir esta prueba se recomienda utilizar un identificador de usuario ficticio diferente y verificar previamente que no tenga registros asociados.

## 12. Verificación de persistencia

Durante el despliegue se identificó una incompatibilidad entre las credenciales de MySQL y el volumen existente.

Se realizó un respaldo y se recuperó la base de datos mediante un volumen independiente.

Después del despliegue exitoso se verificó que los registros anteriores de Documentos e Historial permanecían disponibles.

## 13. Resumen de resultados

| Prueba | Estado |
|---|---|
| Contenedores Docker individuales | Exitosa |
| Endpoints funcionales | Exitosa |
| Docker Compose | Exitosa |
| Persistencia de datos | Exitosa |
| Docker Swarm | Exitosa |
| Escalabilidad manual | Exitosa |
| Publicación de eventos SQS | Exitosa |
| Consumo de mensajes mediante Lambda | Exitosa |
| Protección de rutas API Gateway | Exitosa |
| Pipeline CI/CD | Exitosa |
| Integración E2E | Exitosa |
| Verificación mediante CloudWatch | Exitosa |

## 14. Pruebas pendientes y mejoras futuras

- Pruebas de carga con diferentes niveles de concurrencia.
- Escalabilidad automática basada en métricas.
- Pruebas automatizadas de lógica de negocio.
- Pruebas E2E incorporadas directamente al pipeline.
- Protección completa de los endpoints directos.
- Validación de idempotencia para evitar registros duplicados.
- Pruebas de recuperación ante fallos de infraestructura.

## 15. Conclusión

Las pruebas realizadas permitieron verificar el funcionamiento de los cinco microservicios, la infraestructura distribuida, la persistencia de datos, el despliegue automatizado y la integración asíncrona mediante servicios AWS.

Los resultados demuestran el funcionamiento de NexusVital dentro del alcance del entorno académico implementado.