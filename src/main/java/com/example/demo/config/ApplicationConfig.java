package com.example.demo.config;

import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.mapper.DTOMapper;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repo.RoleRepo;
import com.example.demo.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.modelmapper.ModelMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfig {

    private final PasswordEncoder passwordEncoder;

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public ApplicationRunner applicationRunner(UserRepo userRepo, RoleRepo roleRepo) {
        return args -> {
            Role roleAdmin = roleRepo.findByName("ADMIN")
                    .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
            if (userRepo.findByRole(roleAdmin).isEmpty()) {
                User userAdmin = User.builder()
                        .fullName("ADMIN_REAL")
                        .phoneNumber("0856599009")
                        .password(passwordEncoder.encode("123456789"))
                        .address("Ha Noi")
                        .dateOfBirth(LocalDate.now())
                        .role(roleAdmin)
                        .build();
                userRepo.save(userAdmin);
                log.warn("admin user has been created with default password: 123456789");
            }
        };
    }
}
