package com.motoboutique.catalogo.domain;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.util.*; import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="productos") @Getter @Setter @NoArgsConstructor
public class Producto extends AuditableEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="categoria_id") private Categoria categoria;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="marca_id") private Marca marca;
 @Column(nullable=false,length=60) private String codigo; @Column(nullable=false,length=180) private String nombre; @Column(columnDefinition="text") private String descripcion;
 @Column(name="precio_compra",nullable=false,precision=14,scale=2) private BigDecimal precioCompra; @Column(name="precio_venta",nullable=false,precision=14,scale=2) private BigDecimal precioVenta; @Column(name="stock_minimo",nullable=false,precision=14,scale=3) private BigDecimal stockMinimo=BigDecimal.ZERO;
 @Column(nullable=false,length=20) private String unidad="UNIDAD"; @Column(nullable=false) private boolean activo=true;
 @Column(name="imagen_url", columnDefinition="text") private String imagenUrl;
 @JdbcTypeCode(SqlTypes.JSON)
 @Column(columnDefinition="jsonb", nullable=false) private Map<String,Object> especificaciones=new HashMap<>();
 @ManyToMany @JoinTable(name="producto_compatibilidades",joinColumns=@JoinColumn(name="producto_id"),inverseJoinColumns=@JoinColumn(name="modelo_moto_id")) private Set<ModeloMoto> compatibilidades=new HashSet<>();
}
