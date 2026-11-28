# AWS Lambda - NexusVital

## 1. Objetivo

Implementar una función serverless que permita generar automáticamente registros en el historial clínico a partir de los eventos publicados por el microservicio Documentos en Amazon SQS.

La función permite desacoplar ambos microservicios mediante comunicación asíncrona.

## 2. Archivos

| Archivo | Descripción |
|---|---|
| lambda_function.py | Código fuente de la función Lambda |
| test-event.json | Evento de prueba para ejecución manual |
| function.zip | Paquete generado para el despliegue |
| README.md | Documentación de configuración, despliegue y pruebas |

El archivo function.zip se genera durante el proceso de aprovisionamiento y no necesita almacenarse en GitHub.

## 3. Configuración de la función

| Parámetro | Valor |
|---|---|
| Nombre | registra-historial |
| Runtime | Python 3.12 |
| Arquitectura | x86_64 |
| Handler | lambda_function.handler |
| Región | us-east-1 |
| Rol de ejecución | LabRole |
| Memoria | 128 MB |
| Disparador | Amazon SQS |
| Cola principal | nexus-historial-queue |
| Dead Letter Queue | nexus-historial-dlq |

## 4. Variables de entorno

La función utiliza la siguiente variable:

`HISTORIAL_URL`

Esta variable contiene la dirección del endpoint del microservicio Historial.

```text
http://IP_PUBLICA_MANAGER:8082/api/historiales
```

Se debe reemplazar IP_PUBLICA_MANAGER por la dirección pública vigente del nodo Manager.

Si AWS Academy modifica la dirección IP de la instancia, será necesario actualizar esta configuración.

## 5. Funcionamiento del código

La función utiliza las bibliotecas estándar de Python:

- json: procesamiento de mensajes JSON.
- os: lectura de variables de entorno.
- urllib.request: ejecución de peticiones HTTP.

El procesamiento se realiza mediante los siguientes pasos:

1. Recibir el evento generado por Amazon SQS.
2. Recorrer los mensajes contenidos en Records.
3. Interpretar el contenido JSON de cada mensaje.
4. Obtener los datos del documento y del usuario.
5. Construir el registro correspondiente al historial clínico.
6. Enviar una petición HTTP POST al microservicio Historial.
7. Registrar el código de respuesta en CloudWatch.
8. Retornar la cantidad de mensajes procesados.

## 6. Estructura del evento recibido

Ejemplo de mensaje publicado en Amazon SQS:

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

Amazon SQS entrega este contenido a Lambda dentro del atributo body de cada registro.

## 7. Transformación de datos

La función transforma los datos del documento en una solicitud compatible con el microservicio Historial.

| Campo de Historial | Origen |
|---|---|
| usuarioId | usuarioId del mensaje |
| diagnostico | Texto Documento cargado y nombreArchivo |
| tratamiento | Texto Tipo y tipoDocumento |
| fechaAtencion | fechaSubida |

Ejemplo de solicitud enviada al microservicio Historial:

```json
{
  "usuarioId": 994,
  "diagnostico": "Documento cargado: prueba_final_cicd.pdf",
  "tratamiento": "Tipo: Examen de prueba CI/CD",
  "fechaAtencion": "2026-11-28"
}
```

## 8. Despliegue automatizado

La función se crea o actualiza mediante el script:

`scripts/provision-aws.sh`

El script realiza las siguientes operaciones:

- Generación del paquete ZIP.
- Creación o actualización de la función Lambda.
- Configuración del rol de ejecución.
- Actualización de la variable HISTORIAL_URL.
- Configuración del disparador asociado a SQS.
- Habilitación del disparador cuando se encuentra desactivado.

El script se ejecuta automáticamente como parte del pipeline CI/CD de GitHub Actions.

Para ejecutarlo manualmente desde un entorno Bash, se requieren AWS CLI, jq, zip, credenciales AWS vigentes y los recursos previos correspondientes.

Desde la raíz del repositorio:

```bash
export AWS_REGION=us-east-1

read -rp "URL del microservicio Historial: " HISTORIAL_URL
export HISTORIAL_URL

bash scripts/provision-aws.sh
```

## 9. Verificar la configuración

Consultar el estado de la función:

```bash
aws lambda get-function-configuration \
  --function-name registra-historial \
  --region us-east-1 \
  --query '{Nombre:FunctionName,Runtime:Runtime,Handler:Handler,Estado:State}' \
  --output table
```

Consultar el estado del disparador:

```bash
aws lambda list-event-source-mappings \
  --function-name registra-historial \
  --region us-east-1 \
  --query 'EventSourceMappings[].{UUID:UUID,Estado:State}' \
  --output table
```

El estado esperado del disparador es Enabled.

## 10. Prueba manual

La función puede probarse desde la consola de AWS Lambda utilizando un evento con la estructura de Amazon SQS.

Ejemplo:

```json
{
  "Records": [
    {
      "body": "{\"usuarioId\":999,\"nombreArchivo\":\"prueba_lambda.pdf\",\"tipoDocumento\":\"Examen de prueba\",\"fechaSubida\":\"2026-11-28\"}"
    }
  ]
}
```

El resultado esperado es:

```json
{
  "procesados": 1
}
```

La ejecución manual genera un registro real de prueba en la base de datos del entorno académico.

## 11. Prueba de integración automática

Se verificó la ejecución de Lambda mediante el registro de un documento desde API Gateway.

El flujo comprobado fue:

API Gateway → Documentos → Amazon SQS → AWS Lambda → Historial → MySQL.

Durante la prueba posterior al despliegue CI/CD se utilizó el documento `prueba_final_cicd.pdf`, asociado al usuario ficticio 994.

El registro generado automáticamente en Historial fue:

```json
{
  "diagnostico": "Documento cargado: prueba_final_cicd.pdf",
  "fechaAtencion": "2026-11-28",
  "id": 8,
  "tratamiento": "Tipo: Examen de prueba CI/CD",
  "usuarioId": 994
}
```

La prueba fue exitosa y no requirió ejecutar manualmente Lambda ni publicar directamente un mensaje en SQS.

## 12. Monitoreo mediante CloudWatch

Los registros de ejecución se encuentran en el grupo:

`/aws/lambda/registra-historial`

Para consultar las ejecuciones recientes:

```bash
aws logs tail /aws/lambda/registra-historial \
  --since 15m \
  --region us-east-1
```

Durante la prueba final se obtuvieron los siguientes resultados:

| Indicador | Resultado |
|---|---|
| Ejecución | Exitosa |
| Respuesta de Historial | HTTP 200 |
| Duración | Aproximadamente 475 ms |
| Duración facturada | 614 ms |
| Memoria asignada | 128 MB |
| Memoria máxima utilizada | 50 MB |

El mensaje registrado por la función fue:

```text
historial respondio 200
```

## 13. Manejo de errores

Cuando una ejecución falla, Amazon SQS permite que el mensaje vuelva a estar disponible para un nuevo intento de procesamiento.

La cola principal dispone de una política de redirección hacia la Dead Letter Queue después de tres recepciones sin procesamiento exitoso.

Esto permite conservar los mensajes que no pudieron procesarse para su posterior revisión.

## 14. Limitaciones y mejoras futuras

- Implementar idempotencia para evitar registros duplicados.
- Validar la estructura y el tipo de evento recibido.
- Incorporar un manejo de errores más detallado.
- Implementar respuestas parciales para lotes de mensajes.
- Utilizar comunicaciones privadas entre Lambda y los microservicios.
- Configurar alarmas de CloudWatch.
- Aplicar permisos IAM de mínimo privilegio.

La implementación actual corresponde a un entorno académico y utiliza datos ficticios.

## 15. Conclusión

La función registra-historial permite implementar procesamiento asíncrono mediante una arquitectura serverless, integrando Amazon SQS con los microservicios de NexusVital.

Su funcionamiento fue comprobado mediante ejecuciones manuales, procesamiento automático de mensajes, consultas funcionales y pruebas de integración de extremo a extremo.

## Documentación relacionada

- [Infraestructura AWS](../docs/AWS.md)
- [Pipeline CI/CD](../docs/CI-CD.md)
- [Pruebas funcionales y de integración](../docs/PRUEBAS.md)