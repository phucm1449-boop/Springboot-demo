package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Payload used to create or update an order line item")
public class OrderDetailDTO {

    @JsonProperty("order_id")
    @Min(value=1, message = "Order's ID must be > 0")
    @Schema(name = "order_id", example = "10")
    private Integer orderId;

    @Min(value=1, message = "Product's ID must be > 0")
    @JsonProperty("product_id")
    @Schema(name = "product_id", example = "5")
    private Integer productId;

    @Min(value=0, message = "Product's ID must be >= 0")
    @Schema(example = "1299.99")
    private Float price;

    @Min(value=1, message = "number_of_products must be >= 1")
    @JsonProperty("number_of_products")
    @Schema(name = "number_of_products", example = "2")
    private int numberOfProducts;

    @Min(value=0, message = "total_money must be >= 0")
    @JsonProperty("total_money")
    @Schema(name = "total_money", example = "2599.98")
    private Float totalMoney;

    @Schema(example = "Black")
    private String color;
}
