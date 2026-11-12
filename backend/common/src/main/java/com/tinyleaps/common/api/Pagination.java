package com.tinyleaps.common.api;

/** Pagination block for endpoints returning paged data. {@code page} is zero-based. */
public record Pagination(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {

    public static Pagination of(int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new Pagination(page, size, totalElements, totalPages, page + 1 < totalPages, page > 0);
    }
}
