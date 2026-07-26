package com.example.demo.service;

import com.example.demo.dto.ProductDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.models.Category;
import com.example.demo.models.Product;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.ProductRepo;
import com.example.demo.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.example.demo.mapper.DTOMapper;
import com.example.demo.repo.ProductImageRepo;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;
    @Mock
    private ProductRepo productRepo;
    @Mock
    private CategoryRepo categoryRepo;
    @Mock
    private ProductImageRepo productImageRepo;
    @Mock
    private DTOMapper dtoMapper;
    private Category category;
    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void initData() {
        Long categoryId = 1L;
        category = Category.builder()
                .id(categoryId)
                .name("Laptop")
                .build();
        product = Product.builder()
                .id(1L)
                .name("Laptop HP")
                .description("Laptop HP made in Viet Nam")
                .price(15000F)
                .thumbnail("")
                .category(category)
                .build();
        productDTO = ProductDTO.builder()
                .name("Laptop HP")
                .description("Laptop HP made in Viet Nam")
                .price(15000F)
                .thumbnail("")
                .categoryId(categoryId)
                .build();
    }

    @Test
    void findById_success() {
        when(productRepo.findById(1L))
                .thenReturn(Optional.of(product));

        Product result = this.productService.getProductById(1L);

//        assertThat(result).isEqualTo(product);
        verify(productRepo).findById(1L);
    }

    @Test
    void findById_fail_throwException() {
        when(productRepo.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
        .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.DATA_NOT_FOUND);
    }

    @Test
    void createProduct_success() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

//        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(productRepo.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        Product result = productService.createProduct(productDTO);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop HP");
        assertThat(result.getDescription()).isEqualTo("Laptop HP made in Viet Nam");
        assertThat(result.getPrice()).isEqualTo(15000F);
        assertThat(result.getThumbnail()).isEqualTo("");
        assertThat(result.getCategory()).isEqualTo(category);

//      Weak assertions — added ArgumentCaptor to verify the actual Product passed
//      to save() has fields correctly mapped from productDTO
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo(productDTO.getName());
        assertThat(saved.getDescription()).isEqualTo(productDTO.getDescription());
        assertThat(saved.getPrice()).isEqualTo(productDTO.getPrice());
        assertThat(saved.getThumbnail()).isEqualTo(productDTO.getThumbnail());
        assertThat(saved.getCategory()).isEqualTo(category);
    }

    @Test
    void createProduct_categoryNotFound_throwsException() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(productDTO))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.DATA_NOT_FOUND);
    }
}
