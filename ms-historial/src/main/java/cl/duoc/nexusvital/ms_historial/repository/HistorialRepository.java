package cl.duoc.nexusvital.ms_historial.repository;

import cl.duoc.nexusvital.ms_historial.model.HistorialMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialRepository extends JpaRepository<HistorialMedico, Long> {
    List<HistorialMedico> findByUsuarioId(Long usuarioId);
}
