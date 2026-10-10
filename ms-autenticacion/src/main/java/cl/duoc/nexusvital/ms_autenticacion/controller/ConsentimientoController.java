package cl.duoc.nexusvital.ms_autenticacion.controller;

import cl.duoc.nexusvital.ms_autenticacion.model.Consentimiento;
import cl.duoc.nexusvital.ms_autenticacion.service.ConsentimientoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consentimientos")
public class ConsentimientoController {

    @Autowired
    private ConsentimientoService consentimientoService;

    @GetMapping
    public List<Consentimiento> obtenerTodos() {
        return consentimientoService.obtenerTodos();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Consentimiento> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return consentimientoService.obtenerPorUsuario(usuarioId);
    }

    @PostMapping
    public Consentimiento crear(@RequestBody Consentimiento consentimiento) {
        return consentimientoService.guardar(consentimiento);
    }
}