package ru.yandex.practicum.filmorate.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilmControllerTest extends ClientRequests {
    private static final int FILM_COUNT = 10;
    private static ConfigurableApplicationContext context;
    private static Gson gson;

    ///////////////////////// Методы жизненного цикла ////////////////////////

    @BeforeAll
    static void init() {
        context = SpringApplication.run(FilmorateApplication.class);
        client = HttpClient.newHttpClient();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gson = gsonBuilder.create();
    }

    @AfterAll
    static void close() {
        client = null;
        context.close();
    }

    ///////////////////////////////// Тесты //////////////////////////////////

    @Test
    void getFilmByIdTest() throws IOException, InterruptedException {
        int number = FILM_COUNT + 17;
        //отсутствующий фильм
        assertEquals(responseToGET("/films/" + number).statusCode(), 404);
        //фильм с нечисловым номером
        assertEquals(responseToGET("/films/" + "1a2").statusCode(), 400);
        //существующий фильм
        number = 1;
        HttpResponse<String> response = responseToGET("/films/" + number);
        assertEquals(response.statusCode(), 200);
    }

    @Test
    void getAllFilmsTest() throws IOException, InterruptedException {
        HttpResponse<String> response = responseToGET("/films/");
        assertEquals(response.statusCode(), 200);
    }

    @Test
    void addLikeToFilmTest() throws IOException, InterruptedException {
        //добавляем лайк несуществующему фильму
        int filmId = FILM_COUNT + 17;
        int userId = 1;
        assertEquals(setLike(filmId, userId), 404);
        //корректное добавление лайка
        filmId = 1;
        assertEquals(setLike(filmId, userId), 200);
        //повторное добавление лайка
        assertEquals(setLike(filmId, userId), 200); //это не ошибка
        //удаление лайка (возвращаем исходное состояние)
        removeLike(filmId, userId);
    }

    @Test
    void deleteLikeOfFilmTest() throws IOException, InterruptedException {
        int filmId = 1;
        int userId = 5;
        //вначале ставим лайк
        setLike(filmId, userId);
        //а теперь удаляем
        String request = String.format("/films/%d/like/%d", filmId, userId);
        HttpResponse<String> response = responseToDELETE(request);
        assertEquals(response.statusCode(), 200);
        //повторное удаление лайка
        assertEquals(responseToDELETE(request).statusCode(), 200); //это не ошибка
    }

    @Test
    void topFilmsByLikeTest() throws IOException, InterruptedException {
        //вначале расставляем лайки
        for (int id = 1; id <= FILM_COUNT; id++) {
            for (int i = 1; i <= id; i++) {
                setLike(id, i);
            }
        }
        String request;
        //проверяем нечисловой параметр
        request = "/films/popular?count=1a2";
        assertEquals(responseToGET(request).statusCode(), 400);
        //проверяем некорректные числовые параметры
        request = "/films/popular?count=0";
        assertEquals(responseToGET(request).statusCode(), 400);

        //получаем список 10 топ-фильмов
        request = "/films/popular";
        HttpResponse<String> response;
        response = responseToGET(request);
        assertEquals(response.statusCode(), 200);
        List<Film> topList = TestUtils.jsonToFilmList(response.body(), gson);
        assertEquals(topList.size(), FILM_COUNT);
        for (int i = 0; i < FILM_COUNT; i++) {
            assertEquals(topList.get(i).getId(), FILM_COUNT - i);
        }

        //получаем список заданного числа топ-фильмов
        int topCount = 5;
        request = request + String.format("?count=%d", topCount);
        response = responseToGET(request);
        assertEquals(response.statusCode(), 200);
        topList = TestUtils.jsonToFilmList(response.body(), gson);
        assertEquals(topList.size(), topCount);
        for (int i = 0; i < topCount; i++) {
            assertEquals(topList.get(i).getId(), FILM_COUNT - i);
        }

        //удаляем лайки (возвращаем исходное состояние)
        for (int id = 1; id <= FILM_COUNT; id++) {
            for (int i = 1; i <= id; i++) {
                removeLike(id, i);
            }
        }
    }

    ///////////////////////// Вспомогательные функции ////////////////////////

    //добавляет лайк фильму
    private int setLike(long filmId, long userId) throws IOException, InterruptedException {
        String request = "/films/%s/like/%s";
        HttpResponse<String> response = responseToVoidPUT(String.format(request, filmId, userId));
        return response.statusCode();
    }

    //удаляет лайк
    private void removeLike(long userId, long friendId) throws IOException, InterruptedException {
        String request = String.format("/films/%d/like/%d", userId, friendId);
        responseToDELETE(request);
    }
}