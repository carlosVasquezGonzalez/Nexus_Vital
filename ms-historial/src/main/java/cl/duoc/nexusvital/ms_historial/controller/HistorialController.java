package cl.duoc.nexusvital.ms_historial.controller;

import cl.duoc.nexusvital.ms_historial.model.HistorialMedico;
import cl.duoc.nexusvital.ms_historial.service.HistorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historiales")
public class HistorialController {

    @Autowired
    private HistorialService historialService;

    @GetMapping
    public List<HistorialMedico> obtenerTodos() {
        return historialService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistorialMedico> obtenerPorId(@PathVariable Long id) {
        return historialService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<HistorialMedico> obtenerPorUsuarioId(@PathVariable Long usuarioId) {
        return historialService.obtenerPorUsuarioId(usuarioId);
    }

    @PostMapping
    public HistorialMedico crear(@RequestBody HistorialMedico historial) {
        return historialService.guardar(historial);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        historialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}