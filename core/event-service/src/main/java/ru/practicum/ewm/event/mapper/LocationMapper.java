package ru.practicum.ewm.event.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.dto.event.LocationDto;
import ru.practicum.ewm.event.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    Location toEntity(LocationDto locationDto);

    LocationDto toDto(Location location);
}