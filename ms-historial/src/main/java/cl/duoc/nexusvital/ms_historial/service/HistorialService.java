package cl.duoc.nexusvital.ms_historial.service;

import cl.duoc.nexusvital.ms_historial.model.HistorialMedico;
import cl.duoc.nexusvital.ms_historial.repository.HistorialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HistorialService {

    @Autowired
    private HistorialRepository historialRepository;

    public List<HistorialMedico> obtenerTodos() {
        return historialRepository.findAll();
    }

    public Optional<HistorialMedico> obtenerPorId(Long id) {
        return historialRepository.findById(id);
    }

    public List<HistorialMedico> obtenerPorUsuarioId(Long usuarioId) {
        return historialRepository.findByUsuarioId(usuarioId);
    }

    public HistorialMedico guardar(HistorialMedico historial) {
        return historialRepository.save(historial);
    }

    public void eliminar(Long id) {
        historialRepository.deleteById(id);
    }
}