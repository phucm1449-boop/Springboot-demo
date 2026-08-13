package com.example.demo.repo;

import com.example.demo.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Integer> {
    // tìm các đơn hàng của 1 user nào đó
    List<Order> findByUserId(Integer userId);
}
