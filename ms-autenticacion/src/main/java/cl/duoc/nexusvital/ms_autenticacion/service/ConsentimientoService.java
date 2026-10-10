package cl.duoc.nexusvital.ms_autenticacion.service;

import cl.duoc.nexusvital.ms_autenticacion.model.Consentimiento;
import cl.duoc.nexusvital.ms_autenticacion.repository.ConsentimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsentimientoService {

    @Autowired
    private ConsentimientoRepository consentimientoRepository;

    public List<Consentimiento> obtenerTodos() {
        return consentimientoRepository.findAll();
    }

    public List<Consentimiento> obtenerPorUsuario(Long usuarioId) {
        return consentimientoRepository.findByUsuarioId(usuarioId);
    }

    public Consentimiento guardar(Consentimiento consentimiento) {
        return consentimientoRepository.save(consentimiento);
    }
}