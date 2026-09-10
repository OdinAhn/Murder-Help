package org.example.murderhelp.domain.product.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.entity.ProductStatus;
import org.example.murderhelp.domain.product.entity.ProductTier;
import org.example.murderhelp.domain.product.entity.QCategory;
import org.example.murderhelp.domain.product.entity.QProduct;
import org.example.murderhelp.domain.product.entity.QProductSpec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QProduct product = QProduct.product;
    private final QProductSpec productSpec = QProductSpec.productSpec;
    private final QCategory category = new QCategory("productCategory");
    private final QCategory parentCategory = new QCategory("parentCategory");

    @Override
    public Optional<Product> findProductDetail(Long productId, ProductStatus excludedStatus) {
        return queryFactory
                .selectFrom(product)
                .join(product.category, category).fetchJoin()
                .join(category.parent, parentCategory).fetchJoin()
                .leftJoin(product.specs, productSpec).fetchJoin()
                .where(
                        product.id.eq(productId),
                        product.status.ne(excludedStatus)
                )
                .distinct()
                .fetch()
                .stream()
                .findFirst();
    }

    @Override
    public Page<Product> findProducts(
            String categoryName,
            String subCategoryName,
            ProductTier tier,
            ProductStatus excludedStatus,
            Pageable pageable
    ) {
        List<Product> products = queryFactory
                .selectFrom(product)
                .join(product.category, category).fetchJoin()
                .join(category.parent, parentCategory).fetchJoin()
                .where(
                        parentCategory.name.eq(categoryName),
                        subCategoryEq(subCategoryName),
                        product.tier.eq(tier),
                        product.status.ne(excludedStatus)
                )
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .join(product.category, category)
                .join(category.parent, parentCategory)
                .where(
                        parentCategory.name.eq(categoryName),
                        subCategoryEq(subCategoryName),
                        product.tier.eq(tier),
                        product.status.ne(excludedStatus)
                );

        return PageableExecutionUtils.getPage(
                products,
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count == null ? 0L : count;
                }
        );
    }

    @Override
    public Page<Product> searchProducts(
            String keyword,
            ProductTier tier,
            ProductStatus excludedStatus,
            Pageable pageable
    ) {
        List<Product> products = queryFactory
                .selectFrom(product)
                .join(product.category, category).fetchJoin()
                .join(category.parent, parentCategory).fetchJoin()
                .where(
                        product.name.contains(keyword),
                        product.tier.eq(tier),
                        product.status.ne(excludedStatus)
                )
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .join(product.category, category)
                .join(category.parent, parentCategory)
                .where(
                        product.name.contains(keyword),
                        product.tier.eq(tier),
                        product.status.ne(excludedStatus)
                );

        return PageableExecutionUtils.getPage(
                products,
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count == null ? 0L : count;
                }
        );
    }

    private BooleanExpression subCategoryEq(String subCategoryName) {
        return subCategoryName == null ? null : category.name.eq(subCategoryName);
    }

    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        return sort.stream()
                .map(this::toOrderSpecifier)
                .toArray(OrderSpecifier<?>[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder) {
        com.querydsl.core.types.Order direction = sortOrder.isAscending()
                ? com.querydsl.core.types.Order.ASC
                : com.querydsl.core.types.Order.DESC;

        return switch (sortOrder.getProperty()) {
            case "id" -> new OrderSpecifier<>(direction, product.id);
            case "price" -> new OrderSpecifier<>(direction, product.price);
            case "productCode" -> new OrderSpecifier<>(direction, product.productCode);
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 상품 정렬 필드입니다: " + sortOrder.getProperty()
            );
        };
    }
}
