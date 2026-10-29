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
@Table(name = "transferencia_detalles")
@Getter
@Setter
@NoArgsConstructor
public class TransferenciaDetalle extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferencia_id", nullable = false)
    private Transferencia transferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_salida_id")
    private MovimientoInventario movimientoSalida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_entrada_id")
    private MovimientoInventario movimientoEntrada;
}
