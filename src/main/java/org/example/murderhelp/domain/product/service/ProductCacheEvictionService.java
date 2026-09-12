package org.example.murderhelp.domain.product.service;

import org.example.murderhelp.global.config.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ProductCacheEvictionService {

    @CacheEvict(
            cacheNames = {
                    CacheNames.PRODUCT_DETAIL,
                    CacheNames.PRODUCT_LIST,
                    CacheNames.PRODUCT_SEARCH
            },
            allEntries = true
    )
    public void evictProductCaches() {
    }
}
