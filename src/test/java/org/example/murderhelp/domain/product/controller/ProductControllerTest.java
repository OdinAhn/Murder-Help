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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class ProductControllerTest {

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
        insertCategory(12L, "Revolver", 1L);
        insertCategory(2L, "Weapons", null);
        insertCategory(21L, "Knife", 2L);

        insertProduct(101L, "P001", 11L, "Purple Pistol", 2_000L, "purple", "ON_SALE");
        insertProduct(102L, "P002", 11L, "Yellow Pistol", 900L, "yellow", "ON_SALE");
        insertProduct(103L, "P003", 11L, "Red Pistol", 4_000L, "red", "ON_SALE");
        insertProduct(104L, "P004", 12L, "Purple Revolver", 2_200L, "purple", "ON_SALE");
        insertProduct(105L, "P005", 11L, "Discontinued Purple Pistol", 1_100L, "purple", "DISCONTINUED");
        insertProduct(106L, "P006", 11L, "Sold Out Purple Pistol", 1_500L, "purple", "SOLD_OUT");
        insertProduct(107L, "P007", 21L, "Purple Knife", 1_700L, "purple", "ON_SALE");
    }

    @Test
    void 선택한_카테고리와_등급으로_상품을_필터링하고_페이지로_반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("purple-member").authorities(() -> "PURPLE"))
                                .param("category", "Guns")
                                .param("subCategory", "Pistol")
                                .param("tier", "purple")
                                .param("sort", "PRICE_ASC")
                                .param("page", "1")
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productCode").value("P006"))
                .andExpect(jsonPath("$.data.items[0].description").value("Sold Out Purple Pistol description"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://example.com/P006.jpg"))
                .andExpect(jsonPath("$.data.items[0].category").value("Guns"))
                .andExpect(jsonPath("$.data.items[0].subCategory").value("Pistol"))
                .andExpect(jsonPath("$.data.items[0].tier").value("purple"))
                .andExpect(jsonPath("$.data.items[0].status").value("SOLD_OUT"));
    }

    @Test
    void 서브카테고리를_생략하면_대분류의_모든_하위_카테고리를_조회한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("purple-member").authorities(() -> "PURPLE"))
                                .param("category", "Guns")
                                .param("tier", "purple")
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    @Test
    void 자기_등급보다_낮은_등급의_상품을_조회할_수_있다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("purple-member").authorities(() -> "PURPLE"))
                                .param("category", "Guns")
                                .param("subCategory", "Pistol")
                                .param("tier", "yellow")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].productCode").value("P002"));
    }

    @Test
    void 로컬_프로필에서는_HTTP_헤더로_상품_등급_권한을_검증할_수_있다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .header("X-Product-Tier", "RED")
                                .param("category", "Guns")
                                .param("subCategory", "Pistol")
                                .param("tier", "yellow")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].tier").value("yellow"));
    }

    @Test
    void 자기_등급보다_높은_등급을_요청하면_접근을_거부한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("yellow-member").authorities(() -> "YELLOW"))
                                .param("category", "Guns")
                                .param("tier", "red")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("접근할 수 없는 상품 등급입니다."));
    }

    @Test
    void 지원하지_않는_상품_등급을_요청하면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("red-member").authorities(() -> "RED"))
                                .param("category", "Guns")
                                .param("tier", "blue")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void 카테고리를_입력하지_않으면_잘못된_요청으로_응답한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("yellow-member").authorities(() -> "YELLOW"))
                                .param("tier", "yellow")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("카테고리는 필수입니다."));
    }

    @Test
    void 로그인했어도_상품_등급_권한이_없으면_접근을_거부한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .with(user("member"))
                                .param("category", "Guns")
                                .param("tier", "yellow")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("상품 등급 권한이 없습니다."));
    }

    @Test
    void 인증하지_않은_사용자는_상품을_조회할_수_없다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .param("category", "Guns")
                                .param("tier", "yellow")
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
