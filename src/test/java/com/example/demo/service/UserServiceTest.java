package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repo.RoleRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepo userRepo;

    @Mock
    private RoleRepo roleRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User user;
    private UserDTO userDTO;
    private LocalDate dob;
    private Role role;

    @BeforeEach
    public void initData() {
        dob = LocalDate.of(2001, 8, 22);
        role = new Role(1L, "USER");
        userDTO = UserDTO.builder()
                .fullName("Mai Gia Phuc").phoneNumber("0854475387")
                .address("Đây là USER nhé").password("22012007")
                .retypePassword("22012007").dateOfBirth(dob)
                .facebookAccountId(0).googleAccountId(0)
                .roleId(1L)
                .build();
        user = User.builder().id(17L)
                .fullName("Mai Gia Phuc").phoneNumber("0854475387")
                .address("Đây là USER nhé").active(true)
                .dateOfBirth(dob).facebookAccountId(0).googleAccountId(0)
                .role(role)
                .build();
    }

    @Test
    // đây gọi là 1 test case
    void createUser_validRequest_success() {
        // GIVEN - chuẩn bị dữ liệu giả lập (mock)
        // 1. Fake: "this phone number does NOT exist yet"
        Mockito.when(userRepo.existsByPhoneNumber(ArgumentMatchers.anyString()))
                .thenReturn(false);
        // 2. Fake: "when asked for role with id=1, return the USER role"
        Mockito.when(roleRepo.findById(1L))
                .thenReturn(Optional.of(role));
        // 3. Fake: "when saving any User, return our pre-built user object"
        Mockito.when(userRepo.save(ArgumentMatchers.any(User.class)))
                .thenReturn(user);
        Mockito.when(passwordEncoder.encode(ArgumentMatchers.anyString()))
                .thenReturn("encoded-password");

        // WHEN - gọi hàm cần test
        User result = userService.createUser(userDTO);

        // Capture
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        User savedUser = captor.getValue();

        // THEN - kiểm tra kết quả trả về đúng không
        assertThat(savedUser.getId()).isEqualTo(17L);
        assertThat(savedUser.getFullName()).isEqualTo("Mai Gia Phuc");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("0854475387");
        assertThat(savedUser.getRole().getName()).isEqualTo("USER");
    }
}
