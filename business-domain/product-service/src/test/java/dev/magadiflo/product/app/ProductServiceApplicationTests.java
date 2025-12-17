package dev.magadiflo.product.app;

import dev.magadiflo.product.app.dto.ProductRequest;
import dev.magadiflo.product.app.dto.ProductResponse;
import dev.magadiflo.product.app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

    @Autowired
    private TestRestTemplate client;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setup() {
        this.productRepository.deleteAll();
    }

    @Test
    void shouldCreateProduct() {
        // given
        ProductRequest request = new ProductRequest(
                "iPhone 18",
                "iPhone 18 es un Smathphone de Apple",
                "p-001",
                new BigDecimal("2000")
        );

        // when
        ResponseEntity<ProductResponse> response = this.client
                .postForEntity("/api/v1/products", request, ProductResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        ProductResponse body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isNotBlank();
        assertThat(body.price()).isEqualByComparingTo("2000");
        assertThat(body)
                .usingRecursiveComparison()
                .ignoringFields("id", "price")
                .isEqualTo(request);

        assertThat(this.productRepository.findAll()).hasSize(1);
        assertThat(this.productRepository.findById(body.id()))
                .isPresent()
                .hasValueSatisfying(product -> {
                    assertThat(product.getName()).isEqualTo(request.name());
                    assertThat(product.getSkuCode()).isEqualTo(request.skuCode());
                });
    }

}
