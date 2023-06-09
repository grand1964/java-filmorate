package ru.yandex.practicum.filmorate.model;

import lombok.Builder;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.Storable;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Data
@Builder
public class Film implements Storable {
    private long id;
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private LocalDate releaseDate;
    @Min(value = 1)
    private int duration;
    private List<Genre> genres;
    private Mpa mpa;
    private Set<Long> likes;

    /////////////////////////////// Конвертация //////////////////////////////

    //упаковка полей фильма в отображение "поле -> значение" (используется как параметр в запросах)
    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<>();
        values.put("name", name);
        values.put("description", description);
        values.put("release_date", releaseDate);
        values.put("duration", duration);
        values.put("mpa_id", mpa.getId());
        return values;
    }

    //распаковка строки базы в фильм
    public static Film mapRowToFilm(ResultSet resultSet, int rowNum) throws SQLException {
        Film film = Film.builder()
                .id(resultSet.getLong("id"))
                .name(resultSet.getString("name"))
                .description(resultSet.getString("description"))
                .releaseDate(resultSet.getDate("release_date").toLocalDate())
                .duration(resultSet.getInt("duration"))
                .mpa(Mpa.builder()
                        .id(resultSet.getLong("mpa_id"))
                        .name(null)
                        .build())
                .build();
        film.setLikes(new HashSet<>());
        return film;
    }
}
