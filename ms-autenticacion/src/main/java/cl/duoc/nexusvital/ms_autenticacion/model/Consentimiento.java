package cl.duoc.nexusvital.ms_autenticacion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consentimientos")
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    private String tipoConsentimiento;
    private Boolean otorgado;
    private LocalDateTime fechaRegistro;

    public Consentimiento() {
        this.fechaRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTipoConsentimiento() { return tipoConsentimiento; }
    public void setTipoConsentimiento(String tipoConsentimiento) { this.tipoConsentimiento = tipoConsentimiento; }

    public Boolean getOtorgado() { return otorgado; }
    public void setOtorgado(Boolean otorgado) { this.otorgado = otorgado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}