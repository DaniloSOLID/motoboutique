package com.motoboutique.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_eventos")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaEvento extends AuditableEntity {

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(nullable = false, length = 100)
    private String entidad;

    @Column(name = "entidad_id", length = 80)
    private String entidadId;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(nullable = false)
    private Instant fecha = Instant.now();
}
