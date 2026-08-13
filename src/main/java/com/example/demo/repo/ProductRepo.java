package com.example.demo.repo;

import com.example.demo.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Integer> {
    // check xem sản phẩm đó với cái name đó có tồn tại không
    boolean existsByName(String name);

//    Page<Product> findAll(Pageable pageable);// phân trang ( trang 1 có 10 phần tử, trang 2 từ 11-20 phần tử...)
}
