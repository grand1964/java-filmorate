package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
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
    public Film getFilm(@PathVariable("id") String filmId) {
        return service.get(filmId);
    }

    //получение топовых фильмов
    @GetMapping(value = "/films/popular")
    public List<Film> getTopFilms(
            @RequestParam(required = false, defaultValue = "10") String count) {
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
    public void addLike(@PathVariable Map<String, String> params) {
        service.addLike(params.get("id"), params.get("userId"));
    }

    ////////////////////////////// Удаление данных ///////////////////////////

    //удаление лайка с фильма
    @DeleteMapping(value = "/films/{id}/like/{userId}")
    public Film deleteLike(@PathVariable Map<String, String> params) {
        return service.deleteLike(params.get("id"), params.get("userId"));
    }

    //удаление всех фильмов (нужно для тестов)
    @DeleteMapping(value = "/films")
    public void deleteAll() {
        service.deleteAll();
    }
}
