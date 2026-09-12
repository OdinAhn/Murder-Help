package org.example.murderhelp.domain.order.service;

import org.example.murderhelp.domain.cart.dto.CartItemResponse;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mockup service class 입니다.
 * 실 구현이 완료되면 이 클래스는 삭제될 예정입니다.
 */
@Service
public class MockupService {

    private final ProductRepository productRepository;

    public MockupService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 본인 것인지, 요청한 cart id가 전부 있는지 전부 검증 후
     * cart item 조회하여 반환
     */
    public List<CartItemResponse> getCartList(Long memberId, List<Long> cartIds) {
        // mockup...
        return List.of(new CartItemResponse(1L, 1L, 1));
    }

    /**
     * FOR UPDATE 락을 걸고 product 조회
     */
    public List<Product> getProductByIds(List<Long> productIds) {
        return productIds.stream()
                .map(productRepository::getReferenceById)
                .toList();
    }

    public void deleteCartItems(List<Long> cartItemIds) {
        // mockup...
    }

}
