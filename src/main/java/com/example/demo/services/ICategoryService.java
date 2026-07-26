package com.example.demo.services;

import com.example.demo.dto.CategoryDTO;
import com.example.demo.models.Category;

import java.util.List;

public interface ICategoryService {

    Category getCategoryById(long id);

    Category createCategory(CategoryDTO categoryDTO);

    List<Category> getAllCategories();

    Category updateCategory(long categoryId, CategoryDTO categoryDTO);

    void deleteCategory(long id);
}
