package org.example.murderhelp.domain.product.controller;

import org.example.murderhelp.domain.product.scheduler.ProductRankingScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductRankingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRankingScheduler productRankingScheduler;

    @Test
    void GREEN_등급은_수동으로_상품_랭킹을_갱신할_수_있다() throws Exception {
        mockMvc.perform(post("/api/admin/product-rankings/refresh")
                        .with(user("green-admin").roles("GREEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(productRankingScheduler).runWeeklyBestUpdate();
    }

    @Test
    void GREEN이_아닌_등급은_수동_랭킹_갱신을_할_수_없다() throws Exception {
        mockMvc.perform(post("/api/admin/product-rankings/refresh")
                        .with(user("yellow-member").roles("YELLOW")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void 인증하지_않은_사용자는_수동_랭킹_갱신을_할_수_없다() throws Exception {
        mockMvc.perform(post("/api/admin/product-rankings/refresh"))
                .andExpect(status().isUnauthorized());
    }
}
