package org.example.murderhelp.domain.product.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-search-v2-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class ProductSearchV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from product_specs");
        jdbcTemplate.update("delete from products");
        jdbcTemplate.update("delete from categories");

        insertCategory(1L, "Guns", null);
        insertCategory(11L, "Pistol", 1L);

        insertProduct(101L, "P001", 11L, "Purple Pistol", 2_000L, "purple", "ON_SALE");
        insertProduct(102L, "P002", 11L, "Yellow Pistol", 900L, "yellow", "ON_SALE");
        insertProduct(103L, "P003", 11L, "Red Pistol", 4_000L, "red", "ON_SALE");
        insertProduct(105L, "P005", 11L, "Discontinued Purple Pistol", 1_100L, "purple", "DISCONTINUED");
    }

    @Test
    void v1과_동일한_요청이면_v2도_같은_검색_결과를_반환한다() throws Exception {
        MvcResult v1Result = mockMvc.perform(
                        get("/api/v1/products/search")
                                .with(user("purple-member").authorities(() -> "PURPLE"))
                                .param("keyword", "Pistol")
                                .param("tier", "purple")
                                .param("sort", "PRICE_ASC")
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andReturn();

        MvcResult v2Result = mockMvc.perform(
                        get("/api/v2/products/search")
                                .with(user("purple-member").authorities(() -> "PURPLE"))
                                .param("keyword", "Pistol")
                                .param("tier", "purple")
                                .param("sort", "PRICE_ASC")
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andReturn();

        assertThat(v2Result.getResponse().getContentAsString())
                .isEqualTo(v1Result.getResponse().getContentAsString());
    }

    @Test
    void 같은_v2_요청을_반복해도_같은_결과를_반환한다() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            get("/api/v2/products/search")
                                    .with(user("purple-member").authorities(() -> "PURPLE"))
                                    .param("keyword", "Pistol")
                                    .param("tier", "purple")
                                    .param("page", "1")
                                    .param("size", "10")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].productCode").value("P001"));
        }
    }

    @Test
    void 자기_등급보다_높은_등급을_요청하면_접근을_거부한다() throws Exception {
        mockMvc.perform(
                        get("/api/v2/products/search")
                                .with(user("yellow-member").authorities(() -> "YELLOW"))
                                .param("keyword", "Pistol")
                                .param("tier", "red")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void 검색어를_입력하지_않으면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(
                        get("/api/v2/products/search")
                                .with(user("purple-member").authorities(() -> "PURPLE"))
                                .param("tier", "purple")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("검색어는 필수입니다."));
    }

    @Test
    void 인증하지_않은_사용자는_상품을_검색할_수_없다() throws Exception {
        mockMvc.perform(
                        get("/api/v2/products/search")
                                .param("keyword", "Pistol")
                                .param("tier", "purple")
                )
                .andExpect(status().isUnauthorized());
    }

    private void insertCategory(Long id, String name, Long parentId) {
        jdbcTemplate.update(
                "insert into categories (id, name, parent_id) values (?, ?, ?)",
                id,
                name,
                parentId
        );
    }

    private void insertProduct(
            Long id,
            String productCode,
            Long categoryId,
            String name,
            long price,
            String tier,
            String status
    ) {
        jdbcTemplate.update(
                """
                        insert into products (
                            id, product_code, category_id, name, description, image_url,
                            price, stock_quantity, tier, status
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                productCode,
                categoryId,
                name,
                name + " description",
                "https://example.com/" + productCode + ".jpg",
                price,
                status.equals("SOLD_OUT") ? 0 : 10,
                tier,
                status
        );
    }
}
