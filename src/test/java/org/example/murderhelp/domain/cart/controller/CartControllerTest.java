package org.example.murderhelp.domain.cart.controller;

import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cart-controller-test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from cart_items");
        jdbcTemplate.update("delete from product_specs");
        jdbcTemplate.update("delete from products");
        jdbcTemplate.update("delete from categories");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from member_spending");
        jdbcTemplate.update("delete from members");

        Member member = memberRepository.saveAndFlush(
                Member.builder()
                        .email("cart-member@example.com")
                        .password("encoded-password")
                        .name("장바구니 회원")
                        .phone("010-1234-5678")
                        .build()
        );
        memberId = member.getId();

        insertCategory(1L, "Guns", null);
        insertCategory(11L, "Pistol", 1L);
        insertProduct(101L, "P001", 11L, "Yellow Pistol", 10, "yellow", "ON_SALE");
    }

    @Test
    void 로그인한_회원이_상품을_장바구니에_담는다() throws Exception {
        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 2}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.productId").value(101))
                .andExpect(jsonPath("$.data.quantity").value(2));

        Integer quantity = jdbcTemplate.queryForObject(
                "select quantity from cart_items where member_id = ? and product_id = ?",
                Integer.class,
                memberId,
                101L
        );
        assertThat(quantity).isEqualTo(2);
    }

    @Test
    void 같은_상품을_다시_담으면_새_행을_만들지_않고_수량을_증가시킨다() throws Exception {
        addItem(memberId, 101L, 2);

        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 3}
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(5));

        Integer itemCount = jdbcTemplate.queryForObject(
                "select count(*) from cart_items where member_id = ? and product_id = ?",
                Integer.class,
                memberId,
                101L
        );
        assertThat(itemCount).isEqualTo(1);
    }

    @Test
    void 수량이_1보다_작으면_장바구니에_담을_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 0}
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.message").value("장바구니 수량은 1 이상이어야 합니다."));
    }

    @Test
    void 존재하지_않는_상품은_장바구니에_담을_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 999, "quantity": 1}
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_001"));
    }

    @Test
    void 판매중이_아닌_상품은_장바구니에_담을_수_없다() throws Exception {
        jdbcTemplate.update("update products set status = 'SOLD_OUT', stock_quantity = 0 where id = 101");

        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 1}
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_005"));
    }

    @Test
    void 기존_수량과_추가_수량의_합이_재고를_초과하면_담을_수_없다() throws Exception {
        addItem(memberId, 101L, 8);

        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 3}
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_002"));
    }

    @Test
    void 회원_등급보다_높은_상품은_장바구니에_담을_수_없다() throws Exception {
        jdbcTemplate.update("update products set tier = 'purple' where id = 101");

        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(memberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 1}
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("접근할 수 없는 상품 등급입니다."));
    }

    @Test
    void 인증하지_않은_사용자는_장바구니에_담을_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/cart/items")
                                .contentType("application/json")
                                .content("""
                                        {"productId": 101, "quantity": 1}
                                        """)
                )
                .andExpect(status().isUnauthorized());
    }

    private void addItem(Long targetMemberId, Long productId, int quantity) throws Exception {
        mockMvc.perform(
                        post("/api/cart/items")
                                .with(authenticatedMember(targetMemberId))
                                .contentType("application/json")
                                .content("""
                                        {"productId": %d, "quantity": %d}
                                        """.formatted(productId, quantity))
                )
                .andExpect(status().isOk());
    }

    private RequestPostProcessor authenticatedMember(Long targetMemberId) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        targetMemberId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_YELLOW"))
                );
        return authentication(authentication);
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
            int stockQuantity,
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
                1_000L,
                stockQuantity,
                tier,
                status
        );
    }
}
