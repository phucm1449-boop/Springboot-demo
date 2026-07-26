package com.example.demo.controller;

import com.example.demo.dto.CategoryDTO;
import com.example.demo.models.Category;
import com.example.demo.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/categories")
//@Validated
@RequiredArgsConstructor //Dependency Injection
public class CategoryController {

    private final CategoryService categoryService;

    // nếu tham số truyền vào là 1 object thì sao ? ==> Data Transfer Object = Request Object
    @PostMapping("")
    @Operation(summary = "Create a category")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDTO categoryDTO, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            return ResponseEntity.badRequest().body(errorMessages);
        }
        categoryService.createCategory(categoryDTO);
        return ResponseEntity.ok("insertCategory successfully" + categoryDTO);
    }

    // hiện tất cả các categories, lấy ở trang số mấy và bao nhiêu bản ghi
    @GetMapping("") // http://localhost:8088/api/v1/categories?page=1&limit=10
    @Operation(summary = "List all categories")
    public ResponseEntity<List<Category>> getAllCategories(
            @RequestParam("page") int page,
            @RequestParam("limit") int limit) {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<String> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok("updateCategory successfully ");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("deleteCategory with id " + id);
    }
}
