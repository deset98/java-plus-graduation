package ru.practicum.ewm.comment.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.dto.comment.CommentFullDto;
import ru.practicum.ewm.dto.comment.CommentPublicDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdCommentDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    Comment toEntity(NewCommentDto newDto);

    @Mapping(target = "publishedOn", expression = "java(toLocalDateTime(comment.getPublishedOn()))")
    @Mapping(target = "eventId", ignore = true)
    CommentFullDto toFullDto(Comment comment);

    @Mapping(target = "authorName", ignore = true)
    @Mapping(target = "eventTitle", ignore = true)
    @Mapping(target = "publishedOn", expression = "java(toLocalDateTime(comment.getPublishedOn()))")
    CommentPublicDto toPublicDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(UpdCommentDto updDto, @MappingTarget Comment comment);

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }
}