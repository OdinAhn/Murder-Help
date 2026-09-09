package org.example.murderhelp.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductTier {

    YELLOW("yellow"),
    PURPLE("purple"),
    RED("red"),
    GREEN("green");


    private final String value;

    public static ProductTier fromValue(String value) {
        return switch (value) {
            case "yellow" -> YELLOW;
            case "purple" -> PURPLE;
            case "red" -> RED;
            case "green" -> GREEN;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 상품 등급입니다: " + value
            );
        };
    }
}