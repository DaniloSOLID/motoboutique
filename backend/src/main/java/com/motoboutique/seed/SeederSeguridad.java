package com.motoboutique.seed;

import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.service.PermisoService;
import com.motoboutique.seguridad.service.RolService;
import com.motoboutique.seguridad.service.UsuarioService;
import com.motoboutique.shared.exception.NegocioException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales de seguridad: permisos, roles base y el administrador.
 *
 * <p>La contraseña del administrador NUNCA está en el código: llega por la variable de entorno
 * ADMIN_PASSWORD (en desarrollo, desde el archivo .env). Si el administrador ya existe no se
 * modifica, y si falta la contraseña el sistema arranca igual y lo avisa en el log.</p>
 *
 * <p>Los permisos de catálogo, inventario, compras, etc. los agrega el seeder de cada fase
 * (el rol ADMIN los recibe todos).</p>
 */
@Slf4j
@Component
@Order(10)
class SeederSeguridad implements SeederFase {

    private static final String MODULO = "seguridad";

    private record PermisoBase(String codigo, String descripcion) {
    }

    private record RolBase(String codigo, String nombre, String descripcion, List<String> permisos) {
    }

    private static final List<PermisoBase> PERMISOS = List.of(
            new PermisoBase("USUARIO_VER", "Ver la lista y el detalle de los usuarios"),
            new PermisoBase("USUARIO_CREAR", "Crear usuarios"),
            new PermisoBase("USUARIO_EDITAR",
                    "Editar usuarios, activarlos o desactivarlos y restablecer su contraseña"),
            new PermisoBase("ROL_VER", "Ver los roles y sus permisos"),
            new PermisoBase("ROL_EDITAR", "Crear roles y modificar los permisos de un rol"));

    private static final List<String> TODOS_LOS_PERMISOS = PERMISOS.stream()
            .map(PermisoBase::codigo)
            .toList();

    private static final List<RolBase> ROLES = List.of(
            new RolBase("ADMIN", "Administrador", "Acceso total al sistema", TODOS_LOS_PERMISOS),
            new RolBase("ALMACENERO", "Almacenero", "Catálogo, stock, compras y conteo físico", List.of()),
            new RolBase("CONTADOR", "Contador", "Solo lectura: kardex y reportes", List.of()),
            new RolBase("CONSULTA", "Consulta", "Consulta de catálogo y stock, sin modificar", List.of()));

    private final PermisoService permisoService;
    private final RolService rolService;
    private final UsuarioService usuarioService;
    private final String adminEmail;
    private final String adminPassword;

    SeederSeguridad(PermisoService permisoService,
                    RolService rolService,
                    UsuarioService usuarioService,
                    @Value("${ADMIN_EMAIL:admin@motoboutique.local}") String adminEmail,
                    @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.permisoService = permisoService;
        this.rolService = rolService;
        this.usuarioService = usuarioService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void ejecutar() {
        for (PermisoBase permiso : PERMISOS) {
            permisoService.asegurar(permiso.codigo(), permiso.descripcion(), MODULO);
        }
        for (RolBase rol : ROLES) {
            rolService.asegurar(rol.codigo(), rol.nombre(), rol.descripcion(), true, rol.permisos());
        }
        crearAdministrador();
    }

    private void crearAdministrador() {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No se creó el administrador: la variable ADMIN_EMAIL está vacía.");
            return;
        }
        if (usuarioService.existeEmail(adminEmail)) {
            log.info("El administrador {} ya existe", adminEmail);
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("No se creó el administrador: falta la variable ADMIN_PASSWORD. "
                    + "Defínela en el archivo .env y vuelve a arrancar.");
            return;
        }
        try {
            usuarioService.crear(UsuarioCrearDto.de("Administrador", adminEmail, adminPassword,
                    null, Set.of("ADMIN")));
            log.info("Administrador creado: {}", adminEmail);
        } catch (NegocioException e) {
            log.error("No se pudo crear el administrador: {}", e.getMessage());
        }
    }
}
