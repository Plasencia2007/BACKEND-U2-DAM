package pe.upeu.ecorecicla.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.ecorecicla.dominio.Pesaje;
import pe.upeu.ecorecicla.dominio.PuntoAcopio;
import pe.upeu.ecorecicla.dominio.Ruta;
import pe.upeu.ecorecicla.repositorio.PesajeRepository;
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository;
import pe.upeu.ecorecicla.repositorio.RutaRepository;
import pe.upeu.ecorecicla.seguridad.Privilegio;
import pe.upeu.ecorecicla.seguridad.PrivilegioRepository;
import pe.upeu.ecorecicla.seguridad.Rol;
import pe.upeu.ecorecicla.seguridad.RolRepository;
import pe.upeu.ecorecicla.seguridad.Usuario;
import pe.upeu.ecorecicla.seguridad.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_ANULAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_CREAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_EDITAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_EDITAR_TODOS;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_LEER;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PUNTO_GESTIONAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PUNTO_LEER;
import static pe.upeu.ecorecicla.seguridad.Privilegio.RUTA_CERRAR;

/**
 * Datos semilla del anexo (seccion 10). Se cargan al iniciar el servicio:
 * privilegios, roles, 3 usuarios, 12 puntos en 3 zonas, 2 rutas y 15 pesajes.
 * Es idempotente: si los datos ya existen no los duplica, y si cambia el dia crea la ruta del dia.
 */
@Component
public class DatosSemilla implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosSemilla.class);
    private static final String CLAVE_DEMO = "Eco2026*";

    /** Matriz de privilegios por rol (seccion 3.3). Vive en la base de datos, no en los controladores. */
    static final Map<String, Set<String>> MATRIZ = new LinkedHashMap<>();

    static {
        MATRIZ.put("OPERARIO", Set.of(PUNTO_LEER, PESAJE_LEER, PESAJE_CREAR, PESAJE_EDITAR));
        MATRIZ.put("SUPERVISOR", Set.of(PUNTO_LEER, PESAJE_LEER, PESAJE_CREAR, PESAJE_EDITAR, PESAJE_EDITAR_TODOS,
                PESAJE_ANULAR, RUTA_CERRAR));
        MATRIZ.put("ADMINISTRADOR", Set.of(PUNTO_LEER, PESAJE_LEER, PUNTO_GESTIONAR));
    }

    private static final Map<String, String> DESCRIPCIONES = Map.of(
            PUNTO_LEER, "Consultar la ruta y los puntos de acopio",
            PESAJE_LEER, "Consultar los pesajes de un punto",
            PESAJE_CREAR, "Registrar un pesaje nuevo",
            PESAJE_EDITAR, "Corregir pesajes propios del mismo dia",
            PESAJE_EDITAR_TODOS, "Corregir pesajes de cualquier operario y fecha",
            PESAJE_ANULAR, "Anular un pesaje",
            RUTA_CERRAR, "Cerrar la ruta del dia",
            PUNTO_GESTIONAR, "Crear puntos de acopio y asignar rutas");

    private final PrivilegioRepository privilegios;
    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PuntoAcopioRepository puntos;
    private final RutaRepository rutas;
    private final PesajeRepository pesajes;
    private final PasswordEncoder passwordEncoder;

    public DatosSemilla(PrivilegioRepository privilegios, RolRepository roles, UsuarioRepository usuarios,
                        PuntoAcopioRepository puntos, RutaRepository rutas, PesajeRepository pesajes,
                        PasswordEncoder passwordEncoder) {
        this.privilegios = privilegios;
        this.roles = roles;
        this.usuarios = usuarios;
        this.puntos = puntos;
        this.rutas = rutas;
        this.pesajes = pesajes;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarSeguridad();
        List<PuntoAcopio> padron = sembrarPuntos();
        sembrarRutas(padron);
        log.info("Datos semilla listos: {} usuarios, {} puntos, {} rutas, {} pesajes",
                usuarios.count(), puntos.count(), rutas.count(), pesajes.count());
    }

    private void sembrarSeguridad() {
        DESCRIPCIONES.forEach((codigo, descripcion) -> {
            if (privilegios.findByCodigo(codigo).isEmpty()) {
                privilegios.save(new Privilegio(codigo, descripcion));
            }
        });
        MATRIZ.forEach((nombre, codigos) -> {
            if (roles.findByNombre(nombre).isEmpty()) {
                Set<Privilegio> conjunto = codigos.stream()
                        .map(c -> privilegios.findByCodigo(c).orElseThrow())
                        .collect(Collectors.toSet());
                roles.save(new Rol(nombre, conjunto));
            }
        });
        crearUsuario("operario1", "Carlos Quispe Mamani", "OPERARIO");
        crearUsuario("supervisor1", "Lucia Huaman Rojas", "SUPERVISOR");
        crearUsuario("admin", "Administrador EcoRecicla", "ADMINISTRADOR");
    }

    private void crearUsuario(String username, String nombre, String rol) {
        if (usuarios.findByUsername(username).isEmpty()) {
            usuarios.save(new Usuario(username, passwordEncoder.encode(CLAVE_DEMO), nombre,
                    roles.findByNombre(rol).orElseThrow()));
        }
    }

    private List<PuntoAcopio> sembrarPuntos() {
        if (puntos.count() == 0) {
            puntos.saveAll(List.of(
                    new PuntoAcopio("Supermercados del Norte SAC", "Av. Tupac Amaru 1520", "Norte", "Carton"),
                    new PuntoAcopio("Textiles Andinos EIRL", "Jr. Los Telares 340", "Norte", "Plastico"),
                    new PuntoAcopio("Distribuidora La Esperanza SA", "Av. Universitaria 8800", "Norte", "Carton"),
                    new PuntoAcopio("Imprenta Grafica Norte SAC", "Calle Las Prensas 112", "Norte", "Papel"),
                    new PuntoAcopio("Hotel Plaza Centro SA", "Jr. de la Union 455", "Centro", "Vidrio"),
                    new PuntoAcopio("Restaurante El Buen Sabor SAC", "Av. Abancay 210", "Centro", "Vidrio"),
                    new PuntoAcopio("Banco Andino - Sede Central", "Av. Nicolas de Pierola 900", "Centro", "Papel"),
                    new PuntoAcopio("Galeria Comercial Mesa Redonda", "Jr. Andahuaylas 650", "Centro", "Plastico"),
                    new PuntoAcopio("Clinica San Gabriel SA", "Av. Benavides 3120", "Sur", "Plastico"),
                    new PuntoAcopio("Colegio Santa Rosa", "Calle Los Pinos 77", "Sur", "Papel"),
                    new PuntoAcopio("Ferreteria El Constructor EIRL", "Av. Los Heroes 1450", "Sur", "Metal"),
                    new PuntoAcopio("Embotelladora del Sur SAC", "Panamericana Sur km 18", "Sur", "Vidrio")));
        }
        return puntos.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
    }

    private void sembrarRutas(List<PuntoAcopio> p) {
        Usuario operario = usuarios.findByUsername("operario1").orElseThrow();
        Usuario supervisor = usuarios.findByUsername("supervisor1").orElseThrow();
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);

        // Ruta del dia anterior, ya cerrada (permite demostrar RN-01): 5 puntos y 8 pesajes.
        if (rutas.count() == 0) {
            Ruta cerrada = new Ruta(ayer, operario, supervisor);
            for (int i = 0; i < 5; i++) {
                cerrada.agregarPunto(p.get(6 + i), i + 1);
            }
            rutas.save(cerrada);
            double[][] pesos = {{120.5, 8.0}, {95.0, 6.5}, {60.0, 4.0}, {210.0, 12.0},
                    {80.0, 5.5}, {150.0, 9.0}, {45.5, 3.0}, {132.0, 7.5}};
            int[] puntoDeCada = {6, 6, 7, 8, 8, 9, 10, 10};
            for (int i = 0; i < pesos.length; i++) {
                PuntoAcopio punto = p.get(puntoDeCada[i]);
                pesajes.save(new Pesaje(cerrada, punto, ayer.atTime(8 + i, 15), punto.getMaterialHabitual(),
                        pesos[i][0], pesos[i][1], null, operario, null));
            }
            cerrada.cerrar();
        }

        // Ruta del dia, abierta, con 6 puntos: 3 con pesajes y 3 sin pesar (permite demostrar RN-04).
        if (rutas.buscarAsignadas(hoy, operario.getId()).isEmpty()) {
            Ruta abierta = new Ruta(hoy, operario, supervisor);
            for (int i = 0; i < 6; i++) {
                abierta.agregarPunto(p.get(i), i + 1);
            }
            rutas.save(abierta);
            pesajes.save(new Pesaje(abierta, p.get(0), hoy.atTime(7, 30), "Carton", 140.0, 10.0,
                    "Fardos compactados", operario, null));
            pesajes.save(new Pesaje(abierta, p.get(0), hoy.atTime(7, 45), "Carton", 55.0, 5.0, null, operario, null));
            Pesaje anulado = new Pesaje(abierta, p.get(0), hoy.atTime(7, 50), "Plastico", 30.0, 2.0,
                    "Registrado por error", operario, null);
            anulado.anular("Pesaje duplicado");                    // al menos uno anulado
            pesajes.save(anulado);
            pesajes.save(new Pesaje(abierta, p.get(1), hoy.atTime(8, 20), "Plastico", 75.0, 4.5, null, operario, null));
            // Pesaje de otro usuario (supervisor1): operario1 no puede corregirlo (RN-02).
            pesajes.save(new Pesaje(abierta, p.get(1), hoy.atTime(8, 40), "Plastico", 42.0, 3.0,
                    "Registrado por la supervisora", supervisor, null));
            pesajes.save(new Pesaje(abierta, p.get(2), hoy.atTime(9, 10), "Carton", 98.0, 6.0, null, operario, null));
            pesajes.save(new Pesaje(abierta, p.get(2), hoy.atTime(9, 25), "Papel", 33.5, 2.5, null, operario, null));
        }
    }
}
