package com.motoboutique.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.dto.UsuarioDto;
import com.motoboutique.seguridad.dto.UsuarioMapperImpl;
import com.motoboutique.seguridad.repository.RolRepository;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import com.motoboutique.shared.audit.AuditoriaService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private AuditoriaService auditoriaService;

    /** Encoder real con costo bajo para que las pruebas sean rápidas. */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UsuarioServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioServiceImpl(usuarioRepository, rolRepository,
                passwordEncoder, new UsuarioMapperImpl(), auditoriaService);
    }

    private static Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setNombre(codigo);
        return rol;
    }

    private static UsuarioCrearDto datosValidos() {
        return UsuarioCrearDto.de("  Ana Pérez ", "  Ana@Tienda.PE ", "clave-segura-1",
                "999111222", Set.of("ALMACENERO"));
    }

    @Test
    void crearNormalizaElCorreoYGuardaLaContrasenaCifrada() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@tienda.pe")).thenReturn(false);
        when(rolRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(rol("ALMACENERO")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioDto resultado = servicio.crear(datosValidos());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("ana@tienda.pe", guardado.getEmail());
        assertEquals("Ana Pérez", guardado.getNombre());
        assertNotEquals("clave-segura-1", guardado.getPassword());
        assertTrue(passwordEncoder.matches("clave-segura-1", guardado.getPassword()));
        assertEquals("ana@tienda.pe", resultado.getEmail());
        assertEquals(List.of("ALMACENERO"), resultado.getRoles());
    }

    @Test
    void crearRechazaUnCorreoYaRegistrado() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@tienda.pe")).thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.crear(datosValidos()));

        assertTrue(error.getMessage().contains("ana@tienda.pe"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearRechazaUnRolInexistente() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@tienda.pe")).thenReturn(false);
        when(rolRepository.findByCodigoIn(anyCollection())).thenReturn(List.of());

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.crear(datosValidos()));

        assertTrue(error.getMessage().contains("ALMACENERO"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearRechazaUnaContrasenaCorta() {
        UsuarioCrearDto datos = UsuarioCrearDto.de("Ana", "ana@tienda.pe", "corta", null, Set.of("ADMIN"));

        assertThrows(NegocioException.class, () -> servicio.crear(datos));

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void cambiarPasswordGuardaElNuevoHash() {
        Usuario usuario = new Usuario();
        usuario.setPassword("hash-anterior");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        servicio.cambiarPassword(5L, "otra-clave-9");

        assertTrue(passwordEncoder.matches("otra-clave-9", usuario.getPassword()));
    }

    @Test
    void cambiarPasswordRechazaUnaContrasenaCorta() {
        assertThrows(NegocioException.class, () -> servicio.cambiarPassword(5L, "1234567"));

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void obtenerPorIdLanzaErrorSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerPorId(99L));
    }

    @Test
    void obtenerPorIdTrataAUnUsuarioEliminadoComoInexistente() {
        Usuario eliminado = new Usuario();
        eliminado.marcarEliminado();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(eliminado));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerPorId(7L));
    }

    @Test
    void buscarPorEmailNormalizaElTextoAntesDeConsultar() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("ana@tienda.pe");
        when(usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse("ana@tienda.pe"))
                .thenReturn(Optional.of(usuario));

        Optional<UsuarioDto> resultado = servicio.buscarPorEmail("  ANA@Tienda.pe ");

        assertTrue(resultado.isPresent());
        assertEquals("Ana", resultado.get().getNombre());
    }
}
