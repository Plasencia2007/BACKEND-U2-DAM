package pe.upeu.ecorecicla.seguridad;

import java.util.Set;

/**
 * Identidad reconstruida a partir del token firmado. Es el "principal" que el filtro
 * coloca en el contexto de seguridad para cada peticion.
 */
public record UsuarioAutenticado(Long id, String username, String nombre, String rol, Set<String> privilegios) {

    public boolean tiene(String privilegio) {
        return privilegios.contains(privilegio);
    }
}
