package com.motoboutique.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cifrado de contraseñas con BCrypt.
 *
 * <p>Está separado de la configuración de Spring Security a propósito: el servicio de usuarios
 * necesita el encoder y la configuración de seguridad necesita el servicio de usuarios. Juntos
 * formarían una dependencia circular.</p>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
