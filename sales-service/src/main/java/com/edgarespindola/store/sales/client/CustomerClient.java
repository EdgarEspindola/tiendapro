package com.edgarespindola.store.sales.client;

import com.edgarespindola.store.sales.dto.CustomerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerClient {
    private final RestTemplate restTemplate;

    private static final String CUSTOMER_BASE_URL = "http://localhost:8083/api/customers/";

    @CircuitBreaker(name = "customerService", fallbackMethod = "fallbackValidateCustomer")
    @Retry(name = "customerService")
    public boolean validateCustomerExists(Long customerId) {
        String url = CUSTOMER_BASE_URL + customerId;
        try {
            restTemplate.getForObject(url, CustomerResponse.class);
            log.info("✅ Cliente {} validado correctamente", customerId);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Cliente no encontrado con ID: " + customerId);
        }
    }

    // ⚠️ Este método solo se ejecuta si hay error técnico, no si es 404
    private boolean fallbackValidateCustomer(Long customerId, Throwable ex) {
        log.error("⚠️ Fallback activado al validar cliente {}: {}", customerId, ex.toString());
        throw new IllegalStateException("No se pudo validar el cliente. Servicio de clientes no disponible.");
    }
}
