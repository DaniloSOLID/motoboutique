package com.motoboutique.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.motoboutique.seguridad.domain.Permiso;
import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.dto.RolDto;
import com.motoboutique.seguridad.dto.RolMapperImpl;
import com.motoboutique.seguridad.repository.PermisoRepository;
import com.motoboutique.seguridad.repository.RolRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RolServiceImplTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    private RolServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new RolServiceImpl(rolRepository, permisoRepository, new RolMapperImpl());
    }

    private static Permiso permiso(String codigo) {
        Permiso permiso = new Permiso();
        permiso.setCodigo(codigo);
        permiso.setDescripcion(codigo);
        permiso.setModulo("catalogo");
        return permiso;
    }

    @Test
    void asegurarCreaElRolConSusPermisosOrdenados() {
        when(permisoRepository.findByCodigoIn(anyCollection()))
                .thenReturn(List.of(permiso("STOCK_VER"), permiso("PRODUCTO_VER")));
        when(rolRepository.findByCodigo("CONSULTA")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RolDto resultado = servicio.asegurar("CONSULTA", "Consulta", "Solo lectura", true,
                List.of("PRODUCTO_VER", "STOCK_VER"));

        assertEquals("CONSULTA", resultado.getCodigo());
        assertTrue(resultado.isSistema());
        assertEquals(List.of("PRODUCTO_VER", "STOCK_VER"), resultado.getPermisos());
    }

    @Test
    void asegurarAgregaPermisosNuevosSinQuitarLosExistentesNiCambiarElNombre() {
        Rol existente = new Rol();
        existente.setCodigo("ALMACENERO");
        existente.setNombre("Nombre editado por el administrador");
        existente.getPermisos().add(permiso("PRODUCTO_VER"));
        when(permisoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(permiso("STOCK_EDITAR")));
        when(rolRepository.findByCodigo("ALMACENERO")).thenReturn(Optional.of(existente));
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RolDto resultado = servicio.asegurar("ALMACENERO", "Almacenero", "Descripción", true,
                Set.of("STOCK_EDITAR"));

        assertEquals(List.of("PRODUCTO_VER", "STOCK_EDITAR"), resultado.getPermisos());
        assertEquals("Nombre editado por el administrador", resultado.getNombre());
    }

    @Test
    void asegurarRechazaPermisosQueNoExisten() {
        when(permisoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of());

        NegocioException error = assertThrows(NegocioException.class,
                () -> servicio.asegurar("CONSULTA", "Consulta", null, true, Set.of("NO_EXISTE")));

        assertTrue(error.getMessage().contains("NO_EXISTE"));
        verify(rolRepository, never()).save(any());
    }
}
