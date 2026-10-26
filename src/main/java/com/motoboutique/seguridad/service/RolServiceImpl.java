package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.domain.Permiso;
import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.dto.RolDto;
import com.motoboutique.seguridad.dto.RolMapper;
import com.motoboutique.seguridad.repository.PermisoRepository;
import com.motoboutique.seguridad.repository.RolRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolMapper rolMapper;

    @Override
    @Transactional
    public RolDto asegurar(String codigo, String nombre, String descripcion, boolean sistema,
                           Collection<String> codigosPermisos) {
        Set<Permiso> permisos = resolverPermisos(codigosPermisos);

        Rol rol = rolRepository.findByCodigo(codigo).orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setCodigo(codigo);
            nuevo.setNombre(nombre);
            nuevo.setDescripcion(descripcion);
            nuevo.setSistema(sistema);
            return nuevo;
        });
        rol.getPermisos().addAll(permisos);

        return rolMapper.toDto(rolRepository.save(rol));
    }

    @Override
    public List<RolDto> listar() {
        return rolMapper.toDtoList(rolRepository.findByEliminadoFalseOrderByNombreAsc());
    }

    @Override
    public Optional<RolDto> buscarPorCodigo(String codigo) {
        return rolRepository.findByCodigo(codigo).map(rolMapper::toDto);
    }

    private Set<Permiso> resolverPermisos(Collection<String> codigos) {
        Set<String> pedidos = codigos == null ? Set.of() : new LinkedHashSet<>(codigos);
        if (pedidos.isEmpty()) {
            return new HashSet<>();
        }
        List<Permiso> encontrados = permisoRepository.findByCodigoIn(pedidos);
        Set<String> codigosEncontrados = encontrados.stream()
                .map(Permiso::getCodigo)
                .collect(Collectors.toSet());
        List<String> faltantes = pedidos.stream()
                .filter(codigo -> !codigosEncontrados.contains(codigo))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new NegocioException("No existen los permisos: " + String.join(", ", faltantes));
        }
        return new HashSet<>(encontrados);
    }
}
