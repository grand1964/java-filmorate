package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@RestController
@RequestMapping(value = "/mpa")
public class MpaController {
    private final FilmService service;

    @Autowired
    public MpaController(FilmService service) {
        this.service = service;
    }

    ///////////////////////////// Получение данных ///////////////////////////

    //получение всех рейтингов
    @GetMapping
    public List<Mpa> getAllMpa() {
        return service.getAllMpa();
    }

    //получение рейтинга по идентификатору
    @GetMapping(value = "/{id}")
    public Mpa getMpa(@PathVariable("id") long filmId) {
        return service.getMpa(filmId);
    }
}
