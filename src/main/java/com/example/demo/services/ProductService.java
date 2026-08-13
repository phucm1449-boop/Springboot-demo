package com.example.demo.services;

import com.example.demo.dto.ProductDTO;
import com.example.demo.dto.ProductImageDTO;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.mapper.DTOMapper;
import com.example.demo.models.Category;
import com.example.demo.models.Product;
import com.example.demo.models.ProductImage;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.ProductImageRepo;
import com.example.demo.repo.ProductRepo;
import com.example.demo.responses.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService{
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ProductImageRepo productImageRepo;
    private final DTOMapper dtoMapper;

    @Override
    public Product getProductById(int id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
    }

    @Override
    public Product createProduct(ProductDTO productDTO) {
        Category existingCategory = categoryRepo.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
        Product newProduct = Product.builder()
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .thumbnail(productDTO.getThumbnail())
                .category(existingCategory)
                .build();
        return productRepo.save(newProduct);
    }

    @Override
    public Product createProduct1(ProductDTO productDTO) {
        Category existingCategory = categoryRepo.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
        Product newProduct = dtoMapper.toProduct(productDTO);
        newProduct.setCategory(existingCategory);
        return productRepo.save(newProduct);
    }

    @Override
    public Page<ProductResponse> getAllProducts(PageRequest pageRequest) {
        // lấy danh sách product theo trang(page) và giới hạn(limit)
        return productRepo.findAll(pageRequest).map(ProductResponse::fromProduct);
    }

    @Override
    public Product updateProduct(int id, ProductDTO productDTO) {
        Product existingProduct = getProductById(id);
        if (existingProduct != null) {
            Category existingCategory = categoryRepo.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND));
            // coppy các thuộc tính từ DTO --> Product
            // có thể sử dụng ModelMapper để copy trong case quá nhiều thuộc tính
            dtoMapper.updateProduct(existingProduct, productDTO);
//            existingProduct.setName(productDTO.getName());
//            existingProduct.setDescription(productDTO.getDescription());
//            existingProduct.setPrice(productDTO.getPrice());
//            existingProduct.setThumbnail(productDTO.getThumbnail());
            existingProduct.setCategory(existingCategory);
            return productRepo.save(existingProduct);
        }
        return null;
    }

    @Override
    public void deleteProduct(int id) {
        Optional<Product> productOptional = productRepo.findById(id);
        productOptional.ifPresent(productRepo::delete);
    }

    @Override
    public boolean existsByName(String name) {
        return productRepo.existsByName(name);
    }

    @Override
    public ProductImage createProductImage(Integer productId, ProductImageDTO productImageDTO) {
        Product existingProduct = productRepo.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.DATA_NOT_FOUND)) ;
        ProductImage newProductImage = ProductImage.builder()
                .product(existingProduct)
                .imageUrl(productImageDTO.getImageUrl())
                .build();
        // không cho insert quá 5 ảnh trong 1 product
        int size = productImageRepo.findByProductId(existingProduct.getId()).size();
        if (size >= ProductImage.MAXIMUM_IMAGES_PER_PRODUCT) {
            throw new AppException(ErrorCode.SIZE_IS_LARGER_THAN_MAXIMUM_IMAGES_PER_PRODUCT);
        }
        return productImageRepo.save(newProductImage);
    }
}
