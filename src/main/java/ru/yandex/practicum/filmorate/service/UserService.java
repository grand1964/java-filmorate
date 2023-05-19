package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.Storage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService extends AbstractService<User> {
    @Autowired
    public UserService(Storage<User> storage) {
        super(storage);
    }

    //добавление друга
    public User addFriend(String userParam, String friendParam) {
        long userId = validateId(userParam);
        long friendId = validateId(friendParam);
        if (userId == -1) {
            log.error("Пользователь " + userParam + " не найден.");
            throw new IncorrectParameterException("Пользователь %s не найден: ", userParam);
        }
        if (friendId == -1) {
            log.error("Друг " + userParam + " не найден.");
            throw new IncorrectParameterException("Друг %s не найден: ", friendParam);
        }
        User friend = storage.get(friendId).orElseThrow();
        if (!storage.get(userId).orElseThrow().getFriends().add(friendId)) {
            log.warn("У пользователя " + userParam + " друг " + friendParam + " уже есть.");
        } else {
            log.info("В друзья пользователя " + userParam + " добавлен " + friendParam);
        }
        if (!friend.getFriends().add(userId)) {
            log.warn("У пользователя " + friendParam + " друг " + userParam + " уже есть.");
        } else {
            log.info("В друзья пользователя " + friendParam + " добавлен " + userParam);
        }
        return friend;
    }

    //получение списка друзей пользователя
    public List<User> getFriends(String userParam) {
        long userId = validateId(userParam);
        if (userId == -1) {
            log.warn("Пользователь " + userParam + " не найден.");
            return new ArrayList<>();
        }
        log.info("Получен список друзей пользователя " + userParam);
        return idsToUsers(storage.get(userId).orElseThrow().getFriends());
    }

    //получение списка общих друзей
    public List<User> getCommonFriends(String param1, String param2) {
        long id1 = validateId(param1);
        long id2 = validateId(param2);
        if ((id1 == -1) || (id2 == -1)) {
            log.info("Список общих друзей пользователей " + param1 + " и " + param2 + " пуст.");
            return new ArrayList<>(); //пустой список
        }
        User user1 = storage.get(id1).orElseThrow();
        User user2 = storage.get(id2).orElseThrow();
        log.info("Получен список общих друзей пользователей " + param1 + " и " + param2);
        return idsToUsers(intersection(user1.getFriends(), user2.getFriends()));
    }

    //удаление друга
    public User deleteFriend(String userParam, String friendParam) {
        long userId = validateId(userParam);
        long friendId = validateId(friendParam);
        User user = storage.get(userId).orElseThrow();
        User friend = storage.get(friendId).orElseThrow();
        if (user.getFriends().contains(friendId)) {
            log.info("Из друзей пользователя " + userId + " удален " + friendId);
            user.getFriends().remove(friendId);
        } else {
            log.warn("У пользователя " + userId + " не было друга " + friendId);
        }
        if (friend.getFriends().contains(userId)) {
            log.info("Из друзей пользователя " + friendId + " удален " + userId);
            friend.getFriends().remove(userId);
        } else {
            log.warn("У пользователя " + friendId + " не было друга " + userId);
        }
        return friend;
    }

    //////////////////////////////// Валидация ///////////////////////////////

    @Override
    protected void validate(User user) {
        //проверяем логин
        int spacePosition = user.getLogin().indexOf(' ');
        if (spacePosition > -1) {
            String message = "Логин не должен содержать пробелы: " + user.getLogin();
            log.error(message);
            throw new ValidateException(message);
        }
        //если имя пустое - подставляем вместо него логин
        String name = user.getName();
        if ((name == null) || (name.isBlank())) {
            user.setName(user.getLogin());
            log.info("Заменяем отсутствующее имя логином: " + user.getLogin());
        }
    }

    @Override
    protected long validateId(String paramId) {
        long id = super.validateId(paramId);
        if (id == -1) {
            return id;
        }
        if (storage.get(id).isEmpty()) {
            log.error(String.format("Не найден пользователь с идентификатором " + paramId));
            return -1;
        }
        return id;
    }

    ///////////////////////// Вспомогательные функции ////////////////////////

    //возвращает пересечение двух множеств
    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        return set1.stream().filter(set2::contains).collect(Collectors.toSet());
    }

    //возвращает сортированный по идентификатору список пользователей с заданными идентификаторами
    private List<User> idsToUsers(Set<Long> ids) {
        List<User> list = new ArrayList<>();
        ids.forEach(a -> list.add(storage.get(a).orElseThrow()));
        list.sort(Comparator.comparingLong(User::getId));
        return list;
    }
}
