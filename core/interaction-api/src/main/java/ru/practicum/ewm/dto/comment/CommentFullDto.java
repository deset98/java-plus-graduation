package ru.practicum.ewm.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.enums.comment.CommentState;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentFullDto {

    private Long id;

    private String annotation;

    private String text;

    private Long authorId;

    private Long eventId;

    private LocalDateTime publishedOn;

    private CommentState state;
}