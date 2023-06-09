package ru.yandex.practicum.filmorate.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.filmorate.FilmorateApplication;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.util.ClientRequests;
import ru.yandex.practicum.filmorate.util.LocalDateAdapter;
import ru.yandex.practicum.filmorate.util.TestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmValidationTest extends ClientRequests {
    private static ConfigurableApplicationContext context;
    private static Gson gson;
    private static long id;
    private Film film;

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
    void prepareFilm() {
        film = Film.builder()
                .id(++id)
                .name("xxx")
                .description("")
                .releaseDate(LocalDate.of(1895, 12, 28))
                .duration(17)
                .build();
        film.setLikes(new HashSet<>());
    }

    @AfterEach
    void clearFilm() {
        film = null;
    }

    @Test
    void createNormalFilmTest() throws IOException, InterruptedException {
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        String json = gson.toJson(film);
        HttpResponse<String> response = responseToPOST(json, "/films");
        assertEquals(response.statusCode(), 200);
        assertTrue(TestUtils.compareFilmWithJson(film, response.body(), gson));
    }

    @Test
    void createFilmTestWithTooLongDescription() throws IOException, InterruptedException {
        HttpResponse<String> response;
        char[] chars = new char[200];
        for (int i = 0; i < 200; i++) {
            chars[i] = 'a';
        }
        String description = new String(chars);
        film.setDescription(description);
        String json = gson.toJson(film);
        response = responseToPOST(json, "/films");
        assertEquals(response.statusCode(), 200);
        assertTrue(TestUtils.compareFilmWithJson(film, response.body(), gson)); //нормальное описание
        film.setDescription(description + 'b');
        json = gson.toJson(film);
        response = responseToPOST(json, "/films");
        assertEquals(response.statusCode(), 400);
    }

    @Test
    void createFilmTestWithTooOldRelease() throws IOException, InterruptedException {
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        String json = gson.toJson(film);
        HttpResponse<String> response = responseToPOST(json, "/films");
        assertEquals(response.statusCode(), 400);
    }

    @Test
    void createFilmTestWithNonPositiveDuration() throws IOException, InterruptedException {
        film.setDuration(0);
        String json = gson.toJson(film);
        HttpResponse<String> response = responseToPOST(json, "/films");
        assertEquals(response.statusCode(), 400);
    }
}