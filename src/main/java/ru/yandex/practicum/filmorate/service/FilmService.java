package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterFormatException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.Storage;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService extends AbstractService<Film> {
    private static final LocalDate BASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(Storage<Film> storage) {
        super(storage);
    }

    //добавление лайка
    public void addLike(long filmId, long userId) {
        validateFimId(filmId);
        validateUserId(userId);
        if (storage.get(filmId).orElseThrow().getLikes().add(userId)) {
            log.info("Пользователь " + userId + " добавил лайк фильму " + filmId);
        } else {
            log.warn("Пользователь " + userId + " уже ставил лайк фильму " + filmId);
        }
    }

    //удаление лайка
    public Film deleteLike(long filmId, long userId) {
        validateFimId(filmId);
        validateUserId(userId);
        Film film = storage.get(filmId).orElseThrow();
        if (film.getLikes().contains(userId)) {
            film.getLikes().remove(userId);
            log.info("Пользователь " + userId + " удалил лайк с фильма " + filmId);
        } else {
            log.warn("Пользователь " + userId + " не ставил лайк фильму " + filmId);
        }
        return film;
    }

    //Получение 10 топовых фильмов
    public List<Film> getTopFilms(Long count) {
        if (count < 1) {
            String message = "Неверное количество фильмов: ";
            log.error(message + count);
            throw new IncorrectParameterFormatException(message, count.toString());
        }
        List<Film> list = storage.getAll();
        Comparator<Film> comparator = Comparator.comparingLong(a -> a.getLikes().size());
        return list
                .stream()
                .sorted(comparator.reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    //////////////////////////////// Валидация ///////////////////////////////

    @Override
    protected void validate(Film film) {
        validateDescription(film.getDescription());
        validateRelease(film.getReleaseDate());
    }

    private void validateDescription(String description) {
        if ((description != null) && (description.length() > 200)) {
            String message = "Описание фильма не должно быть длиннее 200 символов.";
            log.error(message);
            throw new ValidateException(message);
        }
    }

    private void validateRelease(LocalDate release) {
        if (release.isBefore(BASE_DATE)) {
            String message = "Релиз не может быть раньше " + BASE_DATE;
            log.error(message);
            throw new ValidateException(message);
        }
    }

    private void validateFimId(long id) {
        if (storage.get(id).isEmpty()) {
            String message = "Фильм с идентификатором %d не найден.";
            log.error(String.format(message, id));
            throw new IncorrectParameterException(message, id);
        }
    }

    ////////////////////////// Вспомогательная функция ///////////////////////

    private void validateUserId(long userId) {
        if (userId <= 0) {
            String message = "Некорректный идентификатор пользователя: ";
            log.error(message + userId);
            throw new IncorrectParameterException(message, userId);
        }
    }
}
