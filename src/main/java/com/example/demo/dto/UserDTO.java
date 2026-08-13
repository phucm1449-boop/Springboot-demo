package com.example.demo.dto;

import com.example.demo.validator.DobConstraint;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload used to register a new user")
public class UserDTO {
    @JsonProperty("fullname")
    @Schema(name = "fullname", example = "Nguyen Van A")
    private String fullName;

    @JsonProperty("phone_number")
    @NotBlank(message = "Phone number is required")
    @Schema(name = "phone_number", example = "0987654321")
    private String phoneNumber;

    @Schema(example = "123 Main Street")
    private String address;

    @NotBlank(message = "Password cannot be blank")
    @Schema(example = "secret123")
    @Size(min = 8, message = "INVALID_PASSWORD")
    private String password;

    @JsonProperty("retype_password")
    @Schema(name = "retype_password", example = "secret123")
    private String retypePassword;

    @JsonProperty("date_of_birth")
    @Schema(name = "date_of_birth", example = "2000-01-15T00:00:00.000+00:00")
    @DobConstraint(min = 16, message = "INVALID_DOB")
    private LocalDate dateOfBirth;

    @JsonProperty("facebook_account_id")
    @Schema(name = "facebook_account_id", example = "0")
    private int facebookAccountId;

    @JsonProperty("google_account_id")
    @Schema(name = "google_account_id", example = "0")
    private int googleAccountId;

    @NotNull(message = "Role ID is required")
    @JsonProperty("role_id")
    @Schema(name = "role_id", example = "2")
    private Integer roleId;
}
