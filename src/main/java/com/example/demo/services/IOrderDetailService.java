package com.example.demo.services;

import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.models.OrderDetail;

import java.util.List;

public interface IOrderDetailService {
    OrderDetail createOrderDetail(OrderDetailDTO newOrderDetail) throws Exception;

    OrderDetail getOrderDetail(Long id);

    OrderDetail updateOrderDetail(Long id, OrderDetailDTO newOrderDetailData);

    void deleteById(Long id);

    List<OrderDetail> findByOrderId(Long orderId);
}
