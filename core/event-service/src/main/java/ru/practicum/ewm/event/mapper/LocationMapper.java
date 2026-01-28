package ru.practicum.ewm.event.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.event.model.LocationEntity;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationEntity toEntity(Location location);

    Location toDto(LocationEntity locationEntity);
}