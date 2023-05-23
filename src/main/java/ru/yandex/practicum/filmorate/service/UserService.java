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
    public User addFriend(long userId, long friendId) {
        User user = storage.get(userId).orElseThrow(() -> badUser(userId));
        User friend = storage.get(friendId).orElseThrow(() -> badUser(friendId));
        if (user.addFriend(friendId)) {
            log.info("В друзья пользователя " + userId + " добавлен " + friendId);
        } else {
            log.warn("У пользователя " + userId + " друг " + friendId + " уже есть.");
        }
        if (friend.addFriend(userId)) {
            log.info("В друзья пользователя " + friendId + " добавлен " + userId);
        } else {
            log.warn("У пользователя " + friendId + " друг " + userId + " уже есть.");
        }
        return friend;
    }

    //получение списка друзей пользователя
    public List<User> getFriends(long userId) {
        User user = storage.get(userId).orElseThrow(() -> badUser(userId));
        log.info("Получен список друзей пользователя " + userId);
        return idsToUsers(user.getFriends());
    }

    //получение списка общих друзей
    public List<User> getCommonFriends(long id1, long id2) {
        if (storage.get(id1).isEmpty() || storage.get(id2).isEmpty()) {
            log.info("Список общих друзей пользователей " + id1 + " и " + id2 + " пуст.");
            return new ArrayList<>(); //пустой список
        }
        User user1 = storage.get(id1).orElseThrow(() -> badUser(id1));
        User user2 = storage.get(id2).orElseThrow(() -> badUser(id2));
        log.info("Получен список общих друзей пользователей " + id1 + " и " + id2);
        return idsToUsers(intersection(user1.getFriends(), user2.getFriends()));
    }

    //удаление друга
    public User deleteFriend(long userId, long friendId) {
        User user = storage.get(userId).orElseThrow(() -> badUser(userId));
        User friend = storage.get(friendId).orElseThrow(() -> badUser(friendId));
        if (user.removeFriend(friendId)) {
            log.info("Из друзей пользователя " + userId + " удален " + friendId);
        } else {
            log.warn("У пользователя " + userId + " не было друга " + friendId);
        }
        if (friend.removeFriend(userId)) {
            log.info("Из друзей пользователя " + friendId + " удален " + userId);
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

    private IncorrectParameterException badUser(long id) {
        String message = "Пользователь с идентификатором %d не найден.";
        log.error(String.format(message, id));
        return new IncorrectParameterException(message, id);
    }

    ///////////////////////// Вспомогательные функции ////////////////////////

    //возвращает пересечение двух множеств
    private static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        return set1.stream().filter(set2::contains).collect(Collectors.toSet());
    }

    //возвращает сортированный по идентификатору список пользователей с заданными идентификаторами
    private List<User> idsToUsers(Set<Long> ids) {
        List<User> list = new ArrayList<>();
        ids.forEach(id -> list.add(storage.get(id).orElseThrow(() -> badUser(id))));
        list.sort(Comparator.comparingLong(User::getId));
        return list;
    }
}
