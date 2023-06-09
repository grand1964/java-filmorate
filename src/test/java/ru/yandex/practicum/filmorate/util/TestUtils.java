package ru.yandex.practicum.filmorate.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TestUtils {

    ///////////////////////// Поддержка пользователей ////////////////////////

    //создает пользователя с заданным номером
    public static User generateUser(long number) {
        User user = User.builder()
                .id(number)
                .login("user" + number)
                .name("name" + number)
                .email("user" + number + "@yandex.ru")
                .birthday(LocalDate.of(1940, 12, 9))
                .build();
        user.setFriends(new HashMap<>());
        return user;
    }

    //создает коллекцию пользователей
    public static List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(generateUser(i + 1));
        }
        return users;
    }

    //сравнивает двух пользователей без учета id
    public static boolean compareUsers(User user1, User user2) {
        long oldId = user2.getId();
        user2.setId(user1.getId());
        boolean result = user1.equals(user2);
        user2.setId(oldId);
        return result;
    }

    //сравнивает пользователя с его json-представлением без учета id
    public static boolean compareUserWithJson(User user, String json, Gson gson) {
        User jsonUser = gson.fromJson(json, User.class);
        jsonUser.setId(user.getId());
        return user.equals(jsonUser);
    }

    //распаковывает json-представление массива пользователей
    public static List<User> jsonToUserList(String json, Gson gson) {
        TypeToken<List<User>> listType = new TypeToken<>() {
        };
        return gson.fromJson(json, listType);
    }

    public static String getSqlForResetUsers(int count) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DROP TABLE users, friends CASCADE; ");
        try {
            stringBuilder.append(new String(Files.readAllBytes(
                    Path.of("./src/main/resources/schema.sql"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 1; i <= count; i++) {
            stringBuilder.append("INSERT INTO users (login, name, email, birthday) ");
            stringBuilder.append(String.format(
                    "VALUES ('user%d', 'name%d', 'user%d@yandex.ru', '1940-12-09');", i, i, i));
        }
        return stringBuilder.toString();
    }

    //////////////////////////// Поддержка фильмов ///////////////////////////

    //создает фильм с заданным номером
    public static Film generateFilm(long number) {
        Film film = Film.builder()
                .id(number)
                .name("Film" + number)
                .description("Description" + number)
                .releaseDate(LocalDate.of(1940, 12, 9))
                .duration(17)
                .build();
        film.setLikes(new HashSet<>());
        return film;
    }

    //создает коллекцию фильмов
    public static List<Film> generateFilms(int count) {
        List<Film> films = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            films.add(generateFilm(i + 1));
        }
        return films;
    }

    //сравнивает два фильма без учета id и объектных полей
    public static boolean compareFilms(Film film1, Film film2) {
        if (!film1.getName().equals(film2.getName())) {
            return false;
        }
        if (!film1.getDescription().equals(film2.getDescription())) {
            return false;
        }
        if (!film1.getReleaseDate().equals(film2.getReleaseDate())) {
            return false;
        }
        return film1.getDuration() == film2.getDuration();
    }

    //сравнивает фильм с его json-представлением без учета id и объектных полей
    public static boolean compareFilmWithJson(Film film, String json, Gson gson) {
        Film jsonFilm = gson.fromJson(json, Film.class);
        jsonFilm.setId(film.getId());
        return compareFilms(film, jsonFilm);
        //return film.equals(jsonFilm);
    }

    //распаковывает json-представление массива фильмов
    public static List<Film> jsonToFilmList(String json, Gson gson) {
        TypeToken<List<Film>> listType = new TypeToken<>() {
        };
        return gson.fromJson(json, listType);
    }

    public static String getSqlForResetFilms(int count) {
        StringBuilder stringBuilder = new StringBuilder();
        //удаляем таблицу films, а также связанные с ними likes и film_genre
        stringBuilder.append("DROP TABLE films, likes, film_genres CASCADE; ");
        //воссоздаем эти таблицы из схемы (уже пустые)
        try {
            stringBuilder.append(new String(Files.readAllBytes(
                    Path.of("./src/main/resources/schema.sql"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 1; i <= count; i++) {
            //вставляем в films очередной фильм
            stringBuilder.append(
                    "INSERT INTO films (name, description, release_date, duration, mpa_id) ");
            stringBuilder.append(String.format(
                    "VALUES ('name%d', 'description%d', '1940-12-09', 17, %d);", i, i, i % 5 + 1));
            //вставляем в film_genres жанр для него
            stringBuilder.append(
                    "INSERT INTO film_genres (film_id, genre_id) ");
            stringBuilder.append(String.format(
                    "VALUES (%d, %d);", i, i % 6 + 1));
        }
        return stringBuilder.toString();
    }
}
