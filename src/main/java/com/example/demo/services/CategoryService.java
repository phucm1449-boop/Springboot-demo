package com.example.demo.services;

import com.example.demo.dto.CategoryDTO;
import com.example.demo.models.Category;
import com.example.demo.repo.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {

    private final CategoryRepo categoryRepo;
    // Inject
//    public CategoryService(CategoryRepo categoryRepo) {
//        this.categoryRepo = categoryRepo;
//    }

    @Override
    public Category getCategoryById(int id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public Category createCategory(CategoryDTO categoryDTO) {
        Category category = Category.builder()
                .name(categoryDTO.getName())
                .build();
        return categoryRepo.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }

    @Override
    public Category updateCategory(int categoryId, CategoryDTO categoryDTO) {
        Category existinCategory = getCategoryById(categoryId);
        existinCategory.setName(categoryDTO.getName());
        categoryRepo.save(existinCategory);
        return existinCategory;
    }

    @Override
    public void deleteCategory(int id) {
        categoryRepo.deleteById(id);
    }
}
