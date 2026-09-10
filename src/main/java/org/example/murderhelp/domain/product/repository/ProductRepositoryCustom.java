package org.example.murderhelp.domain.product.repository;

import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductStatus;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepositoryCustom {

    Optional<Product> findProductDetail(Long productId, ProductStatus excludedStatus);

    Page<Product> findProducts(
            String category,
            String subCategory,
            ProductTier tier,
            ProductStatus excludedStatus,
            Pageable pageable
    );

    Page<Product> searchProducts(
            String keyword,
            ProductTier tier,
            ProductStatus excludedStatus,
            Pageable pageable
    );
}
