package org.example.murderhelp.domain.product.service;

import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.global.error.BusinessException;
import org.example.murderhelp.global.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ProductTierAuthorityResolver {

    private static final String ROLE_PREFIX = "ROLE_";

    public ProductTier resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return authentication.getAuthorities().stream()
                .map(authority -> toTier(authority.getAuthority()))
                .filter(tier -> tier != null)
                .max(Comparator.comparingInt(ProductTier::getRank))
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "상품 등급 권한이 없습니다."));
    }

    private ProductTier toTier(String authority) {
        if (authority == null) {
            return null;
        }

        // "ROLE_YELLOW", "YELLOW" 둘 다 등급으로 인식한다
        String tierName = authority.startsWith(ROLE_PREFIX)
                ? authority.substring(ROLE_PREFIX.length())
                : authority;

        try {
            return ProductTier.valueOf(tierName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
