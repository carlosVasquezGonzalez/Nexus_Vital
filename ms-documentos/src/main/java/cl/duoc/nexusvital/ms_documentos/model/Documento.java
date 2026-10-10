package cl.duoc.nexusvital.ms_documentos.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "documentos_examenes")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    private String nombreArchivo;
    private String tipoDocumento; // Ej: Examen de sangre, Radiografia, Receta
    private String urlAlmacenamiento;
    private LocalDate fechaSubida;

    public Documento() {
        this.fechaSubida = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getUrlAlmacenamiento() { return urlAlmacenamiento; }
    public void setUrlAlmacenamiento(String urlAlmacenamiento) { this.urlAlmacenamiento = urlAlmacenamiento; }

    public LocalDate getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDate fechaSubida) { this.fechaSubida = fechaSubida; }
}