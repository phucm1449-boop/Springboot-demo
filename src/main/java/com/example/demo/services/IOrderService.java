package com.example.demo.services;

import com.example.demo.dto.OrderDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.responses.OrderResponse;

import java.util.List;

public interface IOrderService {

    OrderResponse createOrder(OrderDTO orderDTO) throws Exception;

    OrderResponse getOrder(Long id);

    OrderResponse updateOrder(Long id, OrderDTO orderDTO);

    void deleteOrder(Long id);

    List<OrderResponse> getAllOrders(Long userId);
}
