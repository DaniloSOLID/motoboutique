package com.motoboutique.inventario.domain;

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
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Column(name = "stock_reservado", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockReservado = BigDecimal.ZERO;
}
