package dev.magadiflo.product.app.dto;

import java.math.BigDecimal;

public record ProductRequest(String name,
                             String description,
                             String skuCode,
                             BigDecimal price) {
}
