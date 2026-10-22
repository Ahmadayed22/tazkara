package com.tazkara.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for every paginated list (instead of serialising Spring's {@link Page} directly).
 *
 * @param <T> the DTO type of the items
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<? super E, ? extends T> mapper) {
        List<T> content = page.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}
