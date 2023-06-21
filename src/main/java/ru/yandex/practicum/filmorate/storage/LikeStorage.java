package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Set;

public interface LikeStorage {

    //////////////////////////// Поддержка лайков ////////////////////////////

    List<User> getLikes(long filmId);

    Set<Long> getLikeIds(long filmId);

    boolean addLike(long filmId, long userId);

    void storeAllLikes(Long filmId, Set<Long> likes);

    boolean deleteLike(long filmId, long userId);

    void deleteAllLikes(long filmId);

    List<Film> getTopFilms(Long count);
}
