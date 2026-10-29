package com.motoboutique.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "importacion_errores")
@Getter
@Setter
@NoArgsConstructor
public class ImportacionError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importacion_id", nullable = false)
    private Importacion importacion;

    @Column(nullable = false)
    private int fila;

    @Column(length = 80)
    private String columna;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;
}
