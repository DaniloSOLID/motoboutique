package com.motoboutique.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security.
 *
 * <ul>
 *   <li>Todo el sistema requiere sesión, salvo el acceso (login, recuperar y restablecer
 *       contraseña) y los archivos estáticos.</li>
 *   <li>CSRF activo (valor por defecto) en todos los POST, también en los endpoints JSON internos.</li>
 *   <li>@PreAuthorize habilitado para exigir roles y permisos en los métodos.</li>
 *   <li>El usuario sale de {@code UsuarioDetailsService} y la contraseña se compara con el
 *       PasswordEncoder (BCrypt) definido en {@code PasswordEncoderConfig}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/login", "/recuperar", "/restablecer", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll())
                .logout(salida -> salida
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID"));
        return http.build();
    }
}