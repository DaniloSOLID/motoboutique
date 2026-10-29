package com.motoboutique.inventario.domain;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conteo_detalles",
        uniqueConstraints = @UniqueConstraint(name = "uq_conteo_producto",
                columnNames = {"conteo_id", "producto_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ConteoDetalle extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteo_id", nullable = false)
    private ConteoFisico conteo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "stock_sistema", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockSistema;

    @Column(name = "stock_contado", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockContado;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal diferencia;

    @Column(nullable = false)
    private boolean ajustado;
}
