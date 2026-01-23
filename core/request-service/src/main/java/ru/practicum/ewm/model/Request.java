package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.enums.request.RequestStatus;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long event;

    @Column(name = "user_id", nullable = false)
    private Long requester;

    @Column
    @Builder.Default
    private Instant created = Instant.now();

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}