package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FriendStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;
    private final FriendStorage friendStorage;

    @Autowired
    public UserService(UserStorage userStorage, FriendStorage friendStorage) {
        this.userStorage = userStorage;
        this.friendStorage = friendStorage;
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    /////////////////////////// Чтение пользователей /////////////////////////

    //получение пользователя по идентификатору
    public User get(long id) {
        //создаем пользователя и читаем его данные из базы
        return userStorage.get(id).orElseThrow(() -> { //пользователя нет, ошибка
            log.error("Задан ошибочный идентификатор: " + id);
            return new IncorrectParameterException("Задан ошибочный идентификатор: ", id);
        });
    }

    //получение всех пользователей
    public List<User> getAll() {
        //читаем всех пользователей
        return userStorage.getAll();
    }

    /////////////////////////// Запись пользователей /////////////////////////

    //добавление пользователя
    public User create(User user) {
        //проверяем корректность пользователя
        validate(user);
        //пользователь с существующим идентификатором не допускается
        long id = user.getId();
        if (userStorage.get(id).isPresent()) {
            log.error("Пользователь с идентификатором " + id + " уже существует.");
            throw new ObjectAlreadyExistException(id);
        }
        //создаем пользователя в базе с правильным id
        userStorage.create(user);
        //сохраняем его связи в базе
        friendStorage.addFriendsOfUser(user);
        //возвращаем пользователя
        return user;
    }

    //обновление пользователя
    public User update(User user) {
        //проверяем корректность пользователя
        validate(user);
        //обновляем данные пользователя
        long id = user.getId();
        if (!userStorage.update(user)) { //ошибка, пользователя нет
            log.error("Пользователя с идентификатором " + id + " не существует.");
            throw new ObjectNotExistException(id);
        }
        //если удалось - обновляем подписки пользователя
        friendStorage.deleteAllFriends(id); //удаляем из базы всех друзей пользователя
        friendStorage.deleteAllSubscribes(id); //удаляем в базе пользователя из всех подписок
        friendStorage.addFriendsOfUser(user); //заполняем связи в базе данными пользователя
        //возвращаем пользователя
        return user;
    }

    ////////////////////////// Удаление пользователей ////////////////////////

    //удаление по идентификатору
    public boolean delete(long id) {
        boolean result = userStorage.delete(id);
        if (!result) {
            log.warn("Пользователь " + id + " не найден или уже удален.");
        }
        return result;
    }

    //удаление всех пользователей
    public int deleteAll() {
        int count = userStorage.deleteAll();
        log.info("Удалено " + count + " пользователей.");
        return count;
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Действия с друзьями //////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //добавление друга
    public User addFriend(long userId, long friendId) {
        if (!userStorage.contains(userId)) {
            badUser(userId);
        }
        if (!userStorage.contains(friendId)) {
            badUser(friendId);
        }
        if (friendStorage.addFriend(userId, friendId)) {
            log.info("В друзья пользователя " + userId + " добавлен " + friendId);
        } else {
            log.warn("У пользователя " + userId + " друг " + friendId + " уже есть.");
        }
        return userStorage.get(friendId).orElse(null); //тут ошибки быть не может
    }

    //получение списка друзей пользователя
    public List<User> getFriends(long userId) {
        if (!userStorage.contains(userId)) {
            badUser(userId);
        }
        log.info("Получен список друзей пользователя " + userId);
        return friendStorage.getFriends(userId);
    }

    //получение списка подтвержденных друзей пользователя
    public List<User> getAcknowledgedFriends(long userId) {
        if (!userStorage.contains(userId)) {
            badUser(userId);
        }
        log.info("Получен список подтвержденных друзей пользователя " + userId);
        return friendStorage.getAcknowledgedFriends(userId);
    }

    //получение списка общих друзей
    public List<User> getCommonFriends(long id1, long id2) {
        if (userStorage.get(id1).isEmpty()) {
            badUser(id1);
        }
        if (userStorage.get(id2).isEmpty()) {
            badUser(id2);
        }
        log.info("Получен список общих друзей пользователей " + id1 + " и " + id2);
        return friendStorage.getCommonFriends(id1, id2);
    }

    //удаление друга
    public boolean deleteFriend(long userId, long friendId) {
        boolean result = friendStorage.deleteFriend(userId, friendId);
        if (result) {
            log.info("Из друзей пользователя " + userId + " удален " + friendId);
        } else {
            log.warn("У пользователя " + userId + " не было друга " + friendId);
        }
        return result;
    }

    //////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Валидация ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //проверки пользователя перед операциями над ним
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

    //диагностика ошибочного идентификатора
    private void badUser(long id) {
        String message = "Пользователь с идентификатором %d не найден.";
        log.error(String.format(message, id));
        throw new IncorrectParameterException(message, id);
    }
}
