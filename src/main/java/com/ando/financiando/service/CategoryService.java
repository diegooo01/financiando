package com.ando.financiando.service;

import com.ando.financiando.dto.CategoryResponse;
import com.ando.financiando.dto.CreateCategoryRequest;
import com.ando.financiando.exception.NotFoundException;
import com.ando.financiando.model.Category;
import com.ando.financiando.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        List<String> keywords = request.keywords() != null ? request.keywords() : new ArrayList<>();
        Category category = new Category(request.name(), request.emoji(), keywords);
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Category getEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada: " + id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getEmoji(),
                category.getKeywords()
        );
    }
}