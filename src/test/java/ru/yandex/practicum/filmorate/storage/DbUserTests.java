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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.util.TestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DbUserTests {
    private static final int USER_COUNT = 10;
    private final UserService service;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void resetDatabase() {
        jdbcTemplate.update(TestUtils.getSqlForResetUsers(USER_COUNT));
    }

    @Test
    public void getNotExistingUserTest() {
        long badId = 17;
        assertThrows(IncorrectParameterException.class, () -> service.get(badId));
    }

    @Test
    public void getExistingUserTest() {
        long userId = 1;
        service.addFriend(userId, 2);
        service.addFriend(2, userId);
        service.addFriend(userId, 3);
        service.addFriend(userId, 4);
        service.addFriend(4, userId);
        //получаем пользователя
        User user = service.get(userId);
        //проверяем его id
        assertEquals(user.getId(), userId);
        //проверяем друзей
        Map<Long, Boolean> friends = user.getFriends();
        assertEquals(friends.size(), 3); //из 3
        for (long i = 2; i < 5; i++) { //проверяем их id
            assertTrue(friends.containsKey(i));
        }
        //проверяем статусы друзей
        assertTrue(friends.get(2L));
        assertFalse(friends.get(3L));
        assertTrue(friends.get(4L));
    }

    @Test
    public void getAllUsersTest() {
        long userId = 1;
        service.addFriend(userId, 2);
        service.addFriend(2, userId);
        service.addFriend(userId, 3);
        service.addFriend(userId, 4);
        service.addFriend(4, userId);
        List<User> users = service.getAll();
        assertEquals(users.size(), USER_COUNT);
        for (int i = 0; i < USER_COUNT; i++) {
            assertEquals(users.get(i).getId(), i + 1);
        }
        Map<Long, Boolean> friends = users.get(0).getFriends(); //друзья пользователя 1
        assertEquals(friends.size(), 3);
        assertEquals(friends.values().stream().filter((b) -> b).count(), 2);
    }

    @Test
    public void createUserWithExistingIdTest() {
        //создаем пользователя с существующим номером
        User user = TestUtils.generateUser(1);
        assertThrows(ObjectAlreadyExistException.class, () -> service.create(user));
    }

    @Test
    public void createNewUserTest() {
        //создаем пользователя
        User user = TestUtils.generateUser(17);
        //задаем его друзей
        Map<Long, Boolean> friends = new HashMap<>();
        friends.put(2L, false);
        friends.put(3L, false);
        friends.put(4L, true);
        user.setFriends(friends);
        //помещаем пользователя в базу
        service.create(user);
        //тесты users
        assertEquals(user.getId(), USER_COUNT + 1); //id должен быть сгенерирован
        List<User> users = service.getAll();
        assertEquals(users.size(), USER_COUNT + 1); //число людей в базе увеличилось на 1
        //тесты friends
        long userId = user.getId();
        List<User> allFriends = service.getFriends(userId); //все друзья
        assertEquals(allFriends.size(), 3); //их трое
        for (int i = 0; i < 3; i++) { //проверяем их id
            assertEquals(allFriends.get(i).getId(), i + 2);
        }
        List<User> acknowledgedFriends = service.getAcknowledgedFriends(userId); //подтвержденные друзья
        assertEquals(acknowledgedFriends.size(), 1); //такой лишь один
        assertEquals(acknowledgedFriends.get(0).getId(), 4);
    }

    @Test
    public void updateNotExistingUserTest() {
        //создаем пользователя с несуществующим номером
        User user = TestUtils.generateUser(17);
        assertThrows(ObjectNotExistException.class, () -> service.update(user));
    }

    @Test
    public void updateExistingUserTest() {
        int userId = 1;
        //задаем новых друзей для пользователя 1
        service.addFriend(userId, 7);
        service.addFriend(userId, 8);
        service.addFriend(userId, 9);
        service.addFriend(9, userId);
        //создаем нового пользователя с идентификатором 1
        User user = TestUtils.generateUser(userId);
        //задаем его друзей
        Map<Long, Boolean> friends = new HashMap<>();
        friends.put(2L, false);
        friends.put(3L, false);
        friends.put(4L, true);
        user.setFriends(friends);
        //помещаем пользователя в базу
        service.update(user);
        //тесты users
        List<User> users = service.getAll();
        assertEquals(users.size(), USER_COUNT); //число людей в базе не меняется
    }

    @Test
    public void deleteNotExistingUserTest() {
        int userId = 17;
        service.delete(userId);
        assertFalse(service.delete(userId));
        assertEquals(service.getAll().size(), USER_COUNT); //число пользователей не меняется
    }

    @Test
    public void deleteExistingUserTest() {
        int userId = 1;
        assertTrue(service.delete(userId));
        //число пользователей уменьшается на 1
        assertEquals(service.getAll().size(), USER_COUNT - 1);
        //проверка, что пользователя больше нет в базе
        assertThrows(IncorrectParameterException.class, () -> service.get(userId));
    }

    @Test
    public void deleteAllUsersTest() {
        int count = service.deleteAll();
        assertEquals(count, USER_COUNT);
        assertEquals(service.getAll().size(), 0);
    }

    @Test
    public void addFriendsToNotExistingUserTest() {
        assertThrows(IncorrectParameterException.class, () -> service.addFriend(17, 5));
    }

    @Test
    public void addAndGetFriendsToExistingUserTest() {
        long userId = 1;
        service.addFriend(userId, 5);
        service.addFriend(5, userId);
        service.addFriend(5, 7);
        service.addFriend(userId, 7);
        service.addFriend(7, 5);
        service.addFriend(7, 3);
        service.addFriend(userId, 9);
        service.addFriend(9, userId);
        List<User> friends = service.getFriends(userId);
        assertEquals(friends.size(), 3);
        for (int i = 0; i < 3; i++) { //проверяем id друзей
            assertEquals(friends.get(i).getId(), 2 * i + 5);
        }
        //проверяем подтверждение
        assertEquals(friends.get(0).getFriends().values().stream().filter((b) -> b).count(), 2);
        assertEquals(friends.get(1).getFriends().values().stream().filter((b) -> b).count(), 1);
        assertEquals(friends.get(2).getFriends().values().stream().filter((b) -> b).count(), 1);
    }

    @Test
    public void getAcknowledgedFriendsTest() {
        service.addFriend(1, 5);
        service.addFriend(1, 7);
        service.addFriend(7, 1);
        service.addFriend(1, 9);
        service.addFriend(9, 1);
        service.addFriend(9, 3);
        service.addFriend(3, 9);
        List<User> friends = service.getAcknowledgedFriends(1);
        assertEquals(friends.size(), 2);
        for (int i = 0; i < 2; i++) { //проверяем id друзей
            assertEquals(friends.get(i).getId(), 2 * i + 7);
        }
        //проверяем подтверждения
        assertEquals(friends.get(0).getFriends().values().stream().filter((b) -> b).count(), 1);
        assertEquals(friends.get(1).getFriends().values().stream().filter((b) -> b).count(), 2);
    }

    @Test
    public void getCommonFriendsOfBadUsersTest() {
        assertThrows(IncorrectParameterException.class, () -> service.getCommonFriends(17, 5));
        assertThrows(IncorrectParameterException.class, () -> service.getCommonFriends(5, 17));
    }

    @Test
    public void getCommonFriendsOfExistingUsersTest() {
        int id1 = 1;
        int id2 = 2;
        service.addFriend(id1, 5);
        service.addFriend(id1, 7);
        service.addFriend(7, 3);
        service.addFriend(7, 4);
        service.addFriend(id1, 9);
        service.addFriend(9, 2);
        service.addFriend(id2, 7);
        service.addFriend(id2, 8);
        service.addFriend(id2, 9);
        List<User> friends = service.getCommonFriends(id1, id2);
        assertEquals(friends.size(), 2);
        for (int i = 0; i < 2; i++) { //проверяем id друзей
            assertEquals(friends.get(i).getId(), 2 * i + 7);
        }
    }

    @Test
    public void deleteFriendTest() {
        service.addFriend(1, 2);
        assertTrue(service.deleteFriend(1, 2));
        assertEquals(service.getFriends(1).size(), 0);
        assertFalse(service.deleteFriend(1, 2)); //удаляем еще раз
    }
}
