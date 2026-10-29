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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", precision = 14, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "saldo_cantidad", nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoCantidad;

    @Column(name = "costo_promedio", precision = 14, scale = 2)
    private BigDecimal costoPromedio;

    @Column(length = 100)
    private String referencia;

    @Column(name = "documento_referencia", length = 100)
    private String documentoReferencia;

    @Column(length = 255)
    private String motivo;

    @Column(nullable = false)
    private Instant fecha = Instant.now();
}
