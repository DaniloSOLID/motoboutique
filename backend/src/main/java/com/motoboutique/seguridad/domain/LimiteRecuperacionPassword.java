package com.motoboutique.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "limite_recuperacion_password")
@Getter
@Setter
@NoArgsConstructor
public class LimiteRecuperacionPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String clave;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ventana_inicio", nullable = false)
    private Instant ventanaInicio;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;
}
