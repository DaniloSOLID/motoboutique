package com.motoboutique.inventario.service;

import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.inventario.dto.AlmacenDto;
import com.motoboutique.inventario.dto.AlmacenGuardarDto;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.inventario.repository.StockRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlmacenService {
    private final AlmacenRepository almacenRepository;
    private final StockRepository stockRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    @Transactional(readOnly = true)
    public List<AlmacenDto> listar() {
        return almacenRepository.findByEliminadoFalseOrderByNombre().stream()
                .map(this::dto).toList();
    }

    @Transactional
    public AlmacenDto crear(AlmacenGuardarDto solicitud) {
        if (almacenRepository.existsByCodigoIgnoreCase(solicitud.codigo().trim())) {
            throw new NegocioException("Ya existe un almacén con ese código.");
        }
        Almacen almacen = new Almacen();
        aplicar(almacen, solicitud, true);
        return dto(almacenRepository.save(almacen));
    }

    @Transactional
    public AlmacenDto editar(Long id, AlmacenGuardarDto solicitud) {
        Almacen almacen = buscar(id);
        if (!almacen.getCodigo().equalsIgnoreCase(solicitud.codigo().trim())
                && almacenRepository.existsByCodigoIgnoreCase(solicitud.codigo().trim())) {
            throw new NegocioException("Ya existe un almacén con ese código.");
        }
        aplicar(almacen, solicitud, false);
        return dto(almacen);
    }

    @Transactional
    public void eliminar(Long id) {
        Almacen almacen = buscar(id);
        if (stockRepository.countByAlmacenIdAndEliminadoFalse(id) > 0
                || movimientoRepository.existsByAlmacenIdAndEliminadoFalse(id)) {
            throw new NegocioException("No se puede eliminar un almacén con stock o movimientos asociados.");
        }
        almacen.marcarEliminado();
    }

    private Almacen buscar(Long id) {
        return almacenRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Almacén", id));
    }

    private void aplicar(Almacen almacen, AlmacenGuardarDto solicitud, boolean nuevo) {
        almacen.setCodigo(solicitud.codigo().trim());
        almacen.setNombre(solicitud.nombre().trim());
        almacen.setDireccion(solicitud.direccion() == null ? null : solicitud.direccion().trim());
        almacen.setActivo(solicitud.activo() == null ? nuevo || almacen.isActivo() : solicitud.activo());
    }

    private AlmacenDto dto(Almacen almacen) {
        return new AlmacenDto(almacen.getId(), almacen.getCodigo(), almacen.getNombre(),
                almacen.getDireccion(), almacen.isActivo());
    }
}
