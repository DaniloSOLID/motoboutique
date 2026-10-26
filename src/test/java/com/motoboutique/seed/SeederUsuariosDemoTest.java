package com.motoboutique.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.service.UsuarioService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeederUsuariosDemoTest {

    @Mock
    private UsuarioService usuarioService;

    @Test
    void creaUnUsuarioDemoPorCadaRol() {
        new SeederUsuariosDemo(usuarioService, "Demo-Clave-1").ejecutar();

        ArgumentCaptor<UsuarioCrearDto> captor = ArgumentCaptor.forClass(UsuarioCrearDto.class);
        verify(usuarioService, times(3)).crear(captor.capture());
        List<UsuarioCrearDto> creados = captor.getAllValues();
        assertEquals("almacenero@motoboutique.local", creados.get(0).getEmail());
        assertEquals(Set.of("ALMACENERO"), creados.get(0).getRoles());
        assertEquals(Set.of("CONTADOR"), creados.get(1).getRoles());
        assertEquals(Set.of("CONSULTA"), creados.get(2).getRoles());
        assertEquals("Demo-Clave-1", creados.get(2).getPassword());
    }

    @Test
    void sinContrasenaNoHaceNada() {
        new SeederUsuariosDemo(usuarioService, "").ejecutar();

        verifyNoInteractions(usuarioService);
    }

    @Test
    void noDuplicaLosUsuariosQueYaExisten() {
        when(usuarioService.existeEmail(anyString()))
                .thenAnswer(invocacion -> "consulta@motoboutique.local".equals(invocacion.getArgument(0)));

        new SeederUsuariosDemo(usuarioService, "Demo-Clave-1").ejecutar();

        verify(usuarioService, times(2)).crear(any());
    }
}
