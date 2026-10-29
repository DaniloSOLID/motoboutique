package com.motoboutique.compras.domain;

import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor extends AuditableEntity {
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "razon_social", nullable = false, length = 180)
    private String razonSocial;

    @Column(length = 11, unique = true)
    private String ruc;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private boolean activo = true;
}
