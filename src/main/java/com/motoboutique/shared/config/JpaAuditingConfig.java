package com.motoboutique.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa la auditoría automática de JPA (@CreatedBy, @CreatedDate, etc.).
 * Está en una clase aparte (y no en la clase principal) para no romper las pruebas de capas web.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
