# [Spring Boot Microservices Tutorial](https://www.youtube.com/playlist?list=PLSVW22jAG8pDeU80nDzbUgr8qqzEMppi8)

- Tutorial tomado del canal de youtube `Programming Techie`.
- Repositorio del tutor
  [spring-boot-3-microservices-course](https://github.com/SaiUpadhyayula/spring-boot-3-microservices-course?tab=readme-ov-file).

---

## Creando Docker Compose

````yml
services:
  s-mongodb:
    image: mongo:8.2
    container_name: c-mongodb
    restart: unless-stopped
    ports:
      - '27017:27017'
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: password
      MONGO_INITDB_DATABASE: db_product_service
    volumes:
      - mongo-data:/data/db
    networks:
      - microservices-tutorial-net

volumes:
  mongo-data:
    name: mongo-data

networks:
  microservices-tutorial-net:
    name: microservices-tutorial-net
````

## Creando Product Service

Creamos el proyecto desde
[Spring Initializr](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.5.8&packaging=jar&configurationFileFormat=yaml&jvmVersion=21&groupId=dev.magadiflo&artifactId=product-service&name=product-service&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.magadiflo.product.app&dependencies=lombok,web,data-mongodb,testcontainers,actuator)
con las siguientes dependencias.

````xml
<!--Spring Boot 3.5.8-->
<!--Java 21-->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mongodb</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
````

## Configurando product service

````yml
server:
  port: 8081
  error:
    include-message: always

spring:
  application:
    name: product-service
  data:
    mongodb:
      uri: mongodb://root:password@localhost:27017/db_product_service?authSource=admin
````

### 📝 Notas Importantes sobre la Configuración

1. `Credenciales obligatorias`. Como en el `compose.yml` definimos el servicio de `MongoDB` con `username` y `password`,
   es necesario incluir estas credenciales en la URI de conexión del `application.yml`. Sin ellas, la aplicación
   levantará sin problemas, pero al intentar interactuar con la base de datos (consultas, inserciones, etc.)
   obtendremos un error de autenticación.
2. El parámetro `authSource=admin`. Es crucial agregar `?authSource=admin` en la URI
   (o `authentication-database: admin` en formato extendido) porque `MongoDB` crea el usuario `root` en la base de
   datos `admin` por defecto cuando usamos `MONGO_INITDB_ROOT_USERNAME`. Sin este parámetro, `MongoDB` buscará el
   usuario en la base de datos `db_product_service` y fallará la autenticación.
3. Formato alternativo de configuración. También puedes usar el `formato extendido`:
   ````yml
   spring:
     data:
       mongodb:
         host: localhost
         port: 27017
         database: db_product_service
         username: root
         password: password
         authentication-database: admin
   ````

## Agregando clases del modelo document y repository

Como primer paso, crearemos la colección a productos y su interface de repositorio:

````java

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private String description;
    private String skuCode;
    private BigDecimal price;
}
````

````java
public interface ProductRepository extends MongoRepository<Product, String> {
}
````

## Creando DTO y Mapper

````java
public record ProductRequest(String name,
                             String description,
                             String skuCode,
                             BigDecimal price) {
}
````

````java
public record ProductResponse(String id,
                              String name,
                              String description,
                              String skuCode,
                              BigDecimal price) {
}
````

````java

@UtilityClass
public class ProductMapper {

    public static ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
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
````

## Creando clase de servicio

````java
public interface ProductService {
    List<ProductResponse> getAllProducts();

    ProductResponse saveProduct(ProductRequest productRequest);
}
````

````java

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
````

## Creando controlador ProductController

````java

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(this.productService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<ProductResponse> saveProduct(@RequestBody ProductRequest productRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.productService.saveProduct(productRequest));
    }

}
````

## Probando endpoints del ProductController

### Crear producto

````bash
$ curl -v -X POST -H "Content-type: application/json" -d "{\"name\": \"iPhone 15\", \"description\":\"iPhone 15 es un smartphone de Apple\", \"skuCode\":\"p-001\", \"price\": 1000}" http://localhost:8081/api/v1/products | jq
>
< HTTP/1.1 201
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Tue, 16 Dec 2025 22:26:24 GMT
<
{
  "id": "6941dc9085c96dcc5b1972f2",
  "name": "iPhone 15",
  "description": "iPhone 15 es un smartphone de Apple",
  "skuCode": "p-001",
  "price": 1000
}
````

### Listar productos

````bash
$ curl -v http://localhost:8081/api/v1/products | jq
>
< HTTP/1.1 200
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Tue, 16 Dec 2025 22:27:04 GMT
<
* Connection #0 to host localhost:8081 left intact
[
  {
    "id": "6941dc9085c96dcc5b1972f2",
    "name": "iPhone 15",
    "description": "iPhone 15 es un smartphone de Apple",
    "skuCode": "p-001",
    "price": 1000
  }
]
````
