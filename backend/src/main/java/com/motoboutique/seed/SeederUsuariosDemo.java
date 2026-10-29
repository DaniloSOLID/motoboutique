package com.motoboutique.seed;

import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.service.UsuarioService;
import com.motoboutique.shared.exception.NegocioException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final UsuarioService usuarioService;
    private final String demoPassword;
    private final boolean resetPasswords;
    private final List<UsuarioDemo> usuarios;

    @Autowired
    SeederUsuariosDemo(UsuarioService usuarioService,
                      @Value("${DEMO_PASSWORD:}") String demoPassword,
                      @Value("${ALMACENERO_EMAIL:almacenero@motoboutique.local}") String almaceneroEmail,
                      @Value("${CONTADOR_EMAIL:contador@motoboutique.local}") String contadorEmail,
                      @Value("${CONSULTA_EMAIL:consulta@motoboutique.local}") String consultaEmail,
                      @Value("${app.seed.reset-passwords:false}") boolean resetPasswords) {
        this.usuarioService = usuarioService;
        this.demoPassword = demoPassword;
        this.resetPasswords = resetPasswords;
        this.usuarios = List.of(
                new UsuarioDemo("almacenero", almaceneroEmail, "ALMACENERO"),
                new UsuarioDemo("contador", contadorEmail, "CONTADOR"),
                new UsuarioDemo("consulta", consultaEmail, "CONSULTA"));
    }

    SeederUsuariosDemo(UsuarioService usuarioService, String demoPassword) {
        this(usuarioService, demoPassword,
                "almacenero@motoboutique.local", "contador@motoboutique.local",
                "consulta@motoboutique.local", false);
    }

    @Override
    public void ejecutar() {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.info("Usuarios demo omitidos: falta la variable DEMO_PASSWORD");
            return;
        }
        for (UsuarioDemo demo : usuarios) {
            if (usuarioService.existeEmail(demo.email())) {
                if (resetPasswords) {
                    usuarioService.buscarPorEmail(demo.email()).ifPresent(usuario ->
                            usuarioService.cambiarPassword(usuario.getId(), demoPassword));
                }
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
