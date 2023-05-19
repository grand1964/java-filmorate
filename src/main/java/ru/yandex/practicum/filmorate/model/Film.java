package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.storage.Storable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Set;

@Data
public class Film implements Storable {
    private long id;
    @NotBlank
    private String name;
    private String description;
    private LocalDate releaseDate;
    @Min(value = 1)
    private int duration;
    private Set<Long> likes;
}
