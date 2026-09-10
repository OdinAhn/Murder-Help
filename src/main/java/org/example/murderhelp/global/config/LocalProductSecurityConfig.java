package org.example.murderhelp.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 로그인 기능이 완성되기 전 로컬 HTTP 요청으로 상품 API를 확인하기 위한 설정이다.
 * local 프로필에서만 X-Product-Tier 값을 상품 등급 권한으로 변환한다.
 */
@Configuration
@Profile("local")
public class LocalProductSecurityConfig {

    static final String PRODUCT_TIER_HEADER = "X-Product-Tier";

    @Bean
    @Order(1)
    SecurityFilterChain localProductSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/products/**", "/api/v1/products/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .addFilterBefore(new LocalProductTierAuthenticationFilter(), AnonymousAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    private static class LocalProductTierAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            String tierHeader = request.getHeader(PRODUCT_TIER_HEADER);

            if (tierHeader != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(tierHeader);
            }

            filterChain.doFilter(request, response);
        }

        private void authenticate(String tierHeader) {
            try {
                ProductTier tier = ProductTier.valueOf(tierHeader.trim().toUpperCase(Locale.ROOT));
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                "local-http-client",
                                null,
                                List.of(new SimpleGrantedAuthority(tier.name()))
                        );

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 등급은 인증하지 않고 SecurityFilterChain이 401로 처리하게 둔다.
            }
        }
    }
}
