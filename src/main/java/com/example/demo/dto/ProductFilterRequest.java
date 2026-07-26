package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductFilterRequest {
    @Min(value = 0, message = "page must be >= 0")
    private Integer page;

    @Min(value = 1, message = "limit must be >= 1")
    private Integer limit;
}
