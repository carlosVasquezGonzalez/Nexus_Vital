package cl.duoc.nexusvital.ms_busqueda.repository;

import cl.duoc.nexusvital.ms_busqueda.model.IndiceBusqueda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndiceBusquedaRepository extends JpaRepository<IndiceBusqueda, Long> {
    List<IndiceBusqueda> findByNombreCompletoContainingIgnoreCase(String nombre);
    IndiceBusqueda findByRut(String rut);
}