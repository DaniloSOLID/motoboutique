package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.dto.UsuarioDto;
import com.motoboutique.seguridad.dto.UsuarioMapper;
import com.motoboutique.seguridad.repository.RolRepository;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import com.motoboutique.shared.audit.AuditoriaService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.motoboutique.shared.web.PageResponse;
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
    private final AuditoriaService auditoriaService;

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

    @Override
    public PageResponse<UsuarioDto> listar(String q, Boolean activo, String rol, int page, int size,
            String sort, String direction) {
        validarPaginacion(page, size);
        String campo = switch (sort == null ? "nombre" : sort) {
            case "email" -> "email";
            case "creadoEn" -> "creadoEn";
            default -> "nombre";
        };
        Sort.Direction sentido = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Specification<Usuario> spec = (root, query, cb) -> {
            var filtros = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            filtros.add(cb.isFalse(root.get("eliminado")));
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                filtros.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("email")), patron)));
            }
            if (activo != null) {
                filtros.add(cb.equal(root.get("activo"), activo));
            }
            if (rol != null && !rol.isBlank()) {
                var subconsulta = query.subquery(Long.class);
                var usuarioRelacionado = subconsulta.from(Usuario.class);
                subconsulta.select(usuarioRelacionado.get("id")).where(
                        cb.equal(usuarioRelacionado.get("id"), root.get("id")),
                        cb.equal(usuarioRelacionado.join("roles").get("codigo"), rol.trim()));
                filtros.add(cb.exists(subconsulta));
            }
            return cb.and(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageResponse.de(usuarioRepository.findAll(spec, PageRequest.of(page, size,
                Sort.by(sentido, campo))), usuarioMapper::toDto);
    }

    @Override
    @Transactional
    public UsuarioDto editar(Long id, String nombre, String email, Collection<String> codigosRoles,
            Boolean activo, Long actorId) {
        Usuario usuario = usuarioRepository.buscarConRoles(id)
                .filter(u -> !u.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
        String emailNormalizado = normalizarEmail(email);
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, id)) {
            throw new NegocioException("Ya existe un usuario con ese correo.");
        }
        Set<Rol> rolesNuevos = resolverRoles(codigosRoles);
        if (actorId.equals(id) && Boolean.FALSE.equals(activo)) {
            throw new NegocioException("No puedes desactivar tu propio usuario.");
        }
        boolean eliminaAdmin = usuario.isActivo() && usuario.tieneRol("ADMIN")
                && (Boolean.FALSE.equals(activo)
                    || rolesNuevos.stream().noneMatch(r -> "ADMIN".equals(r.getCodigo())));
        if (eliminaAdmin && usuarioRepository.contarActivosConRol("ADMIN") <= 1) {
            throw new NegocioException("No se puede desactivar ni quitar el rol del último administrador activo.");
        }
        usuario.setNombre(nombre.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setRoles(rolesNuevos);
        if (activo != null) {
            usuario.setActivo(activo);
        }
        auditoriaService.registrar("EDITAR_USUARIO", "USUARIO", id.toString(),
                "Se actualizaron los datos del usuario.");
        return usuarioMapper.toDto(usuario);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, boolean activo, Long actorId) {
        Usuario usuario = usuarioRepository.buscarConRoles(id)
                .filter(u -> !u.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
        if (actorId.equals(id) && !activo) {
            throw new NegocioException("No puedes desactivar tu propio usuario.");
        }
        protegerUltimoAdmin(usuario, activo);
        usuario.setActivo(activo);
        auditoriaService.registrar(activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
                "USUARIO", id.toString(), null);
    }

    @Override
    @Transactional
    public void eliminar(Long id, Long actorId) {
        if (actorId.equals(id)) {
            throw new NegocioException("No puedes eliminar tu propio usuario.");
        }
        Usuario usuario = usuarioRepository.buscarConRoles(id)
                .filter(u -> !u.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
        protegerUltimoAdmin(usuario, false);
        usuario.marcarEliminado();
        usuario.setActivo(false);
        auditoriaService.registrar("ELIMINAR_USUARIO", "USUARIO", id.toString(), null);
    }

    @Override
    @Transactional
    public void restablecerPasswordAdmin(Long id, String passwordNueva) {
        cambiarPassword(id, passwordNueva);
        auditoriaService.registrar("RESTABLECER_PASSWORD", "USUARIO", id.toString(),
                "Contraseña restablecida por un administrador.");
    }

    private void protegerUltimoAdmin(Usuario usuario, boolean nuevoActivo) {
        if (usuario.isActivo() && usuario.tieneRol("ADMIN") && !nuevoActivo
                && usuarioRepository.contarActivosConRol("ADMIN") <= 1) {
            throw new NegocioException("No se puede desactivar ni eliminar al último administrador activo.");
        }
    }

    private void validarPaginacion(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new NegocioException("La página o el tamaño no son válidos.");
        }
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
