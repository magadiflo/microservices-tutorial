package dev.magadiflo.product.app.repository;

import dev.magadiflo.product.app.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
