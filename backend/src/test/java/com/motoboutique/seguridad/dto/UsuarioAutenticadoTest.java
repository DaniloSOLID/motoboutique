package com.motoboutique.seguridad.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UsuarioAutenticadoTest {

    private static UsuarioAutenticado usuario(String email) {
        return new UsuarioAutenticado(7L, "Ana Pérez", email, "hash-bcrypt", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void eraseCredentialsBorraLaContrasena() {
        UsuarioAutenticado usuario = usuario("ana@tienda.pe");
        assertEquals("hash-bcrypt", usuario.getPassword());

        usuario.eraseCredentials();

        assertNull(usuario.getPassword());
    }

    @Test
    void dosUsuariosConElMismoCorreoSonIgualesSinImportarLasMayusculas() {
        UsuarioAutenticado primero = usuario("ana@tienda.pe");
        UsuarioAutenticado segundo = usuario("ANA@Tienda.PE");

        assertEquals(primero, segundo);
        assertEquals(primero.hashCode(), segundo.hashCode());
    }

    @Test
    void exponeIdNombreYElCorreoComoNombreDeUsuario() {
        UsuarioAutenticado usuario = usuario("ana@tienda.pe");

        assertEquals(7L, usuario.getId());
        assertEquals("Ana Pérez", usuario.getNombre());
        assertEquals("ana@tienda.pe", usuario.getUsername());
        assertTrue(usuario.isEnabled());
    }
}
