package dev.magadiflo.product.app.dto;

import java.math.BigDecimal;

public record ProductResponse(String name,
                              String description,
                              String skuCode,
                              BigDecimal price) {
}
