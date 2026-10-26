package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.dto.UsuarioDto;
import com.motoboutique.seguridad.dto.UsuarioMapper;
import com.motoboutique.seguridad.repository.RolRepository;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UsuarioServiceImpl implements UsuarioService {

    static final int PASSWORD_MINIMO = 8;
    /** BCrypt solo usa los primeros 72 bytes de la contraseña. */
    static final int PASSWORD_MAXIMO_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional
    public UsuarioDto crear(UsuarioCrearDto datos) {
        String email = normalizarEmail(datos.getEmail());
        validarPassword(datos.getPassword());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new NegocioException("Ya existe un usuario con el correo " + email + ".");
        }
        Set<Rol> roles = resolverRoles(datos.getRoles());

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.getNombre().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(datos.getPassword()));
        usuario.setTelefono(limpiar(datos.getTelefono()));
        usuario.setRoles(roles);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado: {}", email);
        return usuarioMapper.toDto(guardado);
    }

    @Override
    public UsuarioDto obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .filter(usuario -> !usuario.isEliminado())
                .map(usuarioMapper::toDto)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
    }

    @Override
    public Optional<UsuarioDto> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmailIgnoreCaseAndEliminadoFalse(normalizarEmail(email))
                .map(usuarioMapper::toDto);
    }

    @Override
    public boolean existeEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return usuarioRepository.existsByEmailIgnoreCase(normalizarEmail(email));
    }

    @Override
    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordNueva) {
        validarPassword(passwordNueva);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(encontrado -> !encontrado.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", usuarioId));
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        log.info("Contraseña actualizada para el usuario con id {}", usuarioId);
    }

    // ------------------------------------------------------------------
    // Reglas internas
    // ------------------------------------------------------------------

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private void validarPassword(String password) {
        boolean muyCorta = password == null || password.length() < PASSWORD_MINIMO;
        boolean muyLarga = password != null
                && password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAXIMO_BYTES;
        if (muyCorta || muyLarga) {
            throw new NegocioException("La contraseña debe tener entre " + PASSWORD_MINIMO
                    + " y " + PASSWORD_MAXIMO_BYTES + " caracteres.");
        }
    }

    private Set<Rol> resolverRoles(Collection<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            throw new NegocioException("El usuario debe tener al menos un rol.");
        }
        Set<String> pedidos = new LinkedHashSet<>(codigos);
        List<Rol> encontrados = rolRepository.findByCodigoIn(pedidos).stream()
                .filter(rol -> !rol.isEliminado())
                .toList();
        Set<String> codigosEncontrados = encontrados.stream()
                .map(Rol::getCodigo)
                .collect(Collectors.toSet());
        List<String> faltantes = pedidos.stream()
                .filter(codigo -> !codigosEncontrados.contains(codigo))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new NegocioException("No existen los roles: " + String.join(", ", faltantes));
        }
        return new HashSet<>(encontrados);
    }
}
