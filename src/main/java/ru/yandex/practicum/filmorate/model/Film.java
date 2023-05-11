package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class Film {
    private long id;
    @NotBlank
    private String name;
    private String description;
    private LocalDate releaseDate;
    @Min(value = 1)
    private int duration;
}
