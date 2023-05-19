package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import java.util.HashSet;
import java.util.Optional;

@Component
public class ImMemoryFilmStorage extends InMemoryStorage<Film> {
    @Override
    public Optional<Film> create(Film film) {
        if (film.getLikes() == null) { //список лайков не инициализирован
            film.setLikes(new HashSet<>()); //создаем его
        }
        return super.create(film); //вызываем общий метод создания объекта
    }

    @Override
    public Optional<Film> update(Film film) {
        if (film.getLikes() == null) { //список лайков не инициализирован
            film.setLikes(new HashSet<>()); //создаем его
        }
        return super.update(film); //вызываем общий метод создания объекта
    }
}
