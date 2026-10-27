package com.motoboutique.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.motoboutique.seguridad.domain.Permiso;
import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioDetailsService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioDetailsService(usuarioRepository);
    }

    private static Permiso permiso(String codigo) {
        Permiso permiso = new Permiso();
        permiso.setCodigo(codigo);
        return permiso;
    }

    private static Rol rol(String codigo, Permiso... permisos) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.getPermisos().addAll(List.of(permisos));
        return rol;
    }

    private static Usuario usuario(boolean activo, Rol... roles) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana Pérez");
        usuario.setEmail("ana@tienda.pe");
        usuario.setPassword("hash-bcrypt");
        usuario.setActivo(activo);
        usuario.getRoles().addAll(List.of(roles));
        return usuario;
    }

    private static Set<String> codigos(UserDetails detalles) {
        return detalles.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void entregaLosRolesYLosPermisosComoAutoridades() {
        Usuario usuario = usuario(true,
                rol("ALMACENERO", permiso("PRODUCTO_VER"), permiso("STOCK_EDITAR")));
        when(usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse("ana@tienda.pe"))
                .thenReturn(Optional.of(usuario));

        UserDetails detalles = servicio.loadUserByUsername("ana@tienda.pe");

        assertEquals(Set.of("ROLE_ALMACENERO", "PRODUCTO_VER", "STOCK_EDITAR"), codigos(detalles));
        assertEquals("ana@tienda.pe", detalles.getUsername());
        assertEquals("hash-bcrypt", detalles.getPassword());
        assertTrue(detalles.isEnabled());
        UsuarioAutenticado autenticado = assertInstanceOf(UsuarioAutenticado.class, detalles);
        assertEquals("Ana Pérez", autenticado.getNombre());
    }

    @Test
    void unUsuarioInexistenteLanzaUsernameNotFound() {
        when(usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse("nadie@tienda.pe"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> servicio.loadUserByUsername("nadie@tienda.pe"));
    }

    @Test
    void unUsuarioDesactivadoNoEstaHabilitado() {
        when(usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse("ana@tienda.pe"))
                .thenReturn(Optional.of(usuario(false, rol("CONSULTA"))));

        UserDetails detalles = servicio.loadUserByUsername("ana@tienda.pe");

        assertFalse(detalles.isEnabled());
    }

    @Test
    void unRolEliminadoNoAportaAutoridades() {
        Rol eliminado = rol("ADMIN", permiso("TODO"));
        eliminado.marcarEliminado();
        when(usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse("ana@tienda.pe"))
                .thenReturn(Optional.of(usuario(true, eliminado)));

        UserDetails detalles = servicio.loadUserByUsername("ana@tienda.pe");

        assertTrue(detalles.getAuthorities().isEmpty());
    }

    @Test
    void ignoraLosEspaciosAlrededorDelCorreo() {
        when(usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse("Ana@Tienda.pe"))
                .thenReturn(Optional.of(usuario(true, rol("CONSULTA"))));

        servicio.loadUserByUsername("  Ana@Tienda.pe ");

        verify(usuarioRepository).findByEmailIgnoreCaseAndEliminadoFalse("Ana@Tienda.pe");
    }

    @Test
    void unCorreoEnBlancoNoConsultaLaBaseDeDatos() {
        assertThrows(UsernameNotFoundException.class, () -> servicio.loadUserByUsername("   "));

        verifyNoInteractions(usuarioRepository);
    }
}
