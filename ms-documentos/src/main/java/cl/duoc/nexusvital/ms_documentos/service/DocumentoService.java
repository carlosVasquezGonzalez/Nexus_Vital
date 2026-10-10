package cl.duoc.nexusvital.ms_documentos.service;

import cl.duoc.nexusvital.ms_documentos.model.Documento;
import cl.duoc.nexusvital.ms_documentos.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    public List<Documento> obtenerTodos() {
        return documentoRepository.findAll();
    }

    public List<Documento> obtenerPorUsuario(Long usuarioId) {
        return documentoRepository.findByUsuarioId(usuarioId);
    }

    public Documento guardar(Documento documento) {
        return documentoRepository.save(documento);
    }
}