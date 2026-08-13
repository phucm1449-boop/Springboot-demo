package com.example.demo.services;

import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.models.OrderDetail;

import java.util.List;

public interface IOrderDetailService {
    OrderDetail createOrderDetail(OrderDetailDTO newOrderDetail) throws Exception;

    OrderDetail getOrderDetail(Integer id);

    OrderDetail updateOrderDetail(Integer id, OrderDetailDTO newOrderDetailData);

    void deleteById(Integer id);

    List<OrderDetail> findByOrderId(Integer orderId);
}
