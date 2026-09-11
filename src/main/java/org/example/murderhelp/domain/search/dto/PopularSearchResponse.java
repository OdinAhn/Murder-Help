package org.example.murderhelp.domain.search.dto;

public record PopularSearchResponse(
        int rank,
        String keyword,
        long score
) {
}
