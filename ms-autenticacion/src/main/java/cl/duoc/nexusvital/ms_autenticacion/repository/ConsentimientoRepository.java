package cl.duoc.nexusvital.ms_autenticacion.repository;

import cl.duoc.nexusvital.ms_autenticacion.model.Consentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {
    List<Consentimiento> findByUsuarioId(Long usuarioId);
}