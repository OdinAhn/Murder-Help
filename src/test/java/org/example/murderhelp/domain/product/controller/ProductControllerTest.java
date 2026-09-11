package org.example.murderhelp.domain.product.controller;

import com.jayway.jsonpath.JsonPath;
import org.example.murderhelp.domain.product.entity.ProductStatus;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.example.murderhelp.global.config.cache.CacheNames;
import org.example.murderhelp.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private ProductRepository productRepository;

    private String yellowAccessToken;
    private String purpleAccessToken;
    private String redAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        Cache productDetailCache = cacheManager.getCache(CacheNames.PRODUCT_DETAIL);
        if (productDetailCache != null) {
            productDetailCache.clear();
        }
        Cache productListCache = cacheManager.getCache(CacheNames.PRODUCT_LIST);
        if (productListCache != null) {
            productListCache.clear();
        }

        jdbcTemplate.update("delete from cart_items");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from member_spending");
        jdbcTemplate.update("delete from members");
        jdbcTemplate.update("delete from product_specs");
        jdbcTemplate.update("delete from products");
        jdbcTemplate.update("delete from categories");

        String encodedPassword = passwordEncoder.encode("password123");
        insertMember(201L, "yellow@example.com", encodedPassword, "YELLOW");
        insertMember(202L, "purple@example.com", encodedPassword, "PURPLE");
        insertMember(203L, "red@example.com", encodedPassword, "RED");
        yellowAccessToken = login("yellow@example.com");
        purpleAccessToken = login("purple@example.com");
        redAccessToken = login("red@example.com");

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

        insertProductSpec(1001L, 101L, "구성품", "본체 · 탄창 1개", 2);
        insertProductSpec(1002L, 101L, "발사 방식", "가스 블로우백", 1);
    }

    @Test
    void 상품_ID로_상세_정보와_정렬된_제원을_조회한다() throws Exception {
        mockMvc.perform(
                        get("/api/products/101")
                                .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.productCode").value("P001"))
                .andExpect(jsonPath("$.data.name").value("Purple Pistol"))
                .andExpect(jsonPath("$.data.description").value("Purple Pistol description"))
                .andExpect(jsonPath("$.data.category").value("Guns"))
                .andExpect(jsonPath("$.data.subCategory").value("Pistol"))
                .andExpect(jsonPath("$.data.price").value(2_000))
                .andExpect(jsonPath("$.data.stockQuantity").value(10))
                .andExpect(jsonPath("$.data.tier").value("purple"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/P001.jpg"))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.specs.length()").value(2))
                .andExpect(jsonPath("$.data.specs[0].name").value("발사 방식"))
                .andExpect(jsonPath("$.data.specs[0].value").value("가스 블로우백"))
                .andExpect(jsonPath("$.data.specs[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data.specs[1].name").value("구성품"));
    }

    @Test
    void 같은_사용자_티어의_상품_상세_반복_조회는_캐시를_사용한다() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            get("/api/products/101")
                                    .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
                    )
                    .andExpect(status().isOk());
        }

        verify(productRepository, times(1))
                .findProductDetail(101L, ProductStatus.DISCONTINUED);
    }

    @Test
    void 같은_조건의_상품_목록_반복_조회는_캐시를_사용한다() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            get("/api/products")
                                    .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
                                    .param("category", "Guns")
                                    .param("subCategory", "Pistol")
                                    .param("tier", "purple")
                                    .param("sort", "PRICE_ASC")
                                    .param("page", "1")
                                    .param("size", "10")
                    )
                    .andExpect(status().isOk());
        }

        verify(productRepository, times(1))
                .findProducts(any(), any(), any(), any(), any());
    }

    @Test
    void 자기_등급보다_높은_상품의_상세_조회는_거부한다() throws Exception {
        mockMvc.perform(
                        get("/api/products/101")
                                .header(HttpHeaders.AUTHORIZATION, bearer(yellowAccessToken))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("접근할 수 없는 상품 등급입니다."));
    }

    @Test
    void 존재하지_않는_상품의_상세_조회는_404를_반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/products/999")
                                .header(HttpHeaders.AUTHORIZATION, bearer(redAccessToken))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_001"));
    }

    @Test
    void 판매_중단된_상품의_상세_조회는_404를_반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/products/105")
                                .header(HttpHeaders.AUTHORIZATION, bearer(redAccessToken))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_001"));
    }

    @Test
    void 품절_상품의_상세_정보는_조회할_수_있다() throws Exception {
        mockMvc.perform(
                        get("/api/products/106")
                                .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.stockQuantity").value(0));
    }

    @Test
    void 인증하지_않은_사용자는_상품_상세를_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/products/101"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 선택한_카테고리와_등급으로_상품을_필터링하고_페이지로_반환한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
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
                                .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
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
                                .header(HttpHeaders.AUTHORIZATION, bearer(purpleAccessToken))
                                .param("category", "Guns")
                                .param("subCategory", "Pistol")
                                .param("tier", "yellow")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].productCode").value("P002"));
    }

    @Test
    void JWT에_인증된_회원의_등급으로_상품_권한을_검증한다() throws Exception {
        mockMvc.perform(
                        get("/api/products")
                                .header(HttpHeaders.AUTHORIZATION, bearer(redAccessToken))
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
                                .header(HttpHeaders.AUTHORIZATION, bearer(yellowAccessToken))
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
                                .header(HttpHeaders.AUTHORIZATION, bearer(redAccessToken))
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
                                .header(HttpHeaders.AUTHORIZATION, bearer(yellowAccessToken))
                                .param("tier", "yellow")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("카테고리는 필수입니다."));
    }

    @Test
    void JWT의_회원이_존재하지_않으면_인증을_거부한다() throws Exception {
        String unknownMemberToken = jwtProvider.createAccessToken(999L, "unknown@example.com");

        mockMvc.perform(
                        get("/api/products")
                                .header(HttpHeaders.AUTHORIZATION, bearer(unknownMemberToken))
                                .param("category", "Guns")
                                .param("tier", "yellow")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
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

    private void insertMember(Long id, String email, String password, String grade) {
        jdbcTemplate.update(
                """
                        insert into members (
                            id, email, password, name, phone, grade, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                        """,
                id,
                email,
                password,
                grade + " member",
                "010-0000-0000",
                grade
        );
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"password123"}
                                        """.formatted(email))
                )
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
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

    private void insertProductSpec(
            Long id,
            Long productId,
            String name,
            String value,
            int sortOrder
    ) {
        jdbcTemplate.update(
                """
                        insert into product_specs (
                            id, product_id, spec_name, spec_value, sort_order
                        ) values (?, ?, ?, ?, ?)
                        """,
                id,
                productId,
                name,
                value,
                sortOrder
        );
    }
}
