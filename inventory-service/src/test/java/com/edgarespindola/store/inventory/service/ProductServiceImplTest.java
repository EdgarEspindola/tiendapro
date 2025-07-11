package com.edgarespindola.store.inventory.service;

import com.edgarespindola.store.inventory.dto.ProductDto;
import com.edgarespindola.store.inventory.entity.Product;
import com.edgarespindola.store.inventory.exception.InvalidProductDataException;
import com.edgarespindola.store.inventory.exception.ProductNotFoundException;
import com.edgarespindola.store.inventory.mapper.ProductMapper;
import com.edgarespindola.store.inventory.repository.ProductRepository;
import com.edgarespindola.store.inventory.service.impl.ProductServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
public class ProductServiceImplTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("inventory_test")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductRepository repository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        ProductMapper mapper = Mappers.getMapper(ProductMapper.class);
        service = new ProductServiceImpl(repository, mapper);
    }

    @Test
    void shouldCreateAndFetchProduct() {
        ProductDto dto = new ProductDto();
        dto.setName("Café");
        dto.setPrice(40.0);
        dto.setStock(20);
        dto.setUnit("bolsa");

        ProductDto saved = service.createProduct(dto);
        assertThat(saved.getId()).isNotNull();

        ProductDto fetched = service.getById(saved.getId());
        assertThat(fetched.getName()).isEqualTo("Café");
    }

    @Test
    void shouldReturnLowStockProducts() {
        ProductDto p1 = new ProductDto();
        p1.setName("Sal");
        p1.setPrice(5.0);
        p1.setStock(2);
        p1.setUnit("kg");

        ProductDto p2 = new ProductDto();
        p2.setName("Azúcar");
        p2.setPrice(8.0);
        p2.setStock(15);
        p2.setUnit("kg");

        service.createProduct(p1);
        service.createProduct(p2);

        List<ProductDto> lowStock = service.getLowStockProducts(5);
        assertThat(lowStock).hasSize(1);
        assertThat(lowStock.get(0).getName()).isEqualTo("Sal");
    }

    @Test
    void shouldFailWhenPriceIsZero() {
        ProductDto dto = new ProductDto();
        dto.setName("Aceite");
        dto.setPrice(0.0); // ❌ inválido
        dto.setStock(10);
        dto.setUnit("litro");

        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidProductDataException.class,
                () -> service.createProduct(dto),
                "El precio debe ser mayor a 0");
    }

    @Test
    void shouldFailWhenStockIsNegative() {
        ProductDto dto = new ProductDto();
        dto.setName("Huevos");
        dto.setPrice(25.0);
        dto.setStock(-5); // ❌ inválido
        dto.setUnit("docena");

        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidProductDataException.class,
                () -> service.createProduct(dto),
                "El stock no puede ser negativo");
    }

    @Test
    void shouldUpdateExistingProductSuccessfully() {
        // Arrange
        Product product = Product
                .builder()
                .name("Azúcar")
                .description("Azúcar refinada")
                .price(25.0)
                .stock(100)
                .unit("kg")
                .build();

        product = repository.save(product);

        ProductDto updateDto = new ProductDto();
        updateDto.setName("Azúcar Morena");
        updateDto.setDescription("Azúcar orgánica");
        updateDto.setPrice(28.5);
        updateDto.setStock(90);
        updateDto.setUnit("kg");

        // Act
        ProductDto result = service.update(product.getId(), updateDto);

        // Assert
        assertEquals("Azúcar Morena", result.getName());
        assertEquals(28.5, result.getPrice());
        assertEquals(90, result.getStock());
        assertEquals("kg", result.getUnit());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentProduct() {
        ProductDto dto = new ProductDto();
        dto.setName("Producto X");
        dto.setPrice(15.0);
        dto.setStock(20);
        dto.setUnit("kg");

        ProductNotFoundException ex = assertThrows(
                ProductNotFoundException.class,
                () -> service.update(999L, dto), "Producto con ID 999 no encontrado");

    }

    @Test
    void shouldFailToUpdateProductWithInvalidPrice() {
        Product product = repository.save(
                Product
                        .builder()
                        .name("Café")
                        .description("Café molido")
                        .price(35.0)
                        .stock(50)
                        .unit("bolsa")
                        .build());

        ProductDto dto = new ProductDto();
        dto.setName("Café Premium");
        dto.setPrice(0.0); // ❌ inválido
        dto.setStock(30);
        dto.setUnit("bolsa");

        InvalidProductDataException ex = assertThrows(
                InvalidProductDataException.class,
                () -> service.update(product.getId(), dto), "El precio debe ser mayor a 0");

    }

    @Test
    void shouldFailToUpdateProductWithNegativeStock() {
        Product product = repository.save(new Product(null, "Harina", "Harina integral", 18.0, 60, "kg"));

        ProductDto dto = new ProductDto();
        dto.setName("Harina orgánica");
        dto.setPrice(20.0);
        dto.setStock(-5); // ❌ inválido
        dto.setUnit("kg");

        InvalidProductDataException ex = assertThrows(
                InvalidProductDataException.class,
                () -> service.update(product.getId(), dto), "El stock no puede ser negativo");

    }

    @Test
    void deleteProduct_whenExists_shouldDelete() {
        ProductDto dto = new ProductDto();
        dto.setName("Producto a eliminar");
        dto.setPrice(100.0);
        dto.setStock(10);
        dto.setUnit("kg");

        ProductDto saved = service.createProduct(dto);
        Long id = saved.getId();

        // Confirmar que existe
        assertThat(service.getById(id)).isNotNull();

        // Ejecutar delete
        service.delete(id);

        // Confirmar que ya no existe
        assertThrows(ProductNotFoundException.class, () -> service.getById(id));
    }

    @Test
    void deleteProduct_whenNotExists_shouldThrow() {
        Long nonexistentId = 999L;
        assertThrows(ProductNotFoundException.class, () -> service.delete(nonexistentId));
    }

}
