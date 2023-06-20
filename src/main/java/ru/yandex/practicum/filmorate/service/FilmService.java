package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.LikeStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate BASE_DATE = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    private final LikeStorage likeStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, GenreStorage genreStorage,
                       MpaStorage mpaStorage, LikeStorage likeStorage) {
        this.filmStorage = filmStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
        this.likeStorage = likeStorage;
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    ////////////////////////////// Чтение фильмов ////////////////////////////

    //получение фильма по идентификатору
    public Film get(long id) {
        //создаем фильм и читаем все его данные из базы
        return filmStorage.get(id).orElseThrow(() -> { //фильма нет, ошибка
            log.error("Задан ошибочный идентификатор: " + id);
            return new IncorrectParameterException("Задан ошибочный идентификатор: ", id);
        });
    }

    //получение всех фильмов
    public List<Film> getAll() {
        //читаем все фильмы со связями
        return filmStorage.getAll();
    }

    ////////////////////////////// Запись фильмов ////////////////////////////

    //добавление фильма
    public Film create(Film film) {
        //проверяем корректность фильма
        validate(film);
        //фильм с существующим идентификатором не допускается
        long id = film.getId();
        if (filmStorage.contains(id)) {
            log.error("Фильм с идентификатором " + id + " уже существует.");
            throw new ObjectAlreadyExistException(id);
        }
        //создаем в базе фильм с правильным id
        filmStorage.create(film);
        //сохраняем в базе жанр, рейтинг и лайки
        storeFilmLinks(film);
        //возвращаем фильм
        return film;
    }

    //обновление фильма
    public Film update(Film film) {
        //проверяем корректность фильма
        validate(film);
        //обновляем данные фильма
        long id = film.getId();
        //обновляем вначале все поля из таблицы film
        if (!filmStorage.update(film)) { //ошибка, фильма нет
            log.error("фильма с идентификатором " + id + " не существует.");
            throw new ObjectNotExistException(id);
        }
        //если удалось - обновляем связи фильма в базе
        storeFilmLinks(film);
        //возвращаем фильм
        return film;
    }

    ///////////////////////////// Удаление фильмов ///////////////////////////

    //удаление по идентификатору
    public boolean delete(long id) {
        boolean result = filmStorage.delete(id);
        if (!result) {
            log.warn("Фильм " + id + " не найден или уже удален.");
        }
        return result;
    }

    public int deleteAll() {
        int count = filmStorage.deleteAll();
        log.info("Удалено " + count + " фильмов.");
        return count;
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Действия с фильмами //////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //////////////////////////// Поддержка лайков ////////////////////////////

    //добавление лайка
    public void addLike(long filmId, long userId) {
        //проверяем существование фильма
        if (!filmStorage.contains(filmId)) { //его нет
            badFilm(filmId); //ошибка
        }
        //добавляем лайк
        if (likeStorage.addLike(filmId, userId)) { //лайк добавлен
            log.info("Пользователь " + userId + " добавил лайк фильму " + filmId);
        } else { //добавить не удалось
            log.warn("Пользователь " + userId + " уже ставил лайк фильму " + filmId);
        }
    }

    //получение списка лайков
    public List<User> getLikes(long filmId) {
        if (filmStorage.get(filmId).isEmpty()) {
            badFilm(filmId);
        }
        log.info("Получен список лайков фильма " + filmId);
        return likeStorage.getLikes(filmId);
    }

    //удаление лайка
    public boolean deleteLike(long filmId, long userId) {
        if (userId <= 0) { //такого идентификатора быть не может
            String message = "Некорректный идентификатор пользователя: ";
            log.error(message + userId);
            throw new IncorrectParameterException(message, userId);
        }
        boolean result = likeStorage.deleteLike(filmId, userId);
        if (result) {
            log.info("Пользователь " + userId + " удалил лайк с фильма " + filmId);
        } else {
            log.warn("Пользователь " + userId + " не ставил лайк фильму " + filmId);
        }
        return result;
    }

    //Получение 10 топовых фильмов
    public List<Film> getTopFilms(Long count) {
        return likeStorage.getTopFilms(count);
    }

    //////////////////////////// Поддержка жанров ////////////////////////////

    //выдает жанр по идентификатору
    public Genre getGenre(long genreId) {
        return genreStorage.getGenre(genreId).orElseThrow(() -> {
            String message = "Жанр с идентификатором %d не найден.";
            log.error(String.format(message, genreId));
            return new IncorrectParameterException(message, genreId);
        });
    }

    //выдает все доступные жанры
    public List<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    /////////////////////////// Поддержка рейтингов //////////////////////////

    //выдает рейтинг по идентификатору
    public Mpa getMpa(long mpaId) {
        return mpaStorage.getMpa(mpaId).orElseThrow(() -> {
            String message = "Рейтинг с идентификатором %d не найден.";
            log.error(String.format(message, mpaId));
            return new IncorrectParameterException(message, mpaId);
        });
    }

    //выдает все доступные рейтинги
    public List<Mpa> getAllMpa() {
        return mpaStorage.getAllMpa();
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Оперирование связями /////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //обновляет связи фильма в других таблицах
    private void storeFilmLinks(Film film) {
        //обработка жанра
        storeFilmGenres(film);
        //обработка рейтинга
        storeFilmMpa(film);
        //обработка лайков
        storeFilmLikes(film);
    }

    //сохранение жанров фильма
    private void storeFilmGenres(Film film) {
        //читаем текущие жанры (их различие обеспечивается на этапе валидации)
        List<Genre> genres = film.getGenres();
        genreStorage.deleteFilmGenres(film.getId()); //удаляем их
        if (genres == null) { //жанры не инициализированы
            film.setGenres(new ArrayList<>()); //создаем пустой массив
        } else { //жанры есть
            genreStorage.setFilmGenres(film.getId(), genres);
        }
    }

    //сохранение рейтинга фильма
    private void storeFilmMpa(Film film) {
        //читаем текущий рейтинг фильма(он содержит идентификатор)
        Mpa mpa = film.getMpa();
        if (mpa.getId() == 0) { //рейтинг не задан
            return; //обработка не требуется
        }
        //читаем из базы полный рейтинг (если его нет - ошибка)
        Mpa mpaStored = mpaStorage.getMpa(mpa.getId()).orElseThrow(() -> {
            String message = "У фильма %d некорректный рейтинг.";
            log.error(String.format(message, film.getId()));
            return new IncorrectParameterException(message, film.getId());
        });
        //присваиваем его фильму
        film.setMpa(mpaStored);
    }

    //сохраняет в базе лайки фильма
    private void storeFilmLikes(Film film) {
        //читаем текущие лайки фильма
        long filmId = film.getId();
        Set<Long> likes = film.getLikes();
        if (likes == null) { //их нет
            likes = new HashSet<>(); //создаем пустое множество лайков
            film.setLikes(likes); //устанавливаем его
        }
        //удаляем все лайки, связанные с фильмом
        likeStorage.deleteAllLikes(filmId);
        //устанавливаем новые
        likeStorage.storeAllLikes(filmId, likes);
    }

    //////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Валидация ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //проверки фильма перед операциями над ним
    private void validate(Film film) {
        validateDescription(film.getDescription());
        validateRelease(film.getReleaseDate());
        validateGenres(film);
        validateMpa(film);
    }

    //валидация описания
    private void validateDescription(String description) {
        if ((description != null) && (description.length() > 200)) {
            String message = "Описание фильма не должно быть длиннее 200 символов.";
            log.error(message);
            throw new ValidateException(message);
        }
    }

    //валидация даты выпуска
    private void validateRelease(LocalDate release) {
        if (release.isBefore(BASE_DATE)) {
            String message = "Релиз не может быть раньше " + BASE_DATE;
            log.error(message);
            throw new ValidateException(message);
        }
    }

    //валидация списка жанров фильма
    private void validateGenres(Film film) {
        //читаем текущие жанры фильма
        List<Genre> genres = film.getGenres();
        if (genres == null) { //жанры не заданы
            film.setGenres(new ArrayList<>()); //создаем пустое множество
            return; //это нормально
        }
        //убираем жанры-двойники и упорядочиваем их по идентификатору
        film.setGenres(genres.stream()
                .distinct()
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toList()));
        //проверяем корректность жанров
        List<Long> storedGenreIds = genreStorage.getAllGenreIds(); //читаем идентификаторы всех жанров из базы
        for (Genre genre : genres) { //жанры фильма должны быть среди них
            if ((genre == null) || !storedGenreIds.contains(genre.getId())) { //найден недопустимый жанр
                String message = "У фильма %d обнаружен некорректный жанр.";
                log.error(String.format(message, film.getId()));
                throw new IncorrectParameterException(message, film.getId());
            }
        }
    }

    //валидация рейтинга
    private void validateMpa(Film film) {
        if (film.getMpa() == null) { //рейтинг не задан
            film.setMpa(Mpa.builder()
                    .id(0)
                    .name("")
                    .build()); //создаем фиктивный рейтинг
        }
    }

    //диагностика ошибочного идентификатора
    private void badFilm(long id) {
        String message = "Фильм с идентификатором %d не найден.";
        log.error(String.format(message, id));
        throw new IncorrectParameterException(message, id);
    }
}
