package com.motoboutique.catalogo.domain;

import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unidades_moto")
@Getter
@Setter
@NoArgsConstructor
public class UnidadMoto extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_moto_id")
    private ModeloMoto modelo;
    @Column(nullable = false, length = 40) private String chasis;
    @Column(nullable = false, length = 40) private String motor;
    @Column(length = 40) private String color;
    @Column(nullable = false, length = 20) private String estado = "DISPONIBLE";
    @Column(columnDefinition = "text") private String observaciones;
}
