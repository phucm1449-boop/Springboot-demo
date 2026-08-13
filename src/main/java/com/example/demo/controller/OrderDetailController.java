package com.example.demo.controller;

import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.models.OrderDetail;
import com.example.demo.responses.OrderDetailResponse;
import com.example.demo.services.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/order_details")
@Tag(name = "Order Details", description = "Order item endpoints")
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    //Thêm mới 1 order detail
    @PostMapping("")
    @Operation(summary = "Create an order detail")
    public ResponseEntity<?> createOrderDetail(
            @Valid @RequestBody OrderDetailDTO orderDetailDTO) {
        try {
            OrderDetail orderDetail = orderDetailService.createOrderDetail(orderDetailDTO);
            return ResponseEntity.ok().body(OrderDetailResponse.fromOrderDetail(orderDetail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order detail by id")
    public ResponseEntity<?> getOrderDetail(
            @Valid @PathVariable("id") Integer id) {
        try {
            OrderDetail orderDetail = orderDetailService.getOrderDetail(id);
            return ResponseEntity.ok().body(OrderDetailResponse.fromOrderDetail(orderDetail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //lấy ra danh sách các order_details của 1 order nào đó
    @GetMapping("/order/{orderId}")
    @Operation(summary = "List order details for an order")
    public ResponseEntity<?> getOrderDetails(
            @Valid @PathVariable("orderId") Integer orderId
    ) {
        List<OrderDetail> orderDetailList = orderDetailService.findByOrderId(orderId);
        List<OrderDetailResponse> orderDetailResponses = orderDetailList
                .stream()
                .map(OrderDetailResponse::fromOrderDetail).toList();
//                .map(orderDetail -> OrderDetailResponse.fromOrderDetail(orderDetail)).toList();
        return ResponseEntity.ok().body(orderDetailResponses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an order detail")
    public ResponseEntity<?> updateOrderDetail(
            @Valid @PathVariable("id") Integer id,
            @RequestBody OrderDetailDTO orderDetailDTO) {
        try {
            OrderDetail orderDetail = orderDetailService.updateOrderDetail(id, orderDetailDTO);
            return ResponseEntity.ok().body(orderDetail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an order detail")
    public ResponseEntity<?> deleteOrderDetail(
            @Valid @PathVariable("id") Integer id) {
        orderDetailService.deleteById(id);
        return ResponseEntity.ok().body("deleteOrderDetail with id: " + id);
    }
}
