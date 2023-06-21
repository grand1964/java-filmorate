package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.util.TestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DbFilmTests {
    private static final int FILM_COUNT = 10;
    private static final int USER_COUNT = 10;
    private final FilmService service;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void resetDatabase() {
        jdbcTemplate.update(TestUtils.getSqlForResetUsers(USER_COUNT));
        jdbcTemplate.update(TestUtils.getSqlForResetFilms(FILM_COUNT));
    }

    @Test
    public void getNotExistingFilmTest() {
        assertThrows(IncorrectParameterException.class, () -> service.get(17));
    }

    @Test
    public void getExistingFilmTest() {
        long filmId = 1;
        service.addLike(filmId, 2);
        service.addLike(filmId, 3);
        service.addLike(filmId, 4);
        //получаем фильм
        Film film = service.get(filmId);
        //проверяем его id
        assertEquals(film.getId(), filmId);
        //проверяем лайки
        Set<Long> likes = film.getLikes();
        assertEquals(likes.size(), 3); //из 3
        for (long i = 2; i < 5; i++) { //проверяем их id
            assertTrue(likes.contains(i));
        }
    }

    @Test
    public void getAllFilmsTest() {
        service.addLike(1, 2);
        service.addLike(2, 3);
        service.addLike(2, 4);
        List<Film> films = service.getAll();
        assertEquals(films.size(), FILM_COUNT);
        for (int i = 0; i < FILM_COUNT; i++) {
            assertEquals(films.get(i).getId(), i + 1);
        }
        assertEquals(films.get(0).getLikes().size(), 1);
        assertEquals(films.get(1).getLikes().size(), 2);
    }

    @Test
    public void createFilmWithExistingIdTest() {
        //создаем фильм с существующим номером
        Film film = TestUtils.generateFilm(1);
        assertThrows(ObjectAlreadyExistException.class, () -> service.create(film));
    }

    @Test
    public void createNewFilmTest() {
        //создаем фильм
        Film film = TestUtils.generateFilm(17);
        //ставим на него лайки
        Set<Long> likes = new HashSet<>();
        likes.add(2L);
        likes.add(3L);
        likes.add(4L);
        film.setLikes(likes);
        //добавляем жанр
        Genre genre = Genre.builder().id(1).name("Комедия").build();
        film.addGenre(genre);
        //помещаем фильм в базу
        service.create(film);
        //тесты films
        assertEquals(film.getId(), FILM_COUNT + 1); //id должен быть сгенерирован
        List<Film> films = service.getAll();
        assertEquals(films.size(), FILM_COUNT + 1); //число фильмов в базе увеличилось на 1
        //тесты likes
        long filmId = film.getId();
        List<User> allLikes = service.getLikes(filmId); //все лайки
        assertEquals(allLikes.size(), 3); //их трое
        for (int i = 0; i < 3; i++) { //проверяем их id
            assertEquals(allLikes.get(i).getId(), i + 2);
        }
    }

    @Test
    public void updateNotExistingFilmTest() {
        //создаем фильм с несуществующим номером
        Film film = TestUtils.generateFilm(17);
        assertThrows(ObjectNotExistException.class, () -> service.update(film));
    }

    @Test
    public void updateExistingFilmTest() {
        int filmId = 1;
        //ставим новые лайки на фильм 1
        service.addLike(filmId, 7);
        service.addLike(filmId, 8);
        service.addLike(filmId, 9);
        service.addLike(9, filmId);
        //создаем новый фильм с идентификатором 1
        Film film = TestUtils.generateFilm(filmId);
        //ставим на него лайки
        Set<Long> likes = new HashSet<>();
        likes.add(2L);
        likes.add(3L);
        likes.add(4L);
        film.setLikes(likes);
        //помещаем фильм в базу
        service.update(film);
        //тесты films
        List<Film> films = service.getAll();
        assertEquals(films.size(), FILM_COUNT); //число фильмов в базе не меняется
        //тесты likes (лайки должны измениться)
        List<User> allLikes = service.getLikes(filmId); //все лайки
        assertEquals(allLikes.size(), 3); //их 3
        for (int i = 0; i < 3; i++) { //проверяем их id
            assertEquals(allLikes.get(i).getId(), i + 2);
        }
    }

    @Test
    public void deleteNotExistingFilmTest() {
        int filmId = 17;
        service.delete(filmId);
        assertFalse(service.delete(filmId));
        assertEquals(service.getAll().size(), FILM_COUNT); //число фильмов не меняется
    }

    @Test
    public void deleteExistingFilmTest() {
        int filmId = 1;
        assertTrue(service.delete(filmId));
        //число фильмов уменьшается на 1
        assertEquals(service.getAll().size(), FILM_COUNT - 1);
        //проверка, что фильма больше нет в базе
        assertThrows(IncorrectParameterException.class, () -> service.get(filmId));
    }

    @Test
    public void deleteAllFilmsTest() {
        int count = service.deleteAll();
        assertEquals(count, FILM_COUNT);
        assertEquals(service.getAll().size(), 0);
    }

    @Test
    public void addLikesToNotExistingFilmTest() {
        assertThrows(IncorrectParameterException.class, () -> service.addLike(17, 5));
    }

    @Test
    public void addLikesToExistingFilmTest() {
        long filmId = 1;
        service.addLike(filmId, 5);
        service.addLike(filmId, 7);
        service.addLike(filmId, 9);
        List<User> likes = service.getLikes(filmId);
        assertEquals(likes.size(), 3);
        for (int i = 0; i < 3; i++) { //проверяем id друзей
            assertEquals(likes.get(i).getId(), 2 * i + 5);
        }
    }

    @Test
    public void getLikesOfNotExistingFilmTest() {
        assertThrows(IncorrectParameterException.class, () -> service.getLikes(17));
    }

    @Test
    public void getLikesOfExistingFilmTest() {
        long filmId = 3;
        service.addLike(filmId, 1);
        service.addLike(filmId, 3);
        service.addLike(filmId, 5);
        List<User> likes = service.getLikes(filmId);
        assertEquals(likes.size(), 3);
        Set<Long> likesExpected = Set.of(1L, 3L, 5L); //ожидаемые идентификаторы
        Set<Long> likesActual = new HashSet<>(); //полученные пользователи
        for (User like : likes) {
            likesActual.add(like.getId()); //их идентификаторы
        }
        assertEquals(likesActual, likesExpected); //сравниваем
    }

    @Test
    public void deleteLikeTest() {
        service.addLike(1, 2);
        assertTrue(service.deleteLike(1, 2));
        assertEquals(service.getLikes(1).size(), 0);
        assertFalse(service.deleteLike(1, 2)); //удаляем еще раз
    }

    @Test
    public void getTopFilmsTest() {
        long count = FILM_COUNT - 2;
        //вначале расставляем лайки
        for (int filmId = 1; filmId <= count; filmId++) {
            for (int userId = 1; userId <= filmId; userId++) {
                service.addLike(filmId, userId);
            }
        }
        List<Film> top = service.getTopFilms(count);
        assertEquals(top.size(), count); //фильмы без лайков не учитываются
        for (int filmId = 0; filmId < count; filmId++) { //первые фильмы идут по убыванию id
            assertEquals(top.get(filmId).getId(), count - filmId);
        }
    }
}
