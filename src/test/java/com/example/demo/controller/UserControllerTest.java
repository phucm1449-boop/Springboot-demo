package com.example.demo.controller;

import com.example.demo.component.JwtTokenUtil;
import com.example.demo.config.SecurityConfig;
import com.example.demo.config.WebSecurityConfig;
import com.example.demo.dto.UserDTO;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repo.InvalidatedTokenRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@WebMvcTest(UserController.class)
// @WebMvcTest does not auto-load @Configuration security classes. Import them explicitly so the
// real permitAll() rules apply; without this, Spring's default security blocks all POST requests.
@Import({SecurityConfig.class, WebSecurityConfig.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Spring Boot auto-configures ObjectMapper with JavaTimeModule — no need to build it manually
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // SecurityConfig requires UserRepo; JwtAuthenticationFilter requires JwtTokenUtil and InvalidatedTokenRepo.
    // @WebMvcTest does not load JPA beans, so we mock them to satisfy the security layer's dependencies.
    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private InvalidatedTokenRepo invalidatedTokenRepo;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    public void initData() {
        LocalDate dob = LocalDate.of(2001, 8, 22);
        Role role = new Role(1, "USER");
        userDTO = UserDTO.builder()
                .fullName("Mai Gia Phuc").phoneNumber("0854475387")
                .address("Đây là USER nhé").password("22012007")
                .retypePassword("22012007").dateOfBirth(dob)
                .facebookAccountId(0).googleAccountId(0)
                .roleId(1)
                .build();
        user = User.builder().id(17)
                .fullName("Mai Gia Phuc").phoneNumber("0854475387")
                .address("Đây là USER nhé").active(true)
                .dateOfBirth(dob).facebookAccountId(0).googleAccountId(0)
                .role(role)
                .build();
    }

    @Test
    void createUser_validRequest_success() throws Exception {
        Mockito.when(userService.createUser(any()))
                .thenReturn(user);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.id").value(17));

        verify(userService, times(1)).createUser(any());
    }

    @Test
    void createUser_passwordInvalid_fail() throws Exception {
        userDTO.setPassword("123456");

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1014))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Password must be at least 8 character"));

        verify(userService, never()).createUser(any());
    }
}
