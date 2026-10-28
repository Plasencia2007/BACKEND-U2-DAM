package pe.upeu.ecorecicla.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Empresa donde se recogen residuos (padron de puntos de acopio). */
@Entity
@Table(name = "punto_acopio")
public class PuntoAcopio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String razonSocial;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false, length = 40)
    private String zona;

    @Column(nullable = false, length = 40)
    private String materialHabitual;

    protected PuntoAcopio() {
    }

    public PuntoAcopio(String razonSocial, String direccion, String zona, String materialHabitual) {
        this.razonSocial = razonSocial;
        this.direccion = direccion;
        this.zona = zona;
        this.materialHabitual = materialHabitual;
    }

    public Long getId() { return id; }
    public String getRazonSocial() { return razonSocial; }
    public String getDireccion() { return direccion; }
    public String getZona() { return zona; }
    public String getMaterialHabitual() { return materialHabitual; }
}
