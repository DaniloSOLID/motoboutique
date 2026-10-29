package com.motoboutique.shared.audit;

import com.motoboutique.shared.web.PageResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaEventoRepository repository;

    @Transactional
    public void registrar(String accion, String entidad, String entidadId, String detalle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setActor(auth == null || !auth.isAuthenticated() ? "sistema" : auth.getName());
        evento.setAccion(accion);
        evento.setEntidad(entidad);
        evento.setEntidadId(entidadId);
        evento.setDetalle(detalle);
        evento.setFecha(Instant.now());
        repository.save(evento);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaDto> listar(String q, String usuario, String accion,
                                             Instant desde, Instant hasta, int page, int size) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new com.motoboutique.shared.exception.NegocioException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
        return PageResponse.de(repository.buscar(normalizar(q), normalizar(usuario),
                normalizar(accion), desde, hasta, pageable), a ->
                new AuditoriaDto(a.getActor(), a.getAccion(), a.getEntidad(), a.getEntidadId(),
                        a.getDetalle(), a.getFecha()));
    }

    private String normalizar(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    public record AuditoriaDto(String quien, String que, String entidad, String entidadId,
                               String detalle, Instant cuando) {}
}
