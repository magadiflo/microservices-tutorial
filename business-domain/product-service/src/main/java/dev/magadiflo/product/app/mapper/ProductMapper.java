package dev.magadiflo.product.app.mapper;

import dev.magadiflo.product.app.dto.ProductRequest;
import dev.magadiflo.product.app.dto.ProductResponse;
import dev.magadiflo.product.app.model.Product;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductMapper {

    public static ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getName(),
                product.getDescription(),
                product.getSkuCode(),
                product.getPrice()
        );
    }

    public static Product toProduct(ProductRequest productRequest) {
        return Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .skuCode(productRequest.skuCode())
                .price(productRequest.price())
                .build();
    }

}
