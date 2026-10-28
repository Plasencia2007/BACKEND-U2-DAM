package pe.upeu.ecorecicla.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    /** Hash BCrypt; la contrasena nunca se guarda en claro. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    protected Usuario() {
    }

    public Usuario(String username, String password, String nombreCompleto, Rol rol) {
        this.username = username;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNombreCompleto() { return nombreCompleto; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
}
