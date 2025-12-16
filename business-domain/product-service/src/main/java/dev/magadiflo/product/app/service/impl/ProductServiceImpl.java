package dev.magadiflo.product.app.service.impl;

import dev.magadiflo.product.app.dto.ProductRequest;
import dev.magadiflo.product.app.dto.ProductResponse;
import dev.magadiflo.product.app.mapper.ProductMapper;
import dev.magadiflo.product.app.model.Product;
import dev.magadiflo.product.app.repository.ProductRepository;
import dev.magadiflo.product.app.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponse> getAllProducts() {
        log.info("Obteniendo todos los productos...");
        return this.productRepository.findAll().stream()
                .map(ProductMapper::toProductResponse)
                .toList();
    }

    @Override
    public ProductResponse saveProduct(ProductRequest productRequest) {
        Product product = ProductMapper.toProduct(productRequest);
        this.productRepository.save(product);
        log.info("Producto guardado correctamente...");
        return ProductMapper.toProductResponse(product);
    }

}
