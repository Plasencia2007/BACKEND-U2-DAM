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
import pe.upeu.ecorecicla.seguridad.Usuario;

import java.time.LocalDateTime;

/**
 * Pesaje de lo recogido en un punto. La anulacion no borra la fila: la marca.
 * El peso neto siempre lo calcula el servicio (RN-03).
 */
@Entity
@Table(name = "pesaje")
public class Pesaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_id")
    private PuntoAcopio punto;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 40)
    private String material;

    @Column(nullable = false)
    private double pesoBruto;

    @Column(nullable = false)
    private double tara;

    @Column(nullable = false)
    private double pesoNeto;

    @Column(length = 250)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id")
    private Usuario operario;

    @Column(nullable = false)
    private boolean anulado;

    @Column(length = 250)
    private String motivoAnulacion;

    /** SC-C: humedad estimada del material en porcentaje (0 a 100). Opcional. */
    private Double humedadEstimada;

    /** Clave unica generada por la aplicacion para que reenviar un pendiente no duplique el pesaje. */
    @Column(unique = true, length = 64)
    private String claveCliente;

    protected Pesaje() {
    }

    public Pesaje(Ruta ruta, PuntoAcopio punto, LocalDateTime fechaHora, String material, double pesoBruto,
                  double tara, String observacion, Usuario operario, String claveCliente) {
        this.ruta = ruta;
        this.punto = punto;
        this.fechaHora = fechaHora;
        this.operario = operario;
        this.claveCliente = claveCliente;
        corregir(material, pesoBruto, tara, observacion);
    }

    /** Regla RN-03: el neto es bruto menos tara. La validacion de que sea positivo la hace el servicio. */
    public static double calcularNeto(double pesoBruto, double tara) {
        return Math.round((pesoBruto - tara) * 1000.0) / 1000.0;
    }

    public void corregir(String material, double pesoBruto, double tara, String observacion) {
        this.material = material;
        this.pesoBruto = pesoBruto;
        this.tara = tara;
        this.pesoNeto = calcularNeto(pesoBruto, tara);
        this.observacion = observacion;
    }

    public void estimarHumedad(Double porcentaje) {
        this.humedadEstimada = porcentaje;
    }

    public void anular(String motivo) {
        this.anulado = true;
        this.motivoAnulacion = motivo;
    }

    public Long getId() { return id; }
    public Ruta getRuta() { return ruta; }
    public PuntoAcopio getPunto() { return punto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getMaterial() { return material; }
    public double getPesoBruto() { return pesoBruto; }
    public double getTara() { return tara; }
    public double getPesoNeto() { return pesoNeto; }
    public String getObservacion() { return observacion; }
    public Usuario getOperario() { return operario; }
    public boolean isAnulado() { return anulado; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public String getClaveCliente() { return claveCliente; }
    public Double getHumedadEstimada() { return humedadEstimada; }
}
