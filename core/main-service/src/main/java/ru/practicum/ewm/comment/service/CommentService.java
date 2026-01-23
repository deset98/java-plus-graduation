package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.dto.comment.CommentFullDto;
import ru.practicum.ewm.dto.comment.CommentPublicDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdCommentDto;

import java.util.List;

public interface CommentService {

    List<CommentPublicDto> getAllBy(Long eventId);

    List<CommentFullDto> getAllBy(Long userId, Long eventId);

    CommentFullDto add(NewCommentDto dto, Long eventId, Long userId);

    CommentFullDto hide(Long eventId, Long commentId, boolean published);

    void delete(Long userId, Long commentId);

    CommentFullDto update(Long userId, Long commentId, UpdCommentDto updDto);
}