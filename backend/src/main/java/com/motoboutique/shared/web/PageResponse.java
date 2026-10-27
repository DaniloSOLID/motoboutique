package com.motoboutique.shared.web;

import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * Página de resultados lista para el frontend: contiene DTOs (nunca entidades) y los datos
 * que necesita la paginación.
 *
 * <p>Uso típico en un service: {@code PageResponse.de(repo.findAll(spec, pageable), mapper::toDto)}.</p>
 */
@Getter
public final class PageResponse<T> {

    private final List<T> contenido;
    /** Número de página base 0 (como Spring Data). */
    private final int numero;
    private final int tamanio;
    private final long totalElementos;
    private final int totalPaginas;

    public PageResponse(List<T> contenido, int numero, int tamanio, long totalElementos, int totalPaginas) {
        this.contenido = List.copyOf(contenido);
        this.numero = numero;
        this.tamanio = tamanio;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
    }

    /** Convierte una página de Spring Data sin transformar su contenido. */
    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /** Convierte una página de entidades a una página de DTOs usando el conversor indicado. */
    public static <E, T> PageResponse<T> de(Page<E> page, Function<? super E, ? extends T> conversor) {
        List<T> items = page.getContent().stream().<T>map(conversor).toList();
        return new PageResponse<>(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> vacia(int tamanio) {
        return new PageResponse<>(List.of(), 0, tamanio, 0, 0);
    }

    /** Número de página base 1, para mostrar al usuario. */
    public int getPaginaActual() {
        return numero + 1;
    }

    public boolean isPrimera() {
        return numero == 0;
    }

    public boolean isUltima() {
        return numero >= totalPaginas - 1;
    }

    public boolean isVacia() {
        return contenido.isEmpty();
    }

    /** Primer registro mostrado (para "Mostrando 21-40 de 340"). */
    public long getDesde() {
        return isVacia() ? 0 : (long) numero * tamanio + 1;
    }

    /** Último registro mostrado. */
    public long getHasta() {
        return isVacia() ? 0 : (long) numero * tamanio + contenido.size();
    }
}
