package com.example.demo.services;

import com.example.demo.component.JwtTokenUtil;
import com.example.demo.dto.UserDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.mapper.DTOMapper;
import com.example.demo.models.Role;
import com.example.demo.models.User;
import com.example.demo.repo.InvalidatedTokenRepo;
import com.example.demo.repo.RoleRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final DTOMapper dtoMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public User createUser(UserDTO userDTO) {
        log.info("UserService: createUser");
        String phoneNumber = userDTO.getPhoneNumber();
//        if(userRepo.existsByPhoneNumber(phoneNumber)) {
//            throw new DataIntegrityViolationException("Phone number already exists");
//        }
        Role role = roleRepo.findById(userDTO.getRoleId())
                .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
        if (role.getName().toUpperCase().equals(Role.ADMIN)) {
            throw new AppException(ErrorCode.YOU_CAN_NOT_REGISTER_USER);
        }
        // convert from UserDTO to User
        User newUser = User.builder()
                .fullName(userDTO.getFullName())
                .phoneNumber(phoneNumber)
                .password(userDTO.getPassword())
                .address(userDTO.getAddress())
                .dateOfBirth(userDTO.getDateOfBirth())
                .facebookAccountId(userDTO.getFacebookAccountId())
                .googleAccountId(userDTO.getGoogleAccountId())
                .build();

        newUser.setRole(role);
        // check nếu có accountId --> không yêu cầu password
        if (userDTO.getFacebookAccountId() == 0 && userDTO.getGoogleAccountId() == 0) {
            String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
            newUser.setPassword(encodedPassword);
        }

        try {
            newUser = userRepo.save(newUser);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.DATA_EXISTED);
        }

        return newUser;
    }

    @Override
    public String login(String phoneNumber, String password) {
        Optional<User> optionalUser = userRepo.findByPhoneNumber(phoneNumber);
        if (optionalUser.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_PHONE_NUMBER_OR_PASSWORD);
        }
        // return JWT Token --> là 1 chìa khóa có thể vào được all url
        User existingUser = optionalUser.get();
        // check password
        if (existingUser.getFacebookAccountId() == 0 && existingUser.getGoogleAccountId() == 0) {
            if (!passwordEncoder.matches(password, existingUser.getPassword())) {
                throw new AppException(ErrorCode.BAD_CREDENTIALS);
            }
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                phoneNumber, password, existingUser.getAuthorities());

        // authenticate with Java Spring Security
        authenticationManager.authenticate(authenticationToken);
        return jwtTokenUtil.generateToken(existingUser);
    }

    @Override
    public void logout(String token) {
        jwtTokenUtil.logout(token);
    }

    @Override
    public String refresh(String token) {
        return jwtTokenUtil.refreshAndGenerateToken(token);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        log.info("In method getAllUsers");
        List<User> userList = this.userRepo.findAll();
        List<UserResponse> userResponseList = new ArrayList<>();
        for (User user : userList) {
            UserResponse userResponse = UserResponse.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .dateOfBirth(user.getDateOfBirth())
                    .facebookAccountId(user.getFacebookAccountId())
                    .googleAccountId(user.getGoogleAccountId())
                    .active(user.isActive())
                    .role(user.getRole())
                    .build();
            userResponseList.add(userResponse);
        }
        return userResponseList;
    }

    @Override
    @PostAuthorize("returnObject.phoneNumber == authentication.name")
    public UserResponse getUserById(Long id) {
        log.info("In method getUserById");
        User user = this.userRepo.findById(id).orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
        return dtoMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String phoneNumber = context.getAuthentication().getName();
        var authorities = context.getAuthentication().getAuthorities();
        var credentials = context.getAuthentication().getCredentials();
        var details = context.getAuthentication().getDetails();
        var principal = context.getAuthentication().getPrincipal();

        log.info("phoneNumber:{}, authorities:{}, credentials:{}, details:{}, principal:{}", phoneNumber, authorities, credentials, details, principal);

       User user = this.userRepo.findByPhoneNumber(phoneNumber)
               .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
       return dtoMapper.toUserResponse(user);
    }

}
