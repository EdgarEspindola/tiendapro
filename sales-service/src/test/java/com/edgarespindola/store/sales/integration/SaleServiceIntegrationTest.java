package com.edgarespindola.store.sales.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.edgarespindola.store.sales.dto.ProductInventoryResponse;
import com.edgarespindola.store.sales.dto.SaleItemRequest;
import com.edgarespindola.store.sales.dto.SaleRequest;
import com.edgarespindola.store.sales.dto.SaleResponse;
import com.edgarespindola.store.sales.repository.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class SaleServiceIntegrationTest {
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

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SaleRepository saleRepository;

    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void setup() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        saleRepository.deleteAll(); // limpia antes de cada prueba
    }

    @Test
    void shouldCreateSaleAndDiscountStock() throws Exception {
        // 1. Preparar respuesta GET /products/{id}
        ProductInventoryResponse product = new ProductInventoryResponse();
        product.setId(1L);
        product.setName("Producto Test");
        product.setStock(10);
        product.setPrice(BigDecimal.valueOf(150.00));

        String getProductJson = objectMapper.writeValueAsString(product);

        mockServer.expect(once(), requestTo("http://localhost:8081/api/products/1"))
                .andRespond(withSuccess(getProductJson, MediaType.APPLICATION_JSON));

        // 2. Preparar respuesta PUT /products/1/stock/decrease
        mockServer.expect(once(), requestTo("http://localhost:8081/api/products/1/stock/decrease"))
                .andRespond(withSuccess());

        // 3. Crear solicitud de venta
        SaleItemRequest item = new SaleItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        SaleRequest request = new SaleRequest();
        request.setItems(List.of(item));

        // 4. Enviar POST /sales
        ResponseEntity<SaleResponse> response = testRestTemplate.postForEntity("/api/sales", request, SaleResponse.class);

        // 5. Verificaciones
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SaleResponse saleResponse = response.getBody();
        assertThat(saleResponse).isNotNull();
        assertThat(saleResponse.getSaleId()).isNotNull();
        assertThat(saleResponse.getTotalAmount()).isEqualByComparingTo("300.00");

        assertThat(saleRepository.findAll()).hasSize(1);
        mockServer.verify(); // verifica que se llamaron los 2 endpoints mockeados
    }
}
