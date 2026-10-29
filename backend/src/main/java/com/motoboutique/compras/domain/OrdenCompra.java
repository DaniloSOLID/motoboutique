package com.motoboutique.compras.domain;

import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
public class OrdenCompra extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    private List<OrdenCompraDetalle> detalles = new ArrayList<>();
}
