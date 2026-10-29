package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.domain.Permiso;
import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga el usuario desde la base de datos para el inicio de sesión.
 *
 * <p>Autoridades que entrega: "ROLE_" + código del rol (para hasRole) y el código de cada
 * permiso (para hasAuthority). Los roles y permisos eliminados no aportan nada.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        String identificador = email.trim();
        Usuario usuario = identificador.contains("@")
                ? usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse(identificador).orElse(null)
                : usuarioRepository.findByNombreIgnoreCaseAndEliminadoFalse(identificador)
                        .orElseGet(() -> usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse(identificador).orElse(null));
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        return new UsuarioAutenticado(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                usuario.getPassword(), usuario.isActivo(), autoridadesDe(usuario));
    }

    private Set<GrantedAuthority> autoridadesDe(Usuario usuario) {
        Set<GrantedAuthority> autoridades = new LinkedHashSet<>();
        for (Rol rol : usuario.getRoles()) {
            if (rol.isEliminado()) {
                continue;
            }
            autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.getCodigo()));
            for (Permiso permiso : rol.getPermisos()) {
                if (!permiso.isEliminado()) {
                    autoridades.add(new SimpleGrantedAuthority(permiso.getCodigo()));
                }
            }
        }
        return autoridades;
    }
}