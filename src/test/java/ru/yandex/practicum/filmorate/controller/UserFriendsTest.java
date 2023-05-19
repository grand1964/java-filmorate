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

class UserFriendsTest extends ClientRequests {
    private static final int USER_COUNT = 10;
    private static ConfigurableApplicationContext context;
    private static Gson gson;
    private static List<User> users;

    ///////////////////////// Методы жизненного цикла ////////////////////////

    @BeforeAll
    static void init() {
        context = SpringApplication.run(FilmorateApplication.class);
        client = HttpClient.newHttpClient();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gson = gsonBuilder.create();
        users = TestUtils.generateUsers(USER_COUNT);
    }

    @AfterAll
    static void close() {
        client = null;
        context.close();
    }

    @BeforeEach
    void prepareUserStorage() throws IOException, InterruptedException {
        for (int i = 0; i < USER_COUNT; i++) {
            String json = gson.toJson(users.get(i));
            responseToPOST(json, "/users");
        }
    }

    @AfterEach
    void clearUser() throws IOException, InterruptedException {
        responseToDELETE("/users");
    }

    ///////////////////////////////// Тесты //////////////////////////////////

    @Test
    void getUserByIdTest() throws IOException, InterruptedException {
        int number = USER_COUNT + 17;
        //отсутствующий пользователь
        assertEquals(responseToGET("/users/" + number).statusCode(), 404);
        //пользователь с нечисловым номером
        assertEquals(responseToGET("/users/" + "1a2").statusCode(), 400);
        //существующий пользователь
        number = 1;
        User user = TestUtils.generateUser(number);
        HttpResponse<String> response = responseToGET("/users/" + number);
        assertEquals(response.statusCode(), 200);
        assertTrue(TestUtils.compareUserWithJson(user, response.body(), gson)); //сверяем отправленное с полученным
    }

    @Test
    void getAllUserTest() throws IOException, InterruptedException {
        HttpResponse<String> response = responseToGET("/users/");
        assertEquals(response.statusCode(), 200);
        List<User> list = TestUtils.jsonToUserList(response.body(), gson);
        for (int i = 0; i < USER_COUNT; i++) {
            assertTrue(TestUtils.compareUsers(users.get(i), list.get(i)));
        }
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
        assertTrue(TestUtils.compareUserWithJson(users.get(friendId - 1), response.body(), gson));
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
    }

    @Test
    void commonFriendsTest() throws IOException, InterruptedException {
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
        assertEquals(friends.size(), 2);
        //проверяем идентификаторы друзей
        Set<Long> commonIds = Set.of(4L, 6L);
        for (User friend : friends) {
            assertTrue(commonIds.contains(friend.getId()));
        }
    }

    ///////////////////////// Вспомогательная функция ////////////////////////

    //Связывает пользователей в друзья
    private int setFriends(long id1, long id2) throws IOException, InterruptedException {
        String request = "/users/%s/friends/%s";
        HttpResponse<String> response = responseToVoidPUT(String.format(request, id1, id2));
        return response.statusCode();
    }
}