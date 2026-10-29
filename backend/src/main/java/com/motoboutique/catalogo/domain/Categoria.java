package com.motoboutique.catalogo.domain;
import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="categorias") @Getter @Setter @NoArgsConstructor
public class Categoria extends AuditableEntity { @Column(nullable=false,length=100) private String nombre; @Column private String descripcion; @Column(nullable=false) private boolean activo=true; }
