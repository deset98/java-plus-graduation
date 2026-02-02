package ru.practicum.ewm.analyzer.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.analyzer.dto.EventSimilarityDto;
import ru.practicum.ewm.analyzer.model.EventSimilarity;

@Mapper(componentModel = "spring")
public interface EventSimilarityMapper {

    EventSimilarityDto toDto(EventSimilarity eventSimilarity);
}