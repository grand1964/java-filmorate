package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    Optional<Genre> getGenre(long genreId);

    List<Genre> getAllGenres();

    List<Long> getAllGenreIds();

    List<Genre> getFilmGenres(long filmId);

    void setFilmGenres(Long filmId, List<Genre> genres);

    void deleteFilmGenres(long filmId);
}
