package com.motoboutique.inventario.domain;

import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conteos_fisicos")
@Getter
@Setter
@NoArgsConstructor
public class ConteoFisico extends AuditableEntity {

    @Column(nullable = false, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO";

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura = Instant.now();

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Column(columnDefinition = "text")
    private String observaciones;
}
