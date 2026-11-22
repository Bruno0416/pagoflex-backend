package com.pagoflex.soporte.features.reporte.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/// Una página de resultados con lo justo para que la app pida la siguiente
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
