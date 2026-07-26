package com.example.demo.repo;

import com.example.demo.models.Role;
import com.example.demo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByPhoneNumber(String phoneNumber); // return 1 user or null
    Optional<User> findByRole(Role role);
}
