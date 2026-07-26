package com.example.demo.services;

import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.models.Order;
import com.example.demo.models.OrderDetail;
import com.example.demo.models.Product;
import com.example.demo.repo.OrderDetailRepo;
import com.example.demo.repo.OrderRepo;
import com.example.demo.repo.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDetailService implements IOrderDetailService {
    private final OrderDetailRepo orderDetailRepo;
    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;

    @Override
    public OrderDetail createOrderDetail(OrderDetailDTO newOrderDetail) throws Exception {
        Order existingOrder = orderRepo.findById(newOrderDetail.getOrderId()).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));
        Product existingProduct = productRepo.findById(newOrderDetail.getProductId()).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));
        OrderDetail orderDetail = OrderDetail.builder()
                .order(existingOrder)
                .product(existingProduct)
                .numberOfProducts(newOrderDetail.getNumberOfProducts())
                .price(newOrderDetail.getPrice())
                .totalMoney(newOrderDetail.getTotalMoney())
                .color(newOrderDetail.getColor())
                .build();
        return orderDetailRepo.save(orderDetail);
    }

    @Override
    public OrderDetail getOrderDetail(Long id) {
        return orderDetailRepo.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));
    }

    @Override
    public OrderDetail updateOrderDetail(Long id, OrderDetailDTO newOrderDetailData) {
        // tìm xem order detail có tồn tại không
        OrderDetail existingOrderDetail = getOrderDetail(id);
        Order existingOrder = orderRepo.findById(newOrderDetailData.getOrderId()).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));
        Product existingProduct = productRepo.findById(newOrderDetailData.getProductId()).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));
        existingOrderDetail.setPrice(newOrderDetailData.getPrice());
        existingOrderDetail.setTotalMoney(newOrderDetailData.getTotalMoney());
        existingOrderDetail.setColor(newOrderDetailData.getColor());
        existingOrderDetail.setNumberOfProducts(newOrderDetailData.getNumberOfProducts());
        existingOrderDetail.setOrder(existingOrder);
        existingOrderDetail.setProduct(existingProduct);
        return orderDetailRepo.save(existingOrderDetail);
    }

    @Override
    public void deleteById(Long id) {
        orderDetailRepo.deleteById(id);
    }

    @Override
    public List<OrderDetail> findByOrderId(Long orderId) {
        return orderDetailRepo.findByOrderId(orderId);
    }
}
