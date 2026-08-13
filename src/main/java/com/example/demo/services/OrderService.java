package com.example.demo.services;

import com.example.demo.dto.OrderDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.mapper.DTOMapper;
import com.example.demo.models.Order;
import com.example.demo.models.OrderStatus;
import com.example.demo.models.User;
import com.example.demo.repo.OrderRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.responses.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {
    private final UserRepo userRepo;
    private final OrderRepo orderRepo;
    private final ModelMapper modelMapper;

    @Override
    public OrderResponse createOrder(OrderDTO orderDTO) throws Exception {
        // check xem user id có tồn tại không
        User user = userRepo.findById(orderDTO.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
        // convert orderDTO => Order
        // dùng thư viện Model Mapper --> set thuộc tính mà ko cần viết nhiều hàm set
        // tạo 1 luồng ánh xạ riêng để kiểm soát việc ánh xạ (copy các field trừ field Id)
        modelMapper.typeMap(OrderDTO.class, Order.class)
                .addMappings(mapper -> mapper.skip(Order::setId));
        // Cập nhật các trường của đơn hàng từ orderDTO
        Order order = new Order();
        modelMapper.map(orderDTO, order);
        order.setUser(user);
        order.setOrderDate(LocalDate.now());//lấy thời điểm hiện tại
        order.setStatus(OrderStatus.PENDING);
        //Kiểm tra shipping date phải >= ngày hôm nay
        LocalDate shippingDate = orderDTO.getShippingDate() == null
                ? LocalDate.now() : orderDTO.getShippingDate();
        if (shippingDate.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.DATE_MUST_BE_AT_LEAST_TODAY);
        }
        order.setShippingDate(shippingDate);
        order.setActive(true);
        orderRepo.save(order);

        modelMapper.typeMap(Order.class, OrderResponse.class)
                .addMappings(mapper -> mapper.map(src -> src.getUser().getId(), OrderResponse::setUserId));

        return modelMapper.map(order, OrderResponse.class);
    }

    @Override
    public OrderResponse getOrder(Integer id) {
        Order order = orderRepo.findById(id).orElse(null);
        modelMapper.typeMap(Order.class, OrderResponse.class)
                .addMappings(mapper -> mapper.map(src -> src.getUser().getId(), OrderResponse::setUserId));
        return modelMapper.map(order, OrderResponse.class);
    }

    @Override
    public OrderResponse updateOrder(Integer id, OrderDTO orderDTO) {
        Order existingOrder = orderRepo.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));
        User existingUser = userRepo.findById(orderDTO.getUserId()).orElseThrow(() ->
                new AppException(ErrorCode.DATA_NOT_FOUND));

        // tạo 1 luồng ánh xạ riêng để kiểm soát việc ánh xạ
        modelMapper.typeMap(OrderDTO.class, Order.class)
                .addMappings(mapper -> mapper.skip(Order::setId));
        // cập nhật các trường của order từ orderDTO
        modelMapper.map(orderDTO, existingOrder);
        existingOrder.setUser(existingUser);
        orderRepo.save(existingOrder);

        modelMapper.typeMap(Order.class, OrderResponse.class)
                .addMappings(mapper -> mapper.map(src -> src.getUser().getId(), OrderResponse::setUserId));

        return modelMapper.map(existingOrder, OrderResponse.class);
    }

    @Override
    public void deleteOrder(Integer id) {
        Order existingOrders = orderRepo.findById(id).orElse(null);
        // không xóa cứng (trong DB) --> hãy xóa mềm
       if (existingOrders != null) {
           existingOrders.setActive(false);
           orderRepo.save(existingOrders);
       }
    }

    @Override
    public List<OrderResponse> getAllOrders(Integer userId) {
        List<Order> orders = orderRepo.findByUserId(userId);

        // Cấu hình ánh xạ user -> userId
        modelMapper.typeMap(Order.class, OrderResponse.class)
                .addMappings(mapper -> mapper.map(src -> src.getUser().getId(), OrderResponse::setUserId));

        // Map từng Order sang OrderResponse
        return orders.stream()
                .map(order -> modelMapper.map(order, OrderResponse.class))
                .toList();
    }
}
