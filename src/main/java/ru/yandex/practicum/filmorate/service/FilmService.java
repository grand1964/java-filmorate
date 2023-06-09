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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate BASE_DATE = LocalDate.of(1895, 12, 28);
    private final FilmStorage storage;

    @Autowired
    public FilmService(FilmStorage storage) {
        this.storage = storage;
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    ////////////////////////////// Чтение фильмов ////////////////////////////

    //получение фильма по идентификатору
    public Film get(long id) {
        //создаем фильм и читаем его данные из базы
        Film film = storage.get(id).orElseThrow(() -> { //фильма нет, ошибка
            log.error("Задан ошибочный идентификатор: " + id);
            return new IncorrectParameterException("Задан ошибочный идентификатор: ", id);
        });
        //загружаем жанр, рейтинг и лайки
        loadFilmLinks(film);
        //возвращаем фильм
        return film;
    }

    //получение списка всех фильмов
    public List<Film> getAll() {
        //читаем все фильмы (без связей)
        List<Film> films = storage.getAll();
        //загружаем их жанры, рейтинги и лайки
        for (Film film : films) {
            loadFilmLinks(film);
        }
        //возвращаем список
        return films;
    }

    ////////////////////////////// Запись фильмов ////////////////////////////

    //добавление фильма
    public Film create(Film film) {
        //проверяем корректность фильма
        validate(film);
        //фильм с существующим идентификатором не допускается
        long id = film.getId();
        if (storage.get(id).isPresent()) {
            log.error("Фильм с идентификатором " + id + " уже существует.");
            throw new ObjectAlreadyExistException(id);
        }
        //создаем в базе фильм с правильным id
        storage.create(film);
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
        if (!storage.update(film)) { //ошибка, фильма нет
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
        boolean result = storage.delete(id);
        if (!result) {
            log.warn("Фильм " + id + " не найден или уже удален.");
        }
        return result;
    }

    public int deleteAll() {
        int count = storage.deleteAll();
        log.info("Удалено " + count + " фильмов.");
        return count;
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Действия с фильмами //////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //////////////////////////// Поддержка лайков ////////////////////////////

    //добавление лайка
    public void addLike(long filmId, long userId) {
        //проверяем корректность идентификатора пользователя
        validateUserId(userId);
        //проверяем существование фильма
        if (storage.get(filmId).isEmpty()) { //его нет
            badFilm(filmId); //ошибка
        }
        //добавляем лайк
        if (storage.addLike(filmId, userId)) { //лайк добавлен
            log.info("Пользователь " + userId + " добавил лайк фильму " + filmId);
        } else { //добавить не удалось
            log.warn("Пользователь " + userId + " уже ставил лайк фильму " + filmId);
        }
    }

    //получение списка лайков
    public List<User> getLikes(long filmId) {
        if (storage.get(filmId).isEmpty()) {
            badFilm(filmId);
        }
        log.info("Получен список лайков фильма " + filmId);
        return storage.getLikes(filmId);
    }

    //удаление лайка
    public boolean deleteLike(long filmId, long userId) {
        validateUserId(userId);
        boolean result = storage.deleteLike(filmId, userId);
        if (result) {
            log.info("Пользователь " + userId + " удалил лайк с фильма " + filmId);
        } else {
            log.warn("Пользователь " + userId + " не ставил лайк фильму " + filmId);
        }
        return result;
    }

    //Получение 10 топовых фильмов
    public List<Film> getTopFilms(Long count) {
        List<Film> films = storage.getTopFilms(count);
        for (Film film : films) {
            loadFilmLinks(film);
        }
        return films;
    }

    //////////////////////////// Поддержка жанров ////////////////////////////

    //выдает жанр по идентификатору
    public Genre getGenre(long genreId) {
        return storage.getGenre(genreId).orElseThrow(() -> {
            String message = "Жанр с идентификатором %d не найден.";
            log.error(String.format(message, genreId));
            return new IncorrectParameterException(message, genreId);
        });
    }

    //выдает все доступные жанры
    public List<Genre> getAllGenres() {
        return storage.getAllGenres();
    }

    /////////////////////////// Поддержка рейтингов //////////////////////////

    //выдает рейтинг по идентификатору
    public Mpa getMpa(long mpaId) {
        return storage.getMpa(mpaId).orElseThrow(() -> {
            String message = "Рейтинг с идентификатором %d не найден.";
            log.error(String.format(message, mpaId));
            return new IncorrectParameterException(message, mpaId);
        });
    }

    //выдает все доступные рейтинги
    public List<Mpa> getAllMpa() {
        return storage.getAllMpa();
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Оперирование связями /////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //загружает связи фильма из других таблиц
    private void loadFilmLinks(Film film) {
        //загрузка жанров
        film.setGenres(storage.getFilmGenres(film.getId()));
        //загрузка рейтингов
        storage.getMpa(film.getMpa().getId()).ifPresent(film::setMpa);
        //загрузка лайков
        film.setLikes(storage.getLikeIds(film.getId()));
    }

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
        storage.deleteFilmGenres(film.getId()); //удаляем их
        if (genres == null) { //жанры не инициализированы
            film.setGenres(new ArrayList<>()); //создаем пустой массив
        } else { //жанры есть
            for (Genre genre : genres) { //пишем в базу их связи с фильмом
                storage.addFilmGenre(film.getId(), genre.getId());
            }
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
        Mpa mpaStored = storage.getMpa(mpa.getId()).orElseThrow(() -> {
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
        storage.deleteAllLikes(filmId);
        //устанавливаем новые
        for (Long id : likes) {
            storage.addLike(filmId, id); //добавляем лайк фильму
        }
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
        validateLikes(film);
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
        //проверяем корректность каждого из жанров
        for (Genre genre : genres) {
            //если жанр не задан или отсутствует в базе - ошибка
            if ((genre == null) || storage.getGenre(genre.getId()).isEmpty()) {
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

    //валидация набора лайков
    private void validateLikes(Film film) {
        //проверяем корректность лайков
        if (film.getLikes() == null) { //лайков нет
            return; //это нормально
        }
        for (Long likeId : film.getLikes()) {
            validateUserId(likeId); //проверяем корректность идентификаторов пользователя
        }
    }

    //диагностика ошибочного идентификатора
    private void badFilm(long id) {
        String message = "Фильм с идентификатором %d не найден.";
        log.error(String.format(message, id));
        throw new IncorrectParameterException(message, id);
    }

    //валидация идентификатора пользователя
    private void validateUserId(long id) {
        if (id <= 0) { //такого идентификатора быть не может
            String message = "Некорректный идентификатор пользователя: ";
            log.error(message + id);
            throw new IncorrectParameterException(message, id);
        }
    }
}
