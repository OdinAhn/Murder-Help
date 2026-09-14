package org.example.murderhelp.domain.search.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.murderhelp.domain.search.service.PopularSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private PopularSearchService popularSearchService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchController searchController;

    @Test
    void 입력이_끝난_검색어만_인기_검색어로_기록한다() {
        when(authentication.getName()).thenReturn("member-1");

        searchController.recordPopularSearch(authentication, "권총");

        verify(popularSearchService).recordSearch("member-1", "권총");
    }
}
