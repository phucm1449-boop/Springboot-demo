package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Payload used to create or update an order")
public class OrderDTO {

    @JsonProperty("user_id")
    @Min(value = 1, message = "User's ID must be > 0")
    @Schema(name = "user_id", example = "1")
    private Long userId;

    @JsonProperty("fullname")
    @Schema(name = "fullname", example = "Nguyen Van A")
    private String fullName;

    @Schema(example = "user@example.com")
    private String email;

    @JsonProperty("phone_number")
    @NotBlank(message = "Phone number is required")
    @Size(min = 5, message = "Phone number must be at least 5 characters")
    @Schema(name = "phone_number", example = "0987654321")
    private String phoneNumber;

    @Schema(example = "123 Main Street")
    private String address;

    @Schema(example = "Please call before delivery")
    private String note;

    @JsonProperty("order_date")
    @Schema(name = "order_date", example = "2026-04-06")
    private LocalDate orderDate;

    @JsonProperty("total_money")
    @Min(value = 0, message = "Total money must be >= 0")
    @Schema(name = "total_money", example = "2599.98")
    private Float totalMoney;

    @JsonProperty("shipping_method")
    @Schema(name = "shipping_method", example = "Express")
    private String shippingMethod;

    @JsonProperty("shipping_address")
    @Schema(name = "shipping_address", example = "456 Warehouse Road")
    private String shippingAddress;

    @JsonProperty("shipping_date")
    @Schema(name = "shipping_date", example = "2026-04-08")
    private LocalDate shippingDate;

    @JsonProperty("payment_method")
    @Schema(name = "payment_method", example = "COD")
    private String paymentMethod;

//    @JsonProperty("cart_items")
//    private List<CartItemDTO> cartItems;
}
