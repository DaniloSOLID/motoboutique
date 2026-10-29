package com.motoboutique.catalogo.domain;

import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "importaciones")
@Getter
@Setter
@NoArgsConstructor
public class Importacion extends AuditableEntity {
    @Column(nullable = false, length = 255)
    private String archivo;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    @Column(name = "filas_validas", nullable = false)
    private Integer filasValidas = 0;

    @Column(nullable = false)
    private Integer errores = 0;

    @Column(nullable = false, length = 20)
    private String estado = "COMPLETADA";

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
}
