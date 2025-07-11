package com.edgarespindola.store.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgarespindola.store.inventory.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class ProductIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldCreateAndGetProductSuccessfully() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("Café");
        dto.setPrice(5.0);
        dto.setStock(20);
        dto.setUnit("paquete");

        String body = objectMapper.writeValueAsString(dto);

        // POST: Crear producto
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        // Obtener el ID del producto creado
        ProductDto saved = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProductDto.class
        );

        // GET: Recuperar producto por ID
        mockMvc.perform(get("/api/products/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Café"));
    }

    @Test
    void shouldFailValidationWhenPriceIsZero() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("Producto inválido");
        dto.setPrice(0.0);
        dto.setStock(5);
        dto.setUnit("unidad");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").value("El precio debe ser mayor a 0"));
    }

    @Test
    void shouldFailValidationWhenStockIsNegative() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("Producto inválido");
        dto.setPrice(10.0);
        dto.setStock(-5);
        dto.setUnit("unidad");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stock").value("El stock no puede ser negativo"));
    }

    @Test
    void shouldDeleteProductSuccessfully() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("BorrarTest");
        dto.setPrice(3.5);
        dto.setStock(8);
        dto.setUnit("pieza");

        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ProductDto saved = objectMapper.readValue(response, ProductDto.class);

        mockMvc.perform(delete("/api/products/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateProductSuccessfully() throws Exception {
        // Crear producto original
        ProductDto dto = new ProductDto();
        dto.setName("Original");
        dto.setPrice(20.0);
        dto.setStock(10);
        dto.setUnit("pieza");

        String createResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ProductDto created = objectMapper.readValue(createResponse, ProductDto.class);

        // Actualizar producto
        ProductDto updateDto = new ProductDto();
        updateDto.setId(created.getId());
        updateDto.setName("Actualizado");
        updateDto.setPrice(99.9);
        updateDto.setStock(50);
        updateDto.setUnit("paquete");

        mockMvc.perform(put("/api/products/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Actualizado"))
                .andExpect(jsonPath("$.price").value(99.9))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.unit").value("paquete"));
    }

@Test
void shouldReturn404WhenProductNotFound() throws Exception {
        String response = mockMvc.perform(get("/api/products/99999"))
                        .andExpect(status().isNotFound())
                        .andReturn().getResponse()
                        .getContentAsString();

        // Verifica que el cuerpo de la respuesta sea igual al mensaje esperado
        org.assertj.core.api.Assertions.assertThat(response)
                        .isEqualTo("Producto con ID 99999 no encontrado");
}

    @Test
    void shouldReturnProductsWithLowStock() throws Exception {
        // Crear producto con stock bajo
        ProductDto lowStockProduct = new ProductDto();
        lowStockProduct.setName("Producto con poco stock");
        lowStockProduct.setPrice(12.0);
        lowStockProduct.setStock(3);
        lowStockProduct.setUnit("pieza");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowStockProduct)))
                .andExpect(status().isOk());

        // Crear producto con stock alto
        ProductDto highStockProduct = new ProductDto();
        highStockProduct.setName("Producto con mucho stock");
        highStockProduct.setPrice(15.0);
        highStockProduct.setStock(50);
        highStockProduct.setUnit("caja");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highStockProduct)))
                .andExpect(status().isOk());

        // Consultar productos con stock bajo usando threshold = 10
        mockMvc.perform(get("/api/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Producto con poco stock"));
    }
}
