package pe.upeu.ecorecicla.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import pe.upeu.ecorecicla.seguridad.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ruta de recojo de un dia: un operario la recorre y un supervisor la cierra. */
@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id")
    private Usuario operario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    @Column(nullable = false)
    private boolean cerrada;

    private LocalDateTime fechaCierre;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("secuencia ASC")
    private List<RutaPunto> puntos = new ArrayList<>();

    protected Ruta() {
    }

    public Ruta(LocalDate fecha, Usuario operario, Usuario supervisor) {
        this.fecha = fecha;
        this.operario = operario;
        this.supervisor = supervisor;
    }

    public RutaPunto agregarPunto(PuntoAcopio punto, int secuencia) {
        RutaPunto rp = new RutaPunto(this, punto, secuencia);
        puntos.add(rp);
        return rp;
    }

    public boolean incluye(Long puntoId) {
        return puntos.stream().anyMatch(rp -> rp.getPunto().getId().equals(puntoId));
    }

    public boolean estaAsignadaA(Long usuarioId) {
        return operario.getId().equals(usuarioId)
                || (supervisor != null && supervisor.getId().equals(usuarioId));
    }

    public void cerrar() {
        this.cerrada = true;
        this.fechaCierre = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public Usuario getOperario() { return operario; }
    public Usuario getSupervisor() { return supervisor; }
    public boolean isCerrada() { return cerrada; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public List<RutaPunto> getPuntos() { return puntos; }
}
