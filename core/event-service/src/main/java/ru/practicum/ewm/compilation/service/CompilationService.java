package ru.practicum.ewm.compilation.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationDto;

import java.util.List;

@Service
public interface CompilationService {

    // Admin API:
    CompilationDto create(NewCompilationDto newCompilationDto);

    CompilationDto update(Long compId, UpdateCompilationDto updateCompilationDto);

    void delete(Long compId);

    // Public API:
    CompilationDto getBy(Long compId);

    List<CompilationDto> getAllBy(Boolean pinned, Integer from, Integer size);
}