package com.example.demo.controller;

import com.example.demo.component.JwtTokenUtil;
import com.example.demo.config.SecurityConfig;
import com.example.demo.config.WebSecurityConfig;
import com.example.demo.dto.ProductDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.models.Category;
import com.example.demo.models.Product;
import com.example.demo.models.User;
import com.example.demo.repo.InvalidatedTokenRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.services.IProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.management.relation.Role;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Slf4j
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, WebSecurityConfig.class})
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProductService productService;

    // SecurityConfig requires UserRepo; JwtAuthenticationFilter requires JwtTokenUtil and InvalidatedTokenRepo.
    // @WebMvcTest does not load JPA beans, so we mock them to satisfy the security layer's dependencies.
    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private InvalidatedTokenRepo invalidatedTokenRepo;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    public void initData() {
        Category category = Category.builder()
                .id(1)
                .name("Laptop")
                .build();
        product = Product.builder()
                .id(1)
                .name("Laptop HP")
                .description("Laptop HP made in Viet Nam")
                .price(15000F)
                .thumbnail("")
                .category(category)
                .build();
        productDTO = ProductDTO.builder()
                .name("Laptop DEL")
                .description("Laptop HP made in Viet Nam")
                .price(15000F)
                .thumbnail("")
                .categoryId(1)
                .build();
    }

    @Test
    void getProductByID_success() throws Exception {
        Mockito.when(productService.getProductById(1)).thenReturn(product);

        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v1/products/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.name").value("Laptop HP"));
    }

    @Test
    void createProduct_categoryNotFound_return404() throws Exception {
        Mockito.when(productService.createProduct1(any()))
                .thenThrow(new AppException(ErrorCode.DATA_NOT_FOUND));

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/products/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code")
                        .value(1002))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Data not found"));

        // was called once, then threw
        verify(productService, times(1)).createProduct1(any(ProductDTO.class));
    }

    @Test
    void createProduct_withoutLogin_return401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/products/new"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void createProduct_withLogin_return403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/products/new"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_withLogin_return200() throws Exception {
        Mockito.when(productService.createProduct1(any(ProductDTO.class)))
                .thenReturn(product);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/products/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(productService).createProduct1(any(ProductDTO.class));
    }

//    @Test
//    @WithMockUser(roles = "ADMIN")
//    void createProduct_withLogin_return200() throws Exception {
//        mockMvc.perform(MockMvcRequestBuilders
//                        .post("/api/v1/products/new"))
//                .andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        verify(productService, times(1)).createProduct1(any(ProductDTO.class));
//    }
}
