package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping
@Validated
public class FilmController {
    private final FilmService service;

    @Autowired
    public FilmController(FilmService service) {
        this.service = service;
    }

    ///////////////////////////// Получение данных ///////////////////////////

    //получение всех фильмов
    @GetMapping(value = "/films")
    public List<Film> getAllFilms() {
        return service.getAll();
    }

    //получение фильма по идентификатору
    @GetMapping(value = "/films/{id}")
    public Film getFilm(@PathVariable("id") long filmId) {
        return service.get(filmId);
    }

    //получение лайков фильма по идентификатору
    @GetMapping(value = "/films/{id}/like")
    public List<User> getLikes(@PathVariable("id") long filmId) {
        return service.getLikes(filmId);
    }

    //получение всех жанров
    @GetMapping(value = "/genres")
    public List<Genre> getAllGenres() {
        return service.getAllGenres();
    }

    //получение жанра по идентификатору
    @GetMapping(value = "/genres/{id}")
    public Genre getGenre(@PathVariable("id") long filmId) {
        return service.getGenre(filmId);
    }

    //получение всех рейтингов
    @GetMapping(value = "/mpa")
    public List<Mpa> getAllMpa() {
        return service.getAllMpa();
    }

    //получение рейтинга по идентификатору
    @GetMapping(value = "/mpa/{id}")
    public Mpa getMpa(@PathVariable("id") long filmId) {
        return service.getMpa(filmId);
    }

    //получение топовых фильмов
    @GetMapping(value = "/films/popular")
    public List<Film> getTopFilms(@RequestParam(defaultValue = "10") @Positive long count) {
        return service.getTopFilms(count);
    }

    ////////////////////////////// Передача данных ///////////////////////////

    @PostMapping(value = "/films")
    public Film createFilm(@Valid @RequestBody Film film) {
        return service.create(film);
    }

    @PutMapping(value = "/films")
    public Film updateFilm(@Valid @RequestBody Film film) {
        return service.update(film);
    }

    //добавление лайка фильму
    @PutMapping(value = "/films/{id}/like/{userId}")
    public void addLike(@PathVariable long id, @PathVariable @Positive long userId) {
        service.addLike(id, userId);
    }

    ////////////////////////////// Удаление данных ///////////////////////////

    //удаление лайка с фильма
    @DeleteMapping(value = "/films/{id}/like/{userId}")
    public void deleteLike(@PathVariable long id, @PathVariable long userId) {
        service.deleteLike(id, userId);
    }

    //удаление всех фильмов (нужно для тестов)
    @DeleteMapping(value = "/films")
    public void deleteAll() {
        service.deleteAll();
    }
}
