package com.motoboutique.catalogo.domain;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="modelos_moto") @Getter @Setter @NoArgsConstructor
public class ModeloMoto extends AuditableEntity {
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="marca_id") private Marca marca;
 @Column(nullable=false,length=120) private String nombre;
 @Column(name="anio_inicio") private Integer anioInicio;
 @Column(name="anio_fin") private Integer anioFin;
 @Column(name="cilindrada_cc") private Integer cilindradaCc;
 @Column(length=40) private String tipo;
}
