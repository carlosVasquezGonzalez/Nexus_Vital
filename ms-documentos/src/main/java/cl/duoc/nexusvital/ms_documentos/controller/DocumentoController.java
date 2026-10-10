package cl.duoc.nexusvital.ms_documentos.controller;

import cl.duoc.nexusvital.ms_documentos.model.Documento;
import cl.duoc.nexusvital.ms_documentos.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @GetMapping
    public List<Documento> obtenerTodos() {
        return documentoService.obtenerTodos();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Documento> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return documentoService.obtenerPorUsuario(usuarioId);
    }

    @PostMapping
    public Documento crear(@RequestBody Documento documento) {
        return documentoService.guardar(documento);
    }
}