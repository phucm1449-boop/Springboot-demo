package com.example.demo.services;


import com.example.demo.dto.ProductDTO;
import com.example.demo.dto.ProductImageDTO;
import com.example.demo.models.Product;
import com.example.demo.models.ProductImage;
import com.example.demo.responses.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface IProductService {

    Product createProduct(ProductDTO productDTO) throws Exception;

    Product createProduct1(ProductDTO productDTO);

    Product getProductById(int id);

    // phân trang
    Page<ProductResponse> getAllProducts(PageRequest pageRequest);

    Product updateProduct(int id, ProductDTO productDTO);

    void deleteProduct(int id);

    boolean existsByName(String name);

    ProductImage createProductImage(Integer productId, ProductImageDTO productImageDTO) throws Exception;
}
