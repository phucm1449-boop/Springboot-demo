package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Credentials used to authenticate a user")
public class UserLoginDTO {
    @JsonProperty("phone_number")
    @NotBlank(message = "Phone number is required")
    @Schema(name = "phone_number", example = "0987654321")
    private String phoneNumber;

    @NotBlank(message = "Password cannot be blank")
    @Schema(example = "secret123")
    private String password;
}
