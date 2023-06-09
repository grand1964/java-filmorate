package ru.yandex.practicum.filmorate.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.filmorate.FilmorateApplication;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.util.ClientRequests;
import ru.yandex.practicum.filmorate.util.LocalDateAdapter;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidationTest extends ClientRequests {
    private static ConfigurableApplicationContext context;
    private static Gson gson;
    private static long id;
    private User user;

    //сравнивает пользователя с его json-представлением без учета id
    private boolean compareUsers(User user, String response) {
        User responseUser = gson.fromJson(response, User.class);
        responseUser.setId(user.getId());
        return user.equals(responseUser);
    }

    @BeforeAll
    static void init() {
        context = SpringApplication.run(FilmorateApplication.class);
        client = HttpClient.newHttpClient();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gson = gsonBuilder.create();
        id = 100;
    }

    @AfterAll
    static void close() {
        client = null;
        context.close();
    }

    @BeforeEach
    void prepareUser() {
        user = User.builder()
                .id(++id)
                .login("xxx")
                .name("Vasya")
                .email("Vasya@mmm")
                .birthday(LocalDate.of(1940, 12, 9))
                .build();
        user.setFriends(new HashMap<>());
    }

    @AfterEach
    void clearUser() {
        user = null;
    }

    @Test
    void createUserEmailTest() throws IOException, InterruptedException {
        //отсутствующий email
        user.setEmail(null);
        String json = gson.toJson(user);
        HttpResponse<String> response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
        //пустой email
        user.setEmail(" ");
        json = gson.toJson(user);
        response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
        //email без @
        user.setEmail("abc");
        json = gson.toJson(user);
        response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
        //email без логина
        user.setEmail("@nnn");
        json = gson.toJson(user);
        response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
        //email с пробелами
        user.setEmail("aaa@n nn");
        json = gson.toJson(user);
        response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
    }

    @Test
    void createUserTestWithBadLogin() throws IOException, InterruptedException {
        //отсутствующий логин
        user.setLogin(null);
        String json = gson.toJson(user);
        HttpResponse<String> response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
        //пустой логин
        user.setLogin(" ");
        json = gson.toJson(user);
        response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
        //логин с пробелами
        user.setLogin("a b");
        json = gson.toJson(user);
        response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400);
    }

    @Test
    void createUserTestWithFutureBirthday() throws IOException, InterruptedException {
        //дата рождения - в будущем
        user.setBirthday(LocalDate.of(2050, 12, 9));
        String json = gson.toJson(user);
        HttpResponse<String> response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 400); //не годится
    }

    @Test
    void createFilmTestWithNowOrFutureBirthday() throws IOException, InterruptedException {
        //дата рождения - текущая (это годится)
        user.setBirthday(LocalDate.now());
        String json = gson.toJson(user);
        HttpResponse<String> response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 200);
        assertTrue(compareUsers(user, response.body())); //сверяем отправленное с полученным
    }

    @Test
    void createFilmTestWithPastBirthday() throws IOException, InterruptedException {
        //дата рождения - на день раньше текущей
        user.setBirthday(user.getBirthday().minusDays(1));
        String json = gson.toJson(user);
        HttpResponse<String> response = responseToPOST(json, "/users");
        assertEquals(response.statusCode(), 200); //это приемлемо
        assertTrue(compareUsers(user, response.body())); //сверяем отправленное с полученным
    }
}