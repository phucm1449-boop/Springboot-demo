package com.example.demo.controller;

import com.example.demo.responses.ApiResponse;
import com.github.javafaker.Faker;
import com.example.demo.dto.ProductDTO;
import com.example.demo.dto.ProductImageDTO;
import com.example.demo.models.Product;
import com.example.demo.models.ProductImage;
import com.example.demo.responses.ProductListResponse;
import com.example.demo.responses.ProductResponse;
import com.example.demo.services.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog endpoints")
public class ProductController {

    private final IProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ApiResponse<?> getProductByID(@PathVariable("id") int productId) {
        ApiResponse<ProductResponse> apiResponse = new ApiResponse<>();
        Product existingProduct = productService.getProductById(productId);
        apiResponse.setResult(ProductResponse.fromProduct(existingProduct));
        return apiResponse;
    }

/*
{
    "name": "ip",
    "price": -812.34,
    "thumbnail": "",
    "description": "This is a test product",
    "category_id": 1
}
* */

    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product")
    public ApiResponse<Product> createProduct1(
            @Valid @RequestBody ProductDTO productDTO) {
        ApiResponse<Product> apiResponse = new ApiResponse<>();
        apiResponse.setResult(productService.createProduct1(productDTO));
        return apiResponse;
    }

    @PostMapping("")
    @Operation(summary = "Create a product")
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody ProductDTO productDTO,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }
            Product newProduct = productService.createProduct(productDTO);
            return ResponseEntity.ok(newProduct);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload one or more images for a product")
    public ResponseEntity<?> uploadImage(@PathVariable Integer id, @ModelAttribute("files") List<MultipartFile> files) {
        try {
            Product existingProduct = productService.getProductById(id);
            files = files == null ? new ArrayList<>() : files;
            if (files.size() > ProductImage.MAXIMUM_IMAGES_PER_PRODUCT) {
                return ResponseEntity.badRequest().body("Number of images must be <= " + ProductImage.MAXIMUM_IMAGES_PER_PRODUCT);
            }
            List<ProductImage> productImages = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.getSize() == 0) {
                    continue;
                }
                // kiểm tra kích thước file and định dạng
                if (file.getSize() > 10 * 1024 * 1024) { // kích thước lớn hơn 10MB
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File is too large! Maximum size is 10MB");
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("File must be an image");
                }
                // lưu file và cập nhật thumbnail trong DTO
                String filename = storeFile(file);
                // lưu vào đối tượng product trong DB
                // lưu vào bảng product_images
                ProductImage productImage =productService.createProductImage(
                        existingProduct.getId(),
                        ProductImageDTO.builder()
                                .imageUrl(filename)
                                .build());
                productImages.add(productImage);
            }
            return ResponseEntity.ok(productImages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (!isImageFile(file) || file.getOriginalFilename() == null) {
            throw new IOException("Invalid image file format ");
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // thêm UUID vào trước tên file để đảm bảo tên file là duy nhất
        // tránh trường hợp 2 image khác nhau nhưng trùng tên --> sẽ bị ghi đè file lên nhau
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

        // đường dẫn đến thư mục mà bạn muốn lưu file
        Path uploadDir = Paths.get("uploads");
        // kiểm tra và tạo thư mục nếu nó không tồn tại
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        // đường link đầy đủ đến file
        Path destination = Paths.get(uploadDir.toString(), uniqueFileName);
        // sao chép file vào thư mục đích
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    @GetMapping("")
    @Operation(summary = "List products with pagination")
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        ApiResponse<ProductListResponse> apiResponse = new ApiResponse<>();
        // tạo Pageable từ thông tin trang và giới hạn
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<ProductResponse> productPage = productService.getAllProducts(pageRequest);
        // lấy tổng số trang
        int totalPages = productPage.getTotalPages();
        // lấy danh sách các product
        List<ProductResponse> products = productPage.getContent();
        apiResponse.setResult(ProductListResponse.builder().products(products).totalPages(totalPages).build());
        return apiResponse;
//        return ResponseEntity.ok(ProductListResponse.builder()
//                        .products(products)
//                        .totalPages(totalPages)
//                        .build());
    }

//    @GetMapping("/ByDTO")
//    @Operation(summary = "List products with pagination")
//    public ResponseEntity<ProductListResponse> getProductsByDTO(@RequestBody ProductFilterRequest filterRequest) {
//        // tạo Pageable từ thông tin trang và giới hạn
//        PageRequest pageRequest = PageRequest.of(
//                filterRequest.getPage(), filterRequest.getLimit(), Sort.by("createdAt").descending());
//        Page<ProductResponse> productPage = productService.getAllProducts(pageRequest);
//        // lấy tổng số trang
//        int totalPages = productPage.getTotalPages();
//        // lấy danh sách các product
//        List<ProductResponse> products = productPage.getContent();
//        return ResponseEntity.ok(ProductListResponse.builder()
//                .products(products)
//                .totalPages(totalPages)
//                .build());
//    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<String> deleteProduct(@PathVariable int id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(String.format("Product with id = %d deleted successfully", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

//    @PostMapping("/generateFakeProducts")
    public ResponseEntity<String> generateFakeProducts() {
        Faker faker = new Faker();
        for (int i = 0; i < 500; i++) {
            String productName = faker.commerce().productName();
            if (productService.existsByName(productName)) {
                continue;
            }
            ProductDTO productDTO = ProductDTO.builder()
                    .name(productName)
                    .price((float) faker.number().numberBetween(10, 90000000))
                    .description(faker.lorem().sentence())
                    .thumbnail("")
                    .categoryId(faker.number().numberBetween(2, 5))
                    .build();
            try {
                productService.createProduct(productDTO);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        return ResponseEntity.ok("generateFakeProducts");
    }

    //update a product
    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ApiResponse<?> updateProduct(
            @PathVariable int id,
            @RequestBody ProductDTO productDTO) {
       ApiResponse<Product> apiResponse = new ApiResponse<>();
       apiResponse.setResult(productService.updateProduct(id, productDTO));
       return apiResponse;
    }
}
