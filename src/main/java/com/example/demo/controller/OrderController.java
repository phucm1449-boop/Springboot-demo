package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.responses.OrderResponse;
import com.example.demo.services.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order lifecycle endpoints")
public class OrderController {
    private final IOrderService orderService;

    @PostMapping("")
    @Operation(summary = "Create an order")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderDTO orderDTO, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }
            OrderResponse orderResponse = orderService.createOrder(orderDTO);
            return ResponseEntity.ok(orderResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{user_id}")//GET http://localhost:8088/api/v1/orders/user/4
    @Operation(summary = "List orders for a user")
    public ResponseEntity<?> getOrders(@Valid @PathVariable Integer user_id) {
        try {
            List<OrderResponse> orderResponseList = orderService.getAllOrders(user_id);
            return ResponseEntity.ok().body(orderResponseList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")//GET http://localhost:8088/api/v1/orders/4
    @Operation(summary = "Get an order by id")
    public ResponseEntity<?> getOrder(@Valid @PathVariable Integer id) {
        try {
            OrderResponse existingOrderResponse = orderService.getOrder(id);
            return ResponseEntity.ok().body(existingOrderResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")//GET http://localhost:8088/api/v1/orders/4
    @Operation(summary = "Update an order")
    public ResponseEntity<?> updateOrder(@Valid @PathVariable Integer id, @Valid @RequestBody OrderDTO orderDTO) {
        try {
            OrderResponse orderResponse = orderService.updateOrder(id, orderDTO);
            return ResponseEntity.ok().body(orderResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an order")
    public ResponseEntity<String> deleteOrder(@Valid @PathVariable Integer id) {
        //xóa mềm => cập nhật trường active = false
        orderService.deleteOrder(id);
        return ResponseEntity.ok().body("deleteOrder");
    }
}
