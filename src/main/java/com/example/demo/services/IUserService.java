package com.example.demo.services;

import com.example.demo.dto.UserDTO;
import com.example.demo.models.User;
import com.example.demo.responses.UserResponse;

import java.util.List;

public interface IUserService {

    User createUser(UserDTO userDTO) throws Exception;

    // return String bởi vì sau này mỗi lần login sẽ return 1 token
    String login(String phoneNumber, String password);

    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse getMyInfo();

    void logout(String token);
    String refresh(String token);
}
