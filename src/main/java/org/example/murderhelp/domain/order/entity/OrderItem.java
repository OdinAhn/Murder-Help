package org.example.murderhelp.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.global.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Size(max = 255)
    @NotNull
    @Column(name = "product_name", nullable = false)
    private String productName;

    @NotNull
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;


}