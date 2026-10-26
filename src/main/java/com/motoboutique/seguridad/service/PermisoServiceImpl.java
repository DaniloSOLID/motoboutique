package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.domain.Permiso;
import com.motoboutique.seguridad.dto.PermisoDto;
import com.motoboutique.seguridad.dto.PermisoMapper;
import com.motoboutique.seguridad.repository.PermisoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PermisoServiceImpl implements PermisoService {

    private final PermisoRepository permisoRepository;
    private final PermisoMapper permisoMapper;

    @Override
    @Transactional
    public PermisoDto asegurar(String codigo, String descripcion, String modulo) {
        Permiso permiso = permisoRepository.findByCodigo(codigo).orElseGet(() -> {
            Permiso nuevo = new Permiso();
            nuevo.setCodigo(codigo);
            return nuevo;
        });
        permiso.setDescripcion(descripcion);
        permiso.setModulo(modulo);
        return permisoMapper.toDto(permisoRepository.save(permiso));
    }

    @Override
    public List<PermisoDto> listar() {
        return permisoMapper.toDtoList(permisoRepository.findByEliminadoFalseOrderByModuloAscCodigoAsc());
    }
}
