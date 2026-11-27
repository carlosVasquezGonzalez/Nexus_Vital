import json
import os
import urllib.request


def handler(event, context):
    url = os.environ["HISTORIAL_URL"]
    for record in event["Records"]:
        msg = json.loads(record["body"])
        payload = {
            "usuarioId": msg["usuarioId"],
            "diagnostico": "Documento cargado: " + str(msg.get("nombreArchivo")),
            "tratamiento": "Tipo: " + str(msg.get("tipoDocumento")),
            "fechaAtencion": msg.get("fechaSubida"),
        }
        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            print("historial respondio", resp.status)
    return {"procesados": len(event["Records"])}
