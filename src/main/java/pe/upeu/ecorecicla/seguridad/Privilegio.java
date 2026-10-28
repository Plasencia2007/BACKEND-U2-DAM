package pe.upeu.ecorecicla.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Permiso atomico del sistema. La autorizacion siempre se verifica contra el codigo
 * del privilegio, nunca contra el nombre del rol.
 */
@Entity
@Table(name = "privilegio")
public class Privilegio {

    // Codigos del contrato de autorizacion (seccion 3.3 del enunciado).
    public static final String PUNTO_LEER = "PUNTO_LEER";
    public static final String PESAJE_LEER = "PESAJE_LEER";
    public static final String PESAJE_CREAR = "PESAJE_CREAR";
    public static final String PESAJE_EDITAR = "PESAJE_EDITAR";
    /** Amplia PESAJE_EDITAR: permite corregir pesajes de cualquier operario y de cualquier dia. */
    public static final String PESAJE_EDITAR_TODOS = "PESAJE_EDITAR_TODOS";
    public static final String PESAJE_ANULAR = "PESAJE_ANULAR";
    public static final String RUTA_CERRAR = "RUTA_CERRAR";
    public static final String PUNTO_GESTIONAR = "PUNTO_GESTIONAR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(length = 120)
    private String descripcion;

    protected Privilegio() {
    }

    public Privilegio(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}
