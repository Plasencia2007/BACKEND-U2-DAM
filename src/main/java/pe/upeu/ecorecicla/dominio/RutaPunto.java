package pe.upeu.ecorecicla.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Punto planificado dentro de una ruta, con su orden de visita y el motivo si se omitio. */
@Entity
@Table(name = "ruta_punto", uniqueConstraints = @UniqueConstraint(columnNames = {"ruta_id", "punto_id"}))
public class RutaPunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "punto_id")
    private PuntoAcopio punto;

    @Column(nullable = false)
    private int secuencia;

    @Column(length = 250)
    private String motivoOmision;

    protected RutaPunto() {
    }

    RutaPunto(Ruta ruta, PuntoAcopio punto, int secuencia) {
        this.ruta = ruta;
        this.punto = punto;
        this.secuencia = secuencia;
    }

    public void omitir(String motivo) {
        this.motivoOmision = motivo;
    }

    public boolean estaOmitido() {
        return motivoOmision != null && !motivoOmision.isBlank();
    }

    public Long getId() { return id; }
    public Ruta getRuta() { return ruta; }
    public PuntoAcopio getPunto() { return punto; }
    public int getSecuencia() { return secuencia; }
    public String getMotivoOmision() { return motivoOmision; }
}
