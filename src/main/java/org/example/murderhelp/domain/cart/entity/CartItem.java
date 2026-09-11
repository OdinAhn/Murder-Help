package org.example.murderhelp.domain.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.product.entity.Product;
import org.example.murderhelp.global.entity.BaseTimeEntity;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_items_member_product",
                columnNames = {"member_id", "product_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_member")
    )
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_product")
    )
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    private CartItem(Member member, Product product, int quantity) {
        if (member == null) {
            throw new IllegalArgumentException("회원은 필수입니다.");
        }
        if (product == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
        validateQuantity(quantity);
        product.validatePurchasable(quantity);

        this.member = member;
        this.product = product;
        this.quantity = quantity;
    }

    public static CartItem create(Member member, Product product, int quantity) {
        return new CartItem(member, product, quantity);
    }

    public void addQuantity(int quantity) {
        validateQuantity(quantity);
        int totalQuantity = Math.addExact(this.quantity, quantity);
        this.product.validatePurchasable(totalQuantity);
        this.quantity = totalQuantity;
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("장바구니 수량은 1 이상이어야 합니다.");
        }
    }
}
