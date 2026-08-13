package com.example.demo.responses;

import com.example.demo.models.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Integer id;
    String fullName;
    String phoneNumber;
    String address;
    boolean active;
    LocalDate dateOfBirth;
    int facebookAccountId;
    int googleAccountId;
    Role role;
}
