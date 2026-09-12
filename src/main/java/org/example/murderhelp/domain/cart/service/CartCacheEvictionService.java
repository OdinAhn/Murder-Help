package org.example.murderhelp.domain.cart.service;

import org.example.murderhelp.global.config.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class CartCacheEvictionService {

    @CacheEvict(cacheNames = CacheNames.CART_ITEMS, key = "'member:' + #memberId")
    public void evictCartItems(Long memberId) {
    }
}
