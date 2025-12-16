package dev.magadiflo.product.app.service;

import dev.magadiflo.product.app.dto.ProductRequest;
import dev.magadiflo.product.app.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
    ProductResponse saveProduct(ProductRequest productRequest);
}
