package cl.duoc.nexusvital.ms_busqueda.model;

import jakarta.persistence.*;

@Entity
@Table(name = "indice_busqueda")
public class IndiceBusqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    private String rut;
    private String nombreCompleto;
    private String prevision;

    public IndiceBusqueda() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getPrevision() { return prevision; }
    public void setPrevision(String prevision) { this.prevision = prevision; }
}