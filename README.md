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
      uri: mongodb://localhost:27017/db_product_service
````

#### 📝 Nota sobre Autenticación MongoDB

La configuración actual de `MongoDB` no requiere autenticación en desarrollo debido al `localhost exception` de
`MongoDB`. Esto permite conexiones sin credenciales desde `localhost` para facilitar el desarrollo.

#### 📝 Comportamiento en Desarrollo

Aunque se definan `MONGO_INITDB_ROOT_USERNAME` y `MONGO_INITDB_ROOT_PASSWORD` en el `compose`, `MongoDB` permite
conexiones locales sin autenticación por defecto. Por eso funciona:

````yml
uri: mongodb://localhost:27017/db_product_service
````

#### 📝 Recomendaciones para producción

- Habilitar `--auth` obligatoriamente.
- Usar variables de entorno para credenciales.
- No exponer el puerto `27017` si `MongoDB` y la app están en la misma red `Docker`.
- Usar nombres de servicio en lugar de `localhost` (ej: `s-mongodb`).
- Para forzar autenticación en producción, modificar el `compose.yml`:
  ````yml
  services:
    s-mongodb:
      # ... resto de la configuración
      command: ["--auth", "--setParameter", "enableLocalhostAuthBypass=false"]
  ````
- Y usar credenciales en `application.yml`:
  ````yml
  spring:
    data:
      mongodb:
        uri: mongodb://root:password@s-mongodb:27017/db_product_service?authSource=admin
  ````
- `..?authSource=admin` o el `authentication-database: admin` es crucial porque `MongoDB` crea el usuario root en la
  base de datos admin por defecto cuando usas `MONGO_INITDB_ROOT_USERNAME`.
