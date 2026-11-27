package cl.duoc.nexusvital.ms_documentos.messaging;

import cl.duoc.nexusvital.ms_documentos.model.Documento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DocumentoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentoEventPublisher.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final String queueUrl;
    private final SqsClient sqs;

    public DocumentoEventPublisher(@Value("${sqs.queue-url:}") String queueUrl,
                                   @Value("${aws.region:us-east-1}") String region) {
        this.queueUrl = queueUrl;
        this.sqs = queueUrl.isBlank() ? null : SqsClient.builder().region(Region.of(region)).build();
    }

    public void publicar(Documento d) {
        if (sqs == null) {
            log.warn("SQS_QUEUE_URL no configurada: no se publica evento");
            return;
        }
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("evento", "DOCUMENTO_CREADO");
            msg.put("documentoId", d.getId());
            msg.put("usuarioId", d.getUsuarioId());
            msg.put("nombreArchivo", d.getNombreArchivo());
            msg.put("tipoDocumento", d.getTipoDocumento());
            msg.put("fechaSubida", d.getFechaSubida() != null ? d.getFechaSubida().toString() : null);
            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(mapper.writeValueAsString(msg))
                    .build());
            log.info("Evento DOCUMENTO_CREADO publicado para documento {}", d.getId());
        } catch (Exception e) {
            log.error("No se pudo publicar en SQS", e);
        }
    }
}
