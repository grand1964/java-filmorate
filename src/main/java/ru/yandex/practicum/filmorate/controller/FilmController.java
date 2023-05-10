package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private static final LocalDate BASE_DATE = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films;
    private long currentId;

    public FilmController() {
        currentId = 0;
        films = new HashMap<>();
    }

    ///////////////////////////// Обработчики REST ///////////////////////////

    @GetMapping
    public List<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    @PostMapping
    public Film createFilm(@Valid @RequestBody Film film) {
        validateFilm(film);
        if (films.containsKey(film.getId())) {
            log.error("Фильм с идентификатором " + film.getId() + " уже существует.");
            throw new ObjectAlreadyExistException("Film", film.getId());
        }
        film.setId(++currentId);
        films.put(currentId, film);
        log.info("Добавлен новый фильм: " + film);
        return film;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        validateFilm(film);
        if (!films.containsKey(film.getId())) {
            log.error("Фильма с идентификатором " + film.getId() + " не существует.");
            throw new ObjectNotExistException("Film", film.getId());
        }
        films.put(film.getId(), film);
        log.info("Фильм с идентификатором " + film.getId() + " заменен на " + film);
        return film;
    }

    /////////////////////////////// Валидаторы ///////////////////////////////

    private void validateDescription(String description) {
        if ((description != null) && (description.length() > 200)) {
            log.error("Ошибка валидации.");
            throw new ValidateException("Описание фильма не должно быть длиннее 200 символов.");
        }
    }

    private void validateRelease(LocalDate release) {
        if (release.isBefore(BASE_DATE)) {
            log.error("Ошибка валидации.");
            throw new ValidateException("Релиз не может быть раньше " + BASE_DATE);
        }
    }

    private void validateFilm(Film film) {
        validateDescription(film.getDescription());
        validateRelease(film.getReleaseDate());
    }
}
