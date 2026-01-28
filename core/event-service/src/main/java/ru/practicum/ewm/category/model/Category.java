package ru.practicum.ewm.category.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories", schema = "events_schema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
