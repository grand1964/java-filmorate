package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

public interface FilmStorage extends Storage<Film> {

    //////////////////////////// Поддержка лайков ////////////////////////////

    List<User> getLikes(long filmId);

    Set<Long> getLikeIds(long filmId);

    boolean addLike(long filmId, long userId);

    boolean deleteLike(long filmId, long userId);

    void deleteAllLikes(long filmId);

    //////////////////////////// Поддержка жанров ////////////////////////////

    List<Genre> getFilmGenres(long filmId);

    Optional<Genre> getGenre(long genreId);

    List<Genre> getAllGenres();

    void addFilmGenre(long filmId, long genreId);

    void deleteFilmGenres(long filmId);

    /////////////////////////// Поддержка рейтингов //////////////////////////

    Optional<Mpa> getMpa(long filmId);

    List<Mpa> getAllMpa();

    List<Film> getTopFilms(Long count);
}
