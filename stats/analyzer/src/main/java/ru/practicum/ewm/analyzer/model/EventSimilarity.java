package ru.practicum.ewm.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "similarities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private Long eventA;

    @Column
    private Long eventB;

    @Column
    private Double score;

    @Column
    private LocalDateTime timestamp;
}