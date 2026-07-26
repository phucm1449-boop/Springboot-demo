package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserLoginDTO;
import com.example.demo.models.User;
import com.example.demo.responses.ApiResponse;
import com.example.demo.responses.UserResponse;
import com.example.demo.services.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Registration and authentication endpoints")
public class UserController {

    private final IUserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserDTO userDTO)
            throws Exception {
        log.info("UserController: createUser");
        User newUser = userService.createUser(userDTO);
        UserResponse userResponse = UserResponse.builder()
                .id(newUser.getId())
                .fullName(newUser.getFullName())
                .phoneNumber(newUser.getPhoneNumber())
                .address(newUser.getAddress())
                .active(newUser.isActive())
                .dateOfBirth(newUser.getDateOfBirth())
                .facebookAccountId(newUser.getFacebookAccountId())
                .googleAccountId(newUser.getGoogleAccountId())
                .role(newUser.getRole())
                .build();
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userResponse);
        return apiResponse;
    }

//    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO, BindingResult result) {
//        try {
//            if (result.hasErrors()) {
//                List<String> errorMessages = result.getFieldErrors()
//                        .stream()
//                        .map(FieldError::getDefaultMessage)
//                        .toList();
//                return ResponseEntity.badRequest().body(errorMessages);
//            }
//            if (!userDTO.getPassword().equals(userDTO.getRetypePassword())) {
//                return ResponseEntity.badRequest().body("Password does not match");
//            }
//            User newUser = userService.createUser(userDTO);
//            return ResponseEntity.ok().body(newUser);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and return a JWT token")
    public ApiResponse<String> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        String token = userService.login(userLoginDTO.getPhoneNumber(), userLoginDTO.getPassword());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(token);
        return apiResponse;
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        this.userService.logout(token);
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        apiResponse.setResult(null);
        return apiResponse;
    }

    @PostMapping("/refresh")
    public ApiResponse<String> refresh(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String newToken = this.userService.refresh(token);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(newToken);
        return apiResponse;
    }

    @GetMapping("")
    public ApiResponse<List<UserResponse>> getUsers() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Username (phoneNumber): {}", authentication.getName());
        authentication.getAuthorities().forEach(g -> log.info("GrantedAuthority: {}", g.getAuthority()));

        ApiResponse<List<UserResponse>> apiResponse = new ApiResponse<>();
        List<UserResponse> userResponseList = userService.getAllUsers();
        apiResponse.setResult(userResponseList);
        return apiResponse;
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Username (phoneNumber): {}", authentication.getName());
        authentication.getAuthorities().forEach(g -> log.info("GrantedAuthority: {}", g.getAuthority()));

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUserById(id));
        return apiResponse;
    }

    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getMyInfo());
        return apiResponse;
    }
}
