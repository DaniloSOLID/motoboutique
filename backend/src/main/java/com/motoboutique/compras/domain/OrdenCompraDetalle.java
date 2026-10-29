package com.motoboutique.compras.domain;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orden_compra_detalles")
@Getter
@Setter
@NoArgsConstructor
public class OrdenCompraDetalle extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenCompra orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "cantidad_recibida", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadRecibida = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;
}
