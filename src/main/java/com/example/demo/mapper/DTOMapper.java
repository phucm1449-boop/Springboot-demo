package com.example.demo.mapper;

import com.example.demo.dto.ProductDTO;
import com.example.demo.models.Product;
import com.example.demo.models.User;
import com.example.demo.responses.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DTOMapper {
    @Mapping(source = "name", target = "description")
    Product toProduct(ProductDTO productDTO);
    void updateProduct(@MappingTarget Product product, ProductDTO productDTO);
    UserResponse toUserResponse(User user);
}
