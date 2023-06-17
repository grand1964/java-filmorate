package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping(value = "/genres")
@Validated
public class GenreController {
    private final FilmService service;

    @Autowired
    public GenreController(FilmService service) {
        this.service = service;
    }

    ///////////////////////////// Получение данных ///////////////////////////

    //получение всех жанров
    @GetMapping
    public List<Genre> getAllGenres() {
        return service.getAllGenres();
    }

    //получение жанра по идентификатору
    @GetMapping(value = "/{id}")
    public Genre getGenre(@PathVariable("id") long filmId) {
        return service.getGenre(filmId);
    }
}
