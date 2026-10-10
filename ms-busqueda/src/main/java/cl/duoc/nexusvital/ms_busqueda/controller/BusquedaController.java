package cl.duoc.nexusvital.ms_busqueda.controller;

import cl.duoc.nexusvital.ms_busqueda.model.IndiceBusqueda;
import cl.duoc.nexusvital.ms_busqueda.service.IndiceBusquedaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/busqueda")
public class BusquedaController {

    @Autowired
    private IndiceBusquedaService service;

    @GetMapping
    public List<IndiceBusqueda> obtenerTodos() {
        return service.obtenerTodos();
    }

    @GetMapping("/nombre")
    public List<IndiceBusqueda> buscarPorNombre(@RequestParam String q) {
        return service.buscarPorNombre(q);
    }

    @GetMapping("/rut/{rut}")
    public IndiceBusqueda buscarPorRut(@PathVariable String rut) {
        return service.buscarPorRut(rut);
    }

    @PostMapping
    public IndiceBusqueda crear(@RequestBody IndiceBusqueda indice) {
        return service.guardar(indice);
    }
}