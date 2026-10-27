package com.motoboutique.seguridad.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UsuarioSesionDtoTest {

    @Test
    void separaLosRolesSinPrefijoDeLosPermisosYLosOrdena() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(7L, "Ana Pérez", "ana@tienda.pe", "hash", true,
                List.of(new SimpleGrantedAuthority("USUARIO_VER"),
                        new SimpleGrantedAuthority("ROLE_CONTADOR"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROL_VER")));

        UsuarioSesionDto dto = UsuarioSesionDto.de(usuario);

        assertEquals(7L, dto.id());
        assertEquals("Ana Pérez", dto.nombre());
        assertEquals("ana@tienda.pe", dto.email());
        assertEquals(List.of("ADMIN", "CONTADOR"), dto.roles());
        assertEquals(List.of("ROL_VER", "USUARIO_VER"), dto.permisos());
    }
}
