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
    public void addLike(String filmParam, String userParam) {
        long userId = validateUserId(userParam);
        long filmId = validateId(filmParam);
        if (userId == -1) {
            log.error("Пользователь " + userParam + " не найден.");
            throw new IncorrectParameterException("Пользователь %s не найден: ", userParam);
        }
        if (filmId == -1) {
            log.error("Фильм " + filmParam + " не найден.");
            throw new IncorrectParameterException("Фильм %s не найден: ", filmParam);
        }
        if (storage.get(filmId).orElseThrow().getLikes().add(userId)) {
            log.info("Пользователь " + userId + " добавил лайк фильму " + filmId);
        } else {
            log.warn("Пользователь " + userId + " уже ставил лайк фильму " + filmId);
        }
    }

    //удаление лайка
    public Film deleteLike(String filmParam, String userParam) {
        long userId = validateUserId(userParam);
        long filmId = validateId(filmParam);
        if (userId == -1) {
            log.error("Пользователь " + userParam + " не найден.");
            throw new IncorrectParameterException("Пользователь %s не найден: ", userParam);
        }
        if (filmId == -1) {
            log.error("Фильм " + filmParam + " не найден.");
            throw new IncorrectParameterException("Фильм %s не найден: ", filmParam);
        }
        Film film = storage.get(filmId).orElseThrow();
        if (film.getLikes().contains(userId)) {
            log.info("Пользователь " + userId + " удалил лайк с фильма " + filmId);
            film.getLikes().remove(userId);
        } else {
            log.warn("Пользователь " + userId + " не ставил лайк фильму " + filmId);
        }
        return film;
    }

    //Получение 10 топовых фильмов
    public List<Film> getTopFilms(String count) {
        int topCount;
        try {
            topCount = Integer.parseInt(count);
            if (topCount < 1) {
                String message = "Неверное количество фильмов: ";
                log.error(message + count);
                throw new IncorrectParameterFormatException(message, count);
            }
        } catch (NumberFormatException e) {
            String message = "Задан нечисловой параметр: ";
            log.error(message + count);
            throw new IncorrectParameterFormatException(message, count);
        }
        List<Film> list = storage.getAll();
        Comparator<Film> comparator = Comparator.comparingLong(a -> a.getLikes().size());
        return list
                .stream()
                .sorted(comparator.reversed())
                .limit(topCount)
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

    @Override
    protected long validateId(String paramId) {
        long id = super.validateId(paramId);
        if (id == -1) {
            return id;
        }
        if (storage.get(id).isEmpty()) {
            log.error(String.format("Не найден фильм с идентификатором " + paramId));
            return -1;
        }
        return id;
    }

    ////////////////////////// Вспомогательная функция ///////////////////////

    private long validateUserId(String userId) {
        long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.error("Задан нечисловой параметр: " + userId);
            throw new IncorrectParameterFormatException("Задан нечисловой параметр: ", userId);
        }
        if (id <= 0) {
            String message = "Некорректный идентификатор пользователя: ";
            log.error(message + userId);
            throw new IncorrectParameterException("Некорректный идентификатор пользователя: ", userId);
        }
        return id;
    }
}
