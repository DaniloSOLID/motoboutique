package com.motoboutique.seguridad.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapperImpl();

    private static Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        return rol;
    }

    @Test
    void mapeaLosDatosYOrdenaLosCodigosDeRol() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("ana@tienda.pe");
        usuario.setPassword("$2a$04$hash-secreto");
        usuario.setTelefono("999111222");
        usuario.setActivo(true);
        usuario.getRoles().add(rol("CONTADOR"));
        usuario.getRoles().add(rol("ALMACENERO"));

        UsuarioDto dto = mapper.toDto(usuario);

        assertEquals("Ana", dto.getNombre());
        assertEquals("ana@tienda.pe", dto.getEmail());
        assertEquals("999111222", dto.getTelefono());
        assertTrue(dto.isActivo());
        assertEquals(List.of("ALMACENERO", "CONTADOR"), dto.getRoles());
    }

    @Test
    void unUsuarioSinRolesGeneraUnaListaVacia() {
        UsuarioDto dto = mapper.toDto(new Usuario());

        assertTrue(dto.getRoles().isEmpty());
    }

    @Test
    void elDtoNoExponeLaContrasena() {
        assertThrows(NoSuchMethodException.class, () -> UsuarioDto.class.getMethod("getPassword"));
    }
}
