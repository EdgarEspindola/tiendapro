package com.edgarespindola.store.sales.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SaleNotificationDto {
    Long customerId;
    BigDecimal totalAmount;
}
