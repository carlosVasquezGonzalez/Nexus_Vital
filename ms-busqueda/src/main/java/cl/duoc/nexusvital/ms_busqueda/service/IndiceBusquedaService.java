package cl.duoc.nexusvital.ms_busqueda.service;

import cl.duoc.nexusvital.ms_busqueda.model.IndiceBusqueda;
import cl.duoc.nexusvital.ms_busqueda.repository.IndiceBusquedaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndiceBusquedaService {

    @Autowired
    private IndiceBusquedaRepository repository;

    public List<IndiceBusqueda> obtenerTodos() {
        return repository.findAll();
    }

    public List<IndiceBusqueda> buscarPorNombre(String nombre) {
        return repository.findByNombreCompletoContainingIgnoreCase(nombre);
    }

    public IndiceBusqueda buscarPorRut(String rut) {
        return repository.findByRut(rut);
    }

    public IndiceBusqueda guardar(IndiceBusqueda indice) {
        return repository.save(indice);
    }
}