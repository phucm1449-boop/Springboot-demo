package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload used to create or update a product")
public class ProductDTO {
    @NotBlank(message = "Title is required")
//    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 character")
    @Size(min = 3, max = 200, message = "VALIDATE_NAME_OF_PRODUCT")
    @Schema(example = "iPhone 15 Pro")
    private String name;

    @Min(value = 0, message = "Price must be greater than or equal to 0")
    @Max(value = 10000000, message = "Price must be less than or equal to 10,000,000")
    @Schema(example = "1299.99")
    private Float price;
    @Schema(example = "iphone15pro.jpg")
    private String thumbnail;
    @Schema(example = "256GB titanium smartphone")
    private String description;

    @JsonProperty("category_id")
    @Schema(name = "category_id", example = "1")
    private Long categoryId;

}
