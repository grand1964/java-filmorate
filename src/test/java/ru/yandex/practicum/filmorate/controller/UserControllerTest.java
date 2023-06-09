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
import ru.yandex.practicum.filmorate.util.TestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest extends ClientRequests {
    private static final int USER_COUNT = 10;
    private static ConfigurableApplicationContext context;
    private static Gson gson;

    ///////////////////////// Методы жизненного цикла ////////////////////////

    @BeforeAll
    static void init() {
        context = SpringApplication.run(FilmorateApplication.class);
        client = HttpClient.newHttpClient();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
    }

    @AfterAll
    static void close() {
        client = null;
        context.close();
    }

    ///////////////////////////////// Тесты //////////////////////////////////

    /*
        Проверяют только коды возврата.
        Не предназначены для тестирования данных в базе.
     */

    @Test
    void getUserByIdTest() throws IOException, InterruptedException {
        int number = USER_COUNT + 17;
        //отсутствующий пользователь
        assertEquals(responseToGET("/users/" + number).statusCode(), 404);
        //пользователь с нечисловым номером
        assertEquals(responseToGET("/users/" + "1a2").statusCode(), 400);
        //существующий пользователь
        HttpResponse<String> response = responseToGET("/users/" + 1);
        assertEquals(response.statusCode(), 200);
    }

    @Test
    void getAllUserTest() throws IOException, InterruptedException {
        HttpResponse<String> response = responseToGET("/users/");
        assertEquals(response.statusCode(), 200);
    }

    @Test
    void addFriendToUserTest() throws IOException, InterruptedException {
        //добавляем несуществующего друга
        int userId = 1;
        int friendId = USER_COUNT + 17;
        assertEquals(setFriends(userId, friendId), 404);
        //добавляем друга несуществующему пользователю
        userId = USER_COUNT + 17;
        friendId = 1;
        assertEquals(setFriends(userId, friendId), 404);
        //корректное добавление друга
        userId = 1;
        friendId = 2;
        assertEquals(setFriends(userId, friendId), 200);
        //повторное добавление друга
        assertEquals(setFriends(userId, friendId), 200); //это не ошибка
        //удаление друга (возвращаем исходное состояние)
        removeFriends(userId, friendId);
    }

    @Test
    void deleteFriendTest() throws IOException, InterruptedException {
        int userId = 1;
        int friendId = 5;
        //вначале создаем друзей
        setFriends(userId, friendId);
        //а теперь удаляем
        String request = String.format("/users/%d/friends/%d", userId, friendId);
        HttpResponse<String> response = responseToDELETE(request);
        assertEquals(response.statusCode(), 200);
        //повторное удаление друга
        assertEquals(responseToDELETE(request).statusCode(), 200); //это не ошибка
    }

    @Test
    void friendsListTest() throws IOException, InterruptedException {
        //вначале создаем друзей
        int userId = 1;
        for (int id = 2; id < 6; id++) {
            setFriends(userId, id);
        }
        //затем запрашиваем их для пользователя 1
        String request = "/users/%s/friends";
        HttpResponse<String> response = responseToGET(String.format(request, userId));
        assertEquals(response.statusCode(), 200);
        //конвертируем ответ в список
        List<User> friends = TestUtils.jsonToUserList(response.body(), gson);
        //проверяем идентификаторы друзей
        assertEquals(friends.size(), 4);
        for (int id = 2; id < 6; id++) {
            assertEquals(friends.get(id - 2).getId(), id);
        }
        //удаляем всех друзей
        for (int id = 2; id < 6; id++) {
            removeFriends(userId, id);
        }
    }

    @Test
    void commonFriendsTest() throws IOException, InterruptedException {
        //создаем друзей
        for (int id = 2; id < USER_COUNT - 1; id++) {
            setFriends(id, id + 1);
            setFriends(1, id);
        }
        long userId = 1;
        long otherId = 5;
        String request = String.format("/users/%d/friends/common/%d", userId, otherId);
        HttpResponse<String> response = responseToGET(request);
        assertEquals(response.statusCode(), 200);
        //конвертируем ответ в список
        List<User> friends = TestUtils.jsonToUserList(response.body(), gson);
        //проверяем количество общих друзей
        assertEquals(friends.size(), 1);
        //проверяем идентификаторы друзей
        Set<Long> commonIds = Set.of(6L);
        for (User friend : friends) {
            assertTrue(commonIds.contains(friend.getId()));
        }
        //удаляем друзей
        for (int id = 2; id < USER_COUNT - 1; id++) {
            removeFriends(id, id + 1);
            removeFriends(1, id);
        }
    }

    ///////////////////////// Вспомогательные функции ////////////////////////

    //связывает пользователей в друзья
    private int setFriends(long id1, long id2) throws IOException, InterruptedException {
        String request = "/users/%s/friends/%s";
        HttpResponse<String> response = responseToVoidPUT(String.format(request, id1, id2));
        return response.statusCode();
    }

    //удаляет друзей
    private void removeFriends(long userId, long friendId) throws IOException, InterruptedException {
        String request = String.format("/users/%d/friends/%d", userId, friendId);
        responseToDELETE(request);
    }
}