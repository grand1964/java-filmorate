package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterFormatException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService service;

    @Autowired
    public FilmController(FilmService service) {
        this.service = service;
    }

    ///////////////////////////// Получение данных ///////////////////////////

    //получение всех фильмов
    @GetMapping
    public List<Film> getAllFilms() {
        return service.getAll();
    }

    //получение фильма по идентификатору
    @GetMapping(value = "/{id}")
    public Film getFilm(@PathVariable("id") String filmId) {
        return service.get(parseNumberParam(filmId));
    }

    //получение топовых фильмов
    @GetMapping(value = "/popular")
    public List<Film> getTopFilms(
            @RequestParam(defaultValue = "10") String count) {
        return service.getTopFilms(parseNumberParam(count));
    }

    ////////////////////////////// Передача данных ///////////////////////////

    @PostMapping
    public Film createFilm(@Valid @RequestBody Film film) {
        return service.create(film);
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        return service.update(film);
    }

    //добавление лайка фильму
    @PutMapping(value = "/{id}/like/{userId}")
    public void addLike(@PathVariable String id, @PathVariable String userId) {
        service.addLike(parseNumberParam(id), parseNumberParam(userId));
    }

    ////////////////////////////// Удаление данных ///////////////////////////

    //удаление лайка с фильма
    @DeleteMapping(value = "/{id}/like/{userId}")
    public Film deleteLike(@PathVariable String id, @PathVariable String userId) {
        return service.deleteLike(parseNumberParam(id), parseNumberParam(userId));
    }

    //удаление всех фильмов (нужно для тестов)
    @DeleteMapping
    public void deleteAll() {
        service.deleteAll();
    }

    ////////////////////////// Конвертация параметров ////////////////////////

    protected long parseNumberParam(String param) {
        try {
            return Long.parseLong(param);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterFormatException("Задан нечисловой параметр: ", param);
        }
    }
}
