package com.motoboutique.seed;

import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.service.UsuarioService;
import com.motoboutique.shared.exception.NegocioException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Un usuario demo por cada rol (excepto el administrador), SOLO en el perfil dev.
 * Comparten la contraseña de la variable DEMO_PASSWORD; si está vacía no se crean.
 */
@Slf4j
@Component
@Profile("dev")
@Order(20)
class SeederUsuariosDemo implements SeederFase {

    private record UsuarioDemo(String nombre, String email, String rol) {
    }

    private static final List<UsuarioDemo> USUARIOS = List.of(
            new UsuarioDemo("Demo Almacenero", "almacenero@motoboutique.local", "ALMACENERO"),
            new UsuarioDemo("Demo Contador", "contador@motoboutique.local", "CONTADOR"),
            new UsuarioDemo("Demo Consulta", "consulta@motoboutique.local", "CONSULTA"));

    private final UsuarioService usuarioService;
    private final String demoPassword;

    SeederUsuariosDemo(UsuarioService usuarioService, @Value("${DEMO_PASSWORD:}") String demoPassword) {
        this.usuarioService = usuarioService;
        this.demoPassword = demoPassword;
    }

    @Override
    public void ejecutar() {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.info("Usuarios demo omitidos: falta la variable DEMO_PASSWORD");
            return;
        }
        for (UsuarioDemo demo : USUARIOS) {
            if (usuarioService.existeEmail(demo.email())) {
                continue;
            }
            try {
                usuarioService.crear(UsuarioCrearDto.de(demo.nombre(), demo.email(), demoPassword,
                        null, Set.of(demo.rol())));
            } catch (NegocioException e) {
                log.error("No se pudo crear el usuario demo {}: {}", demo.email(), e.getMessage());
            }
        }
    }
}
