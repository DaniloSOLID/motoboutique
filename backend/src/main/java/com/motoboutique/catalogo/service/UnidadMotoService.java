package com.motoboutique.catalogo.service;

import com.motoboutique.catalogo.domain.UnidadMoto;
import com.motoboutique.catalogo.dto.UnidadMotoDto;
import com.motoboutique.catalogo.dto.UnidadMotoGuardarDto;
import com.motoboutique.catalogo.repository.ModeloMotoRepository;
import com.motoboutique.catalogo.repository.UnidadMotoRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import com.motoboutique.shared.web.PageResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnidadMotoService {
    private final UnidadMotoRepository unidades;
    private final ModeloMotoRepository modelos;

    public PageResponse<UnidadMotoDto> listar(String q, Long modeloId, String estado, Pageable pageable) {
        Specification<UnidadMoto> spec = (root, query, cb) -> cb.isFalse(root.get("eliminado"));
        if (q != null && !q.isBlank()) { String value = "%" + q.trim().toLowerCase() + "%"; spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("chasis")), value), cb.like(cb.lower(root.get("motor")), value), cb.like(cb.lower(root.get("color")), value))); }
        if (modeloId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("modelo").get("id"), modeloId));
        if (estado != null && !estado.isBlank()) spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado.toUpperCase()));
        return PageResponse.de(unidades.findAll(spec, pageable), this::dto);
    }
    public UnidadMotoDto obtener(Long id) { return dto(unidad(id)); }

    @Transactional public UnidadMotoDto crear(UnidadMotoGuardarDto d) {
        validarIdentificadores(d.chasis(), d.motor(), null);
        UnidadMoto u = new UnidadMoto();
        String estado = normalizarEstado(d.estado(), "DISPONIBLE");
        if (!"DISPONIBLE".equals(estado)) {
            throw new NegocioException("Una unidad nueva debe iniciar en estado DISPONIBLE.");
        }
        u.setModelo(modelo(d.modeloMotoId())); u.setChasis(d.chasis().trim().toUpperCase()); u.setMotor(d.motor().trim().toUpperCase()); u.setColor(d.color()); u.setEstado(estado); u.setObservaciones(d.observaciones());
        return dto(unidades.save(u));
    }
    @Transactional public UnidadMotoDto actualizar(Long id, UnidadMotoGuardarDto d) {
        UnidadMoto u = unidad(id); validarIdentificadores(d.chasis(), d.motor(), id);
        String nuevoEstado = normalizarEstado(d.estado(), u.getEstado());
        validarTransicion(u.getEstado(), nuevoEstado);
        u.setModelo(modelo(d.modeloMotoId())); u.setChasis(d.chasis().trim().toUpperCase()); u.setMotor(d.motor().trim().toUpperCase()); u.setColor(d.color()); u.setEstado(nuevoEstado); u.setObservaciones(d.observaciones());
        return dto(unidades.save(u));
    }
    @Transactional public UnidadMotoDto cambiarEstado(Long id, String estado) { UnidadMoto u = unidad(id); String nuevo = normalizarEstado(estado, u.getEstado()); validarTransicion(u.getEstado(), nuevo); u.setEstado(nuevo); return dto(unidades.save(u)); }
    @Transactional public void eliminar(Long id) { unidad(id).marcarEliminado(); }

    private UnidadMoto unidad(Long id) { return unidades.findByIdAndEliminadoFalse(id).orElseThrow(() -> RecursoNoEncontradoException.de("Unidad de moto", id)); }
    private com.motoboutique.catalogo.domain.ModeloMoto modelo(Long id) { return modelos.findByIdAndEliminadoFalse(id).orElseThrow(() -> RecursoNoEncontradoException.de("Modelo de moto", id)); }
    private void validarIdentificadores(String chasis, String motor, Long id) {
        String chasisNormalizado = chasis.trim().toUpperCase();
        String motorNormalizado = motor.trim().toUpperCase();
        boolean chasisDuplicado = id == null
                ? unidades.existsByChasisIgnoreCase(chasisNormalizado)
                : unidades.existsByChasisIgnoreCaseAndIdNot(chasisNormalizado, id);
        boolean motorDuplicado = id == null
                ? unidades.existsByMotorIgnoreCase(motorNormalizado)
                : unidades.existsByMotorIgnoreCaseAndIdNot(motorNormalizado, id);
        if (chasisDuplicado) throw new NegocioException("Ya existe una unidad con ese chasis.");
        if (motorDuplicado) throw new NegocioException("Ya existe una unidad con ese motor.");
    }
    private String normalizarEstado(String estado, String defecto) { if (estado == null || estado.isBlank()) return defecto; String valor = estado.toUpperCase(); if (!Set.of("DISPONIBLE", "RESERVADA", "VENDIDA").contains(valor)) throw new NegocioException("Estado de unidad inválido."); return valor; }
    private void validarTransicion(String origen, String destino) {
        boolean valida = ("DISPONIBLE".equals(origen) && Set.of("RESERVADA", "VENDIDA").contains(destino))
                || ("RESERVADA".equals(origen) && Set.of("DISPONIBLE", "VENDIDA").contains(destino));
        if (!origen.equals(destino) && !valida) throw new NegocioException("Transición de estado inválida: " + origen + " -> " + destino);
    }
    private UnidadMotoDto dto(UnidadMoto u) { return new UnidadMotoDto(u.getId(), u.getModelo().getId(), u.getModelo().getNombre(), u.getChasis(), u.getMotor(), u.getColor(), u.getEstado(), u.getObservaciones()); }
}
