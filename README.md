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
[Spring Initializr](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.5.9&packaging=jar&configurationFileFormat=yaml&jvmVersion=21&groupId=dev.magadiflo&artifactId=product-service&name=product-service&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.magadiflo.product.app&dependencies=lombok,web,data-mongodb,testcontainers,actuator)
con las siguientes dependencias.

````xml
<!--Spring Boot 3.5.9-->
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

## Prueba de Integración con TestContainers

Por defecto, al haber creado la aplicación de spring boot, este viene con esta clase creada con la configuración del
`testcontainers`. En mi caso solo cambié la imagen de mongo a usar para que sea la misma que levanté con mi
docker compose: `mongo:8.2`.

````java

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:8.2"));
    }

}
````

La siguiente clase también se crea automáticamente cuando creamos la aplicación de spring boot.
Aquí lo único que modifiqué o agregué fue:

- RANDOM_PORT
- @Autowired TestRestTemplate
- @Autowired ProductRepository
- @BeforeEach this.productRepository.deleteAll();
- Método @Test

````java

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
````

En conclusión, aquí no tuve que realizar ninguna configuración sobre testcontainer, simplemente usé las clases que
vinieron creadas de antemano y ejecuté las pruebas sin problemas.

````bash
D:\programming\spring\02.youtube\26.programming_techie\microservices-tutorial\business-domain\product-service (main -> origin)
$ mvn test
[INFO] Scanning for projects...
..
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.5.9)

2025-12-17T18:32:26.706-05:00  INFO 9440 --- [product-service] [           main] o.t.utility.ImageNameSubstitutor         : Image name substitution will be performed by: DefaultImageNameSubstitutor (composite of 'ConfigurationFileImageNameSubstitutor' and 'PrefixingImageNameSubstitutor')
2025-12-17T18:32:26.731-05:00  INFO 9440 --- [product-service] [           main] org.testcontainers.DockerClientFactory   : Testcontainers version: 1.21.3
2025-12-17T18:32:26.966-05:00  INFO 9440 --- [product-service] [           main] o.t.d.DockerClientProviderStrategy       : Loaded org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy from ~/.testcontainers.properties, will try it first
2025-12-17T18:32:27.250-05:00  INFO 9440 --- [product-service] [           main] o.t.d.DockerClientProviderStrategy       : Found Docker environment with local Npipe socket (npipe:////./pipe/docker_engine)
2025-12-17T18:32:27.252-05:00  INFO 9440 --- [product-service] [           main] org.testcontainers.DockerClientFactory   : Docker host IP address is localhost
2025-12-17T18:32:27.272-05:00  INFO 9440 --- [product-service] [           main] org.testcontainers.DockerClientFactory   : Connected to docker:
  Server Version: 29.1.3
  API Version: 1.52
  Operating System: Docker Desktop
  Total Memory: 15958 MB
  Labels:
    com.docker.desktop.address=npipe://\\.\pipe\docker_cli
2025-12-17T18:32:27.301-05:00  INFO 9440 --- [product-service] [           main] tc.testcontainers/ryuk:0.12.0            : Creating container for image: testcontainers/ryuk:0.12.0
2025-12-17T18:32:27.547-05:00  INFO 9440 --- [product-service] [           main] tc.testcontainers/ryuk:0.12.0            : Container testcontainers/ryuk:0.12.0 is starting: f113fb080208c88aa16c06cb0d58ad219e04161a7f24c41e72dc222b2a42fa882025-12-17T18:32:27.880-05:00  INFO 9440 --- [product-service] [           main] tc.testcontainers/ryuk:0.12.0            : Container testcontainers/ryuk:0.12.0 started in PT0.5789781S
2025-12-17T18:32:27.890-05:00  INFO 9440 --- [product-service] [           main] o.t.utility.RyukResourceReaper           : Ryuk started - will monitor and terminate Testcontainers containers on JVM exit
2025-12-17T18:32:27.890-05:00  INFO 9440 --- [product-service] [           main] org.testcontainers.DockerClientFactory   : Checking the system...
2025-12-17T18:32:27.891-05:00  INFO 9440 --- [product-service] [           main] org.testcontainers.DockerClientFactory   : ?? Docker server version should be at least 1.6.0
2025-12-17T18:32:27.892-05:00  INFO 9440 --- [product-service] [           main] tc.mongo:8.2                             : Creating container for image: mongo:8.2
2025-12-17T18:32:27.991-05:00  INFO 9440 --- [product-service] [           main] tc.mongo:8.2                             : Container mongo:8.2 is starting: 45103555e93d17978467f3794f013ee31b30026e3d9c938fa0478343c7964bbf
2025-12-17T18:32:28.704-05:00  INFO 9440 --- [product-service] [           main] tc.mongo:8.2                             : Container mongo:8.2 started in PT0.812199S
2025-12-17T18:32:30.453-05:00  INFO 9440 --- [product-service] [           main] org.mongodb.driver.client                : MongoClient with metadata {"driver": {"name": "mongo-java-driver|sync|spring-boot", "version": "5.5.2"}, "os": {"type": "Windows", "name": "Windows 11", "architecture": "amd64", "version": "10.0"}, "platform": "Java/Oracle Corporation/21.0.6+8-LTS-188"} created with settings MongoClientSettings{readPreference=primary, writeConcern=WriteConcern{w=null, wTimeout=null ms, journal=null}, retryWrites=true, retryReads=true, readConcern=ReadConcern{level=null}, credential=null, transportSettings=null, commandListeners=[io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener@7aa63f50], codecRegistry=ProvidersCodecRegistry{codecProviders=[ValueCodecProvider{}, BsonValueCodecProvider{}, DBRefCodecProvider{}, DBObjectCodecProvider{}, DocumentCodecProvider{}, CollectionCodecProvider{}, IterableCodecProvider{}, MapCodecProvider{}, GeoJsonCodecProvider{}, GridFSFileCodecProvider{}, Jsr310CodecProvider{}, JsonObjectCodecProvider{}, BsonCodecProvider{}, EnumCodecProvider{}, com.mongodb.client.model.mql.ExpressionCodecProvider@142918a0, com.mongodb.Jep395RecordCodecProvider@745cf754, com.mongodb.KotlinCodecProvider@25bc65ab]}, loggerSettings=LoggerSettings{maxDocumentLength=1000}, clusterSettings={hosts=[localhost:57422], srvServiceName=mongodb, mode=SINGLE, requiredClusterType=UNKNOWN, requiredReplicaSetName='null', serverSelector='null', clusterListeners='[]', serverSelectionTimeout='30000 ms', localThreshold='15 ms'}, socketSettings=SocketSettings{connectTimeoutMS=10000, readTimeoutMS=0, receiveBufferSize=0, proxySettings=ProxySettings{host=null, port=null, username=null, password=null}}, heartbeatSocketSettings=SocketSettings{connectTimeoutMS=10000, readTimeoutMS=10000, receiveBufferSize=0, proxySettings=ProxySettings{host=null, port=null, username=null, password=null}}, connectionPoolSettings=ConnectionPoolSettings{maxSize=100, minSize=0, maxWaitTimeMS=120000, maxConnectionLifeTimeMS=0, maxConnectionIdleTimeMS=0, maintenanceInitialDelayMS=0, maintenanceFrequencyMS=60000, connectionPoolListeners=[io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener@6eab92f3], maxConnecting=2}, serverSettings=ServerSettings{heartbeatFrequencyMS=10000, minHeartbeatFrequencyMS=500, serverMonitoringMode=AUTO, serverListeners='[]', serverMonitorListeners='[]'}, sslSettings=SslSettings{enabled=false, invalidHostNameAllowed=false, context=null}, applicationName='null', compressorList=[], uuidRepresentation=JAVA_LEGACY, serverApi=null, autoEncryptionSettings=null, dnsClient=null, inetAddressResolver=null, contextProvider=null, timeoutMS=null}
2025-12-17T18:32:30.468-05:00  INFO 9440 --- [product-service] [localhost:57422] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=localhost:57422, type=REPLICA_SET_PRIMARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=27, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=22359400, minRoundTripTimeNanos=0, setName='docker-rs', canonicalAddress=45103555e93d:27017, hosts=[45103555e93d:27017], passives=[], arbiters=[], primary='45103555e93d:27017', tagSet=TagSet{[]}, electionId=7fffffff0000000000000001, setVersion=1, topologyVersion=TopologyVersion{processId=69433d8cc600fa70fd5bdbdd, counter=6}, lastWriteDate=Wed Dec 17 18:32:30 PET 2025, lastUpdateTimeNanos=529336202448099}
2025-12-17T18:32:31.168-05:00  INFO 9440 --- [product-service] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 1 endpoint beneath base path '/actuator'
2025-12-17T18:32:31.383-05:00  INFO 9440 --- [product-service] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 57425 (http) with context path '/'
2025-12-17T18:32:31.398-05:00  INFO 9440 --- [product-service] [           main] d.m.p.a.ProductServiceApplicationTests   : Started ProductServiceApplicationTests in 7.062 seconds (process running for 8.075)
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
..
2025-12-17T18:32:32.233-05:00  INFO 9440 --- [product-service] [o-auto-1-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2025-12-17T18:32:32.233-05:00  INFO 9440 --- [product-service] [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2025-12-17T18:32:32.235-05:00  INFO 9440 --- [product-service] [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 2 ms
2025-12-17T18:32:32.338-05:00  INFO 9440 --- [product-service] [o-auto-1-exec-1] d.m.p.a.service.impl.ProductServiceImpl  : Producto guardado correctamente...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.563 s -- in dev.magadiflo.product.app.ProductServiceApplicationTests
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14.913 s
[INFO] Finished at: 2025-12-17T18:32:33-05:00
[INFO] ------------------------------------------------------------------------ 
````
