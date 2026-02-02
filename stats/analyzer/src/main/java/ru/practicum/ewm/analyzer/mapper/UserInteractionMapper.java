package ru.practicum.ewm.analyzer.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.analyzer.dto.UserInteractionDto;
import ru.practicum.ewm.analyzer.model.UserInteraction;

@Mapper(componentModel = "spring")
public interface UserInteractionMapper {

    UserInteractionDto toDto(UserInteraction userInteraction);
}