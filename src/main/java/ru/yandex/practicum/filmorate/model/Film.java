package ru.yandex.practicum.filmorate.model;

import lombok.*;
import ru.yandex.practicum.filmorate.storage.Storable;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
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

    ////////////////////////// Обновление коллекций //////////////////////////

    public boolean isGenreNew(long genreId) {
        for (Genre genre : genres) {
            if (genre.getId() == genreId) {
                return false;
            }
        }
        return true;
    }

    public void addGenre(Genre genre) {
        if (genres == null) {
            genres = new ArrayList<>();
        }
        genres.add(genre);
    }

    public void addLike(long userId) {
        if (likes == null) {
            likes = new HashSet<>();
        }
        likes.add(userId);
    }

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

    //распаковка строки таблицы films в фильм (без связей)
    public static Film mapRowToFilm(ResultSet resultSet, int rowNum) throws SQLException {
        Film film = Film.builder()
                .id(resultSet.getLong("films.id"))
                .name(resultSet.getString("films.name"))
                .description(resultSet.getString("films.description"))
                .releaseDate(resultSet.getDate("films.release_date").toLocalDate())
                .duration(resultSet.getInt("films.duration"))
                .mpa(Mpa.builder()
                        .id(resultSet.getLong("films.mpa_id"))
                        .name(null)
                        .build())
                .build();
        film.setLikes(new HashSet<>());
        film.setGenres(new ArrayList<>());
        return film;
    }

    //распаковка строки со всеми связями в фильм
    public static void storeFullRow(ResultSet rs, Map<Long, Film> map) throws SQLException {
        Film film;
        //читаем идентификатор
        long filmId = rs.getLong("films.id");
        if (!map.containsKey(filmId)) { //фильма еще не было в map
            film = mapRowToFilm(rs, 0); //создаем его из набора
        } else { //он уже был
            film = map.get(filmId); //читаем его из map
        }
        //подгружаем имя рейтинга
        film.getMpa().setName(rs.getString("mpa.name"));
        //читаем из сводной таблицы лайк
        long userId = rs.getLong("likes.user_id");
        if (userId > 0) { //он есть
            film.addLike(userId); //добавляем его к фильму
        }
        //читаем из сводной таблицы жанр
        long genreId = rs.getLong("genres.id");
        if ((genreId > 0) && (film.isGenreNew(genreId))) { //он есть и новый
            //создаем объект-жанр
            Genre genre = Genre.builder()
                    .id(genreId)
                    .name(rs.getString("genres.name"))
                    .build();
            //добавляем его к фильму
            film.addGenre(genre);
        }
        //пишем фильм в хранилище
        map.put(filmId, film);
    }
}
