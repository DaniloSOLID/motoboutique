package com.motoboutique.inventario.domain;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.seguridad.domain.Usuario;
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
@Table(name = "ajustes_inventario")
@Getter
@Setter
@NoArgsConstructor
public class AjusteInventario extends AuditableEntity {

    @Column(nullable = false, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(nullable = false, length = 20)
    private String origen = "MANUAL";

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobador_id")
    private Usuario aprobador;

    @Column(columnDefinition = "text")
    private String comentario;

    @Column(name = "fecha_solicitud", nullable = false)
    private Instant fechaSolicitud = Instant.now();

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteo_detalle_id")
    private ConteoDetalle conteoDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_id")
    private MovimientoInventario movimiento;
}
