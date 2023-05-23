package ru.yandex.practicum.filmorate.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TestUtils {

    ///////////////////////// Поддержка пользователей ////////////////////////

    //создает пользователя с заданным номером
    public static User generateUser(int number) {
        User user = new User();
        user.setLogin("User" + number);
        user.setName(user.getLogin());
        user.setEmail(user.getName() + "@yandex.ru");
        user.setBirthday(LocalDate.of(1940, 12, 9));
        user.setFriends(new HashSet<>());
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

    //////////////////////////// Поддержка фильмов ///////////////////////////

    //создает фильм с заданным номером
    public static Film generateFilm(int number) {
        Film film = new Film();
        film.setName("Film" + number);
        film.setDescription("Description" + number);
        film.setDuration(17);
        film.setReleaseDate(LocalDate.of(1940, 12, 9));
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

    //сравнивает два фильма без учета id
    public static boolean compareFilms(Film film1, Film film2) {
        long oldId = film2.getId();
        film2.setId(film1.getId());
        boolean result = film1.equals(film2);
        film2.setId(oldId);
        return result;
    }

    //сравнивает фильм с его json-представлением без учета id
    public static boolean compareFilmWithJson(Film film, String json, Gson gson) {
        Film jsonFilm = gson.fromJson(json, Film.class);
        jsonFilm.setId(film.getId());
        return film.equals(jsonFilm);
    }

    //распаковывает json-представление массива фильмов
    public static List<Film> jsonToFilmList(String json, Gson gson) {
        TypeToken<List<Film>> listType = new TypeToken<>() {
        };
        return gson.fromJson(json, listType);
    }
}
