package ru.practicum.ewm.category.service;

import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.CategoryRequestDto;

import java.util.List;

public interface CategoryService {

    // Admin API:
    CategoryDto add(CategoryRequestDto newDto);

    CategoryDto update(Long catId, CategoryRequestDto updDto);

    void delete(Long categoryId);

    // Public API:
    CategoryDto getById(Long categoryId);

    List<CategoryDto> getAll(int from, int size);
}