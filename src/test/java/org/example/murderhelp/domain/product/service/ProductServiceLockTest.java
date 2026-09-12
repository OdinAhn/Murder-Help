package org.example.murderhelp.domain.product.service;

import jakarta.persistence.LockModeType;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceLockTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상품을_비관적_쓰기_락으로_조회한다() throws NoSuchMethodException {
        Product product = mock(Product.class);
        when(productRepository.findAllByIdInForUpdate(List.of(1L, 2L)))
                .thenReturn(List.of(product));

        List<Product> result = productService.getProducts(List.of(1L, 2L, 1L));

        assertThat(result).containsExactly(product);
        verify(productRepository).findAllByIdInForUpdate(List.of(1L, 2L));

        Method repositoryMethod = ProductRepository.class
                .getMethod("findAllByIdInForUpdate", List.class);
        assertThat(repositoryMethod.getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
