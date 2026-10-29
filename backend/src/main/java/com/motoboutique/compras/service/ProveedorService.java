package com.motoboutique.compras.service;

import com.motoboutique.compras.domain.Proveedor;
import com.motoboutique.compras.dto.ProveedorDto;
import com.motoboutique.compras.dto.ProveedorGuardarDto;
import com.motoboutique.compras.repository.CodigoProveedorRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.compras.repository.ProveedorRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import com.motoboutique.shared.web.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorService {
    private static final Set<String> ORDENES = Set.of("razonSocial", "codigo", "ruc", "activo");
    private final ProveedorRepository proveedorRepository;
    private final CodigoProveedorRepository codigoRepository;
    private final OrdenCompraRepository ordenRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProveedorDto> listar(String q, Boolean activo, int page, int size,
                                             String sort, String direction) {
        Specification<Proveedor> specification = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isFalse(root.get("eliminado")));
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(cb.like(cb.lower(root.get("codigo")), patron),
                        cb.like(cb.lower(root.get("razonSocial")), patron),
                        cb.like(cb.lower(root.get("ruc")), patron)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        Page<Proveedor> resultado = proveedorRepository.findAll(specification,
                pagina(page, size, sort, direction));
        return PageResponse.de(resultado, this::dto);
    }

    @Transactional
    public ProveedorDto crear(ProveedorGuardarDto solicitud) {
        validarRucUnico(solicitud.ruc(), null);
        Proveedor proveedor = new Proveedor();
        proveedor.setCodigo(codigoRepository.siguienteCodigo());
        aplicar(proveedor, solicitud, true);
        return dto(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorDto editar(Long id, ProveedorGuardarDto solicitud) {
        Proveedor proveedor = buscar(id);
        validarRucUnico(solicitud.ruc(), id);
        aplicar(proveedor, solicitud, false);
        return dto(proveedor);
    }

    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = buscar(id);
        if (ordenRepository.existsByProveedorIdAndEliminadoFalse(id)) {
            throw new NegocioException("No se puede eliminar un proveedor que tiene órdenes de compra.");
        }
        proveedor.marcarEliminado();
    }

    private void validarRucUnico(String ruc, Long id) {
        if (ruc == null || ruc.isBlank()) {
            return;
        }
        boolean existe = id == null ? proveedorRepository.existsByRuc(ruc)
                : proveedorRepository.existsByRucAndIdNot(ruc, id);
        if (existe) {
            throw new NegocioException("Ya existe un proveedor registrado con ese RUC.");
        }
    }

    private void aplicar(Proveedor proveedor, ProveedorGuardarDto solicitud, boolean nuevo) {
        proveedor.setRazonSocial(solicitud.razonSocial().trim());
        proveedor.setRuc(solicitud.ruc() == null || solicitud.ruc().isBlank()
                ? null : solicitud.ruc().trim());
        proveedor.setTelefono(limpiar(solicitud.telefono()));
        proveedor.setEmail(limpiar(solicitud.email()));
        proveedor.setDireccion(limpiar(solicitud.direccion()));
        proveedor.setActivo(solicitud.activo() == null ? nuevo || proveedor.isActivo() : solicitud.activo());
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id).filter(proveedor -> !proveedor.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Proveedor", id));
    }

    private ProveedorDto dto(Proveedor proveedor) {
        return new ProveedorDto(proveedor.getId(), proveedor.getCodigo(), proveedor.getRazonSocial(),
                proveedor.getRuc(), proveedor.getTelefono(), proveedor.getEmail(),
                proveedor.getDireccion(), proveedor.isActivo());
    }

    private Pageable pagina(int page, int size, String sort, String direction) {
        String columna = sort != null && ORDENES.contains(sort) ? sort : "razonSocial";
        Sort.Direction sentido = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)),
                Sort.by(sentido, columna));
    }
}
