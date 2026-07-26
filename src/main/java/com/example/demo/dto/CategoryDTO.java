package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Data//toString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload used to create or update a category")
public class CategoryDTO {

    @NotEmpty(message = "Category's name cannot be empty")
    @Schema(example = "Electronics")
    private String name;
}
