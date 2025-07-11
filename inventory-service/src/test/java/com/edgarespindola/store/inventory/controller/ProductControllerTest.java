package com.edgarespindola.store.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgarespindola.store.inventory.dto.ProductDto;
import com.edgarespindola.store.inventory.exception.ProductNotFoundException;
import com.edgarespindola.store.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnAllProducts() throws Exception {
        ProductDto p1 = new ProductDto();
        p1.setId(1L);
        p1.setName("Pan");
        p1.setPrice(10.0);
        p1.setStock(5);
        p1.setUnit("pieza");

        Mockito.when(productService.getAllProducts()).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pan"))
                .andExpect(jsonPath("$[0].stock").value(5));
    }

    @Test
    void shouldCreateValidProduct() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("Leche");
        dto.setPrice(20.0);
        dto.setStock(10);
        dto.setUnit("litro");

        Mockito.when(productService.createProduct(any())).thenReturn(dto);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Leche"));
    }

    @Test
    void shouldFailCreatingInvalidProduct() throws Exception {
        ProductDto dto = new ProductDto(); // nombre vacío y precio en 0
        dto.setPrice(0.0);
        dto.setStock(-1);
        dto.setUnit(""); // inválido

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("El nombre del producto es obligatorio"))
                .andExpect(jsonPath("$.price").value("El precio debe ser mayor a 0"))
                .andExpect(jsonPath("$.stock").value("El stock no puede ser negativo"))
                .andExpect(jsonPath("$.unit").value("La unidad de medida es obligatoria"));
    }

    @Test
    void shouldGetLowStockProducts() throws Exception {
        ProductDto p1 = new ProductDto();
        p1.setId(1L);
        p1.setName("Galletas");
        p1.setPrice(15.0);
        p1.setStock(2);
        p1.setUnit("paquete");

        Mockito.when(productService.getLowStockProducts(5)).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/products/low-stock?threshold=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Galletas"))
                .andExpect(jsonPath("$[0].stock").value(2));
    }

    @Test
    void getProductById_whenExists_shouldReturnProduct() throws Exception {
        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setName("Arroz");
        product.setPrice(1.5);
        product.setStock(30);
        product.setUnit("kg");

        when(productService.getById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Arroz"));
    }

    @Test
    void getProductById_whenNotFound_shouldReturn404() throws Exception {
        when(productService.getById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Producto con ID 99 no encontrado")));
    }

    @Test
    void updateProduct_whenValid_shouldReturnUpdatedProduct() throws Exception {
        ProductDto input = new ProductDto();
        input.setName("Pan");
        input.setPrice(2.0);
        input.setStock(50);
        input.setUnit("pieza");

        ProductDto updated = new ProductDto();
        updated.setId(1L);
        updated.setName("Pan");
        updated.setPrice(2.0);
        updated.setStock(50);
        updated.setUnit("pieza");

        when(productService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Pan"));
    }

    @Test
    void deleteProduct_whenExists_shouldReturn204() throws Exception {
        doNothing().when(productService).delete(1L);

        mockMvc.perform(delete("/api/products/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new ProductNotFoundException(55L)).when(productService).delete(55L);

        mockMvc.perform(delete("/api/products/55"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Producto con ID 55 no encontrado")));
    }
}
