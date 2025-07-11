package com.edgarespindola.store.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductDto {
    private Long id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String name;
    
    private String description;

    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private double price;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private int stock;

    @NotBlank(message = "La unidad de medida es obligatoria")
    private String unit;
}
