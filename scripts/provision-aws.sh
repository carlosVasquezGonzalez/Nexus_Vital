#!/usr/bin/env bash
# Provisión idempotente de los servicios cloud de NexusVital (IE5).
# Se puede ejecutar varias veces: solo crea o actualiza lo que falta.
# Requiere: aws cli, jq, zip y credenciales activas (AWS Academy: con session token).
set -euo pipefail

REGION="${AWS_DEFAULT_REGION:-us-east-1}"
QUEUE_NAME="${QUEUE_NAME:-nexus-historial-queue}"
DLQ_NAME="${DLQ_NAME:-nexus-historial-dlq}"
LAMBDA_NAME="${LAMBDA_NAME:-registra-historial}"
LAMBDA_HANDLER="${LAMBDA_HANDLER:-lambda_function.handler}"
LAMBDA_RUNTIME="${LAMBDA_RUNTIME:-python3.12}"
LAMBDA_ROLE_NAME="${LAMBDA_ROLE_NAME:-LabRole}"
API_NAME="${API_NAME:-nexus-api}"
API_STAGE="${API_STAGE:-prod}"

log() { echo "==> $*"; }

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# ---------------------------------------------------------------- 1. SQS
log "1/5 Cola de mensajes muertos (DLQ): $DLQ_NAME"
DLQ_URL=$(aws sqs create-queue --queue-name "$DLQ_NAME" --query QueueUrl --output text)
DLQ_ARN=$(aws sqs get-queue-attributes --queue-url "$DLQ_URL" \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

log "2/5 Cola principal: $QUEUE_NAME"
if QUEUE_URL=$(aws sqs get-queue-url --queue-name "$QUEUE_NAME" --query QueueUrl --output text 2>/dev/null); then
  echo "    ya existe"
else
  QUEUE_URL=$(aws sqs create-queue --queue-name "$QUEUE_NAME" \
    --attributes VisibilityTimeout=60 --query QueueUrl --output text)
  echo "    creada"
fi
QUEUE_ARN=$(aws sqs get-queue-attributes --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)

# Redrive hacia la DLQ solo si la cola aún no lo tiene (no pisa configuración existente)
CURRENT_REDRIVE=$(aws sqs get-queue-attributes --queue-url "$QUEUE_URL" \
  --attribute-names RedrivePolicy --query 'Attributes.RedrivePolicy' --output text)
if [ "$CURRENT_REDRIVE" = "None" ]; then
  REDRIVE=$(jq -nc --arg arn "$DLQ_ARN" '{deadLetterTargetArn:$arn,maxReceiveCount:"3"}')
  ATTRS=$(jq -nc --arg r "$REDRIVE" '{RedrivePolicy:$r}')
  aws sqs set-queue-attributes --queue-url "$QUEUE_URL" --attributes "$ATTRS"
  echo "    redrive policy aplicada (maxReceiveCount=3)"
fi

# ---------------------------------------------------------------- 2. Lambda
log "3/5 Función Lambda: $LAMBDA_NAME"
ZIP=/tmp/lambda-package.zip
rm -f "$ZIP"
( cd lambda && zip -qr "$ZIP" . -x "function.zip" "test-event.json" "*.md" )

if aws lambda get-function --function-name "$LAMBDA_NAME" >/dev/null 2>&1; then
  aws lambda update-function-code --function-name "$LAMBDA_NAME" \
    --zip-file "fileb://$ZIP" >/dev/null
  aws lambda wait function-updated --function-name "$LAMBDA_NAME"
  echo "    código actualizado"
else
  ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${LAMBDA_ROLE_NAME}"
  aws lambda create-function --function-name "$LAMBDA_NAME" \
    --runtime "$LAMBDA_RUNTIME" --handler "$LAMBDA_HANDLER" \
    --role "$ROLE_ARN" --timeout 30 --zip-file "fileb://$ZIP" >/dev/null
  aws lambda wait function-active --function-name "$LAMBDA_NAME"
  echo "    función creada"
  echo "::warning::La Lambda se creó sin variables de entorno. Configura las que use lambda_function.py (por ejemplo la URL de ms-historial)."
fi

log "4/5 Trigger SQS -> Lambda"
MAPPING=$(aws lambda list-event-source-mappings --function-name "$LAMBDA_NAME" \
  --event-source-arn "$QUEUE_ARN" --query 'EventSourceMappings[0].UUID' --output text)
if [ "$MAPPING" = "None" ] || [ -z "$MAPPING" ]; then
  aws lambda create-event-source-mapping --function-name "$LAMBDA_NAME" \
    --event-source-arn "$QUEUE_ARN" --batch-size 10 >/dev/null
  echo "    mapping creado"
else
  echo "    mapping existente ($MAPPING)"
fi

# ---------------------------------------------------------------- 3. API Gateway
log "5/5 API Gateway: $API_NAME (stage $API_STAGE)"
API_ID=$(aws apigateway get-rest-apis --query "items[?name=='$API_NAME'].id | [0]" --output text)
if [ "$API_ID" = "None" ] || [ -z "$API_ID" ]; then
  echo "::error::No existe el API Gateway '$API_NAME' en esta cuenta."
  exit 1
fi

# Garantiza que ningún método quede sin API key
for ID in $(aws apigateway get-resources --rest-api-id "$API_ID" \
    --query "items[?resourceMethods!=null].id" --output text); do
  REQ=$(aws apigateway get-method --rest-api-id "$API_ID" --resource-id "$ID" \
    --http-method ANY --query apiKeyRequired --output text 2>/dev/null || echo "n/a")
  if [ "$REQ" = "False" ] || [ "$REQ" = "None" ]; then
    aws apigateway update-method --rest-api-id "$API_ID" --resource-id "$ID" \
      --http-method ANY --patch-operations op=replace,path=/apiKeyRequired,value=true >/dev/null
    echo "    método $ID ahora exige API key"
  fi
done

aws apigateway create-deployment --rest-api-id "$API_ID" --stage-name "$API_STAGE" \
  --description "pipeline ${GITHUB_SHA:-local}" >/dev/null
echo "    deployment publicado en $API_STAGE"

# ---------------------------------------------------------------- Salidas
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "sqs_queue_url=$QUEUE_URL" >> "$GITHUB_OUTPUT"
fi
log "Provisión completa"
