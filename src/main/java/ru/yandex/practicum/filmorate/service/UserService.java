package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class UserService {
    private final UserStorage storage;

    @Autowired
    public UserService(UserStorage storage) {
        this.storage = storage;
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    /////////////////////////// Чтение пользователей /////////////////////////

    //получение пользователя по идентификатору
    public User get(long id) {
        //создаем пользователя и читаем его данные из базы
        User user = storage.get(id).orElseThrow(() -> { //пользователя нет, ошибка
            log.error("Задан ошибочный идентификатор: " + id);
            return new IncorrectParameterException("Задан ошибочный идентификатор: ", id);
        });
        //читаем дружеские связи пользователя из базы
        loadUserFriends(user);
        //возвращаем пользователя
        return user;
    }

    //получение списка всех пользователей
    public List<User> getAll() {
        //читаем всех пользователей
        List<User> users = storage.getAll();
        for (User user : users) { //заполняем дружеские связи всех пользователей
            loadUserFriends(user);
        }
        //возвращаем список
        return users;
    }

    /////////////////////////// Запись пользователей /////////////////////////

    //добавление пользователя
    public User create(User user) {
        //проверяем корректность пользователя
        validate(user);
        //пользователь с существующим идентификатором не допускается
        long id = user.getId();
        if (storage.get(id).isPresent()) {
            log.error("Пользователь с идентификатором " + id + " уже существует.");
            throw new ObjectAlreadyExistException(id);
        }
        //создаем пользователя в базе с правильным id
        storage.create(user);
        //сохраняем его связи в базе
        storeUserFriends(user);
        //возвращаем пользователя
        return user;
    }

    //обновление пользователя
    public User update(User user) {
        //проверяем корректность пользователя
        validate(user);
        //обновляем данные пользователя
        long id = user.getId();
        if (!storage.update(user)) { //ошибка, пользователя нет
            log.error("Пользователя с идентификатором " + id + " не существует.");
            throw new ObjectNotExistException(id);
        }
        //если удалось - обновляем подписки пользователя
        storage.deleteAllFriends(id); //удаляем из базы всех друзей пользователя
        storage.deleteAllSubscribes(id); //удаляем в базе пользователя из всех подписок
        storeUserFriends(user); //заполняем связи в базе данными пользователя
        //возвращаем пользователя
        return user;
    }

    ////////////////////////// Удаление пользователей ////////////////////////

    //удаление по идентификатору
    public boolean delete(long id) {
        boolean result = storage.delete(id);
        if (!result) {
            log.warn("Пользователь " + id + " не найден или уже удален.");
        }
        return result;
    }

    //удаление всех пользователей
    public int deleteAll() {
        int count = storage.deleteAll();
        log.info("Удалено " + count + " пользователей.");
        return count;
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Действия с друзьями //////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //добавление друга
    public User addFriend(long userId, long friendId) {
        if (storage.get(userId).isEmpty()) {
            badUser(userId);
        }
        User friend = storage.get(friendId).orElseThrow(() -> { //пользователя нет, ошибка
            log.error("Задан ошибочный идентификатор: " + friendId);
            return new IncorrectParameterException("Пользователь с идентификатором %d не найден.", friendId);
        });
        if (storage.addFriend(userId, friendId)) {
            log.info("В друзья пользователя " + userId + " добавлен " + friendId);
        } else {
            log.warn("У пользователя " + userId + " друг " + friendId + " уже есть.");
        }
        return friend;
    }

    //получение списка друзей пользователя
    public List<User> getFriends(long userId) {
        if (storage.get(userId).isEmpty()) {
            badUser(userId);
        }
        log.info("Получен список друзей пользователя " + userId);
        return storage.getFriends(userId);
    }

    //получение списка подтвержденных друзей пользователя
    public List<User> getAcknowledgedFriends(long userId) {
        if (storage.get(userId).isEmpty()) {
            badUser(userId);
        }
        log.info("Получен список подтвержденных друзей пользователя " + userId);
        return storage.getAcknowledgedFriends(userId);
    }

    //получение списка общих друзей
    public List<User> getCommonFriends(long id1, long id2) {
        if (storage.get(id1).isEmpty()) {
            badUser(id1);
        }
        if (storage.get(id2).isEmpty()) {
            badUser(id2);
        }
        log.info("Получен список общих друзей пользователей " + id1 + " и " + id2);
        return storage.getCommonFriends(id1, id2);
    }

    //удаление друга
    public boolean deleteFriend(long userId, long friendId) {
        boolean result = storage.deleteFriend(userId, friendId);
        if (result) {
            log.info("Из друзей пользователя " + userId + " удален " + friendId);
        } else {
            log.warn("У пользователя " + userId + " не было друга " + friendId);
        }
        return result;
    }

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Оперирование связями /////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //сохраняет в базе данные пользователя о друзьях
    private void storeUserFriends(User user) {
        long userId = user.getId();
        Map<Long, Boolean> friends = user.getFriends(); //читаем друзей
        if (friends == null) { //отображение не инициализировано
            friends = new HashMap<>(); //создаем его
        }
        for (Long id : friends.keySet()) {
            storage.addFriend(userId, id); //добавляем пользователю друга
            if (friends.get(id)) { //дружба подтверждена
                storage.addFriend(id, userId); //добавляем и обратную связь
            }
        }
    }

    //читает из базы данные пользователя о друзьях
    private void loadUserFriends(User user) {
        long userId = user.getId();
        Set<Long> allFriendIds = storage.getFriendIds(userId); //все друзья
        Set<Long> acknowledgedFriendIds = storage.getAcknowledgedFriendIds(userId); //подтвержденные друзья
        Map<Long, Boolean> friends = new HashMap<>();
        for (Long friendId : allFriendIds) {
            if (acknowledgedFriendIds.contains(friendId)) { //дружба подтверждена
                friends.put(friendId, true); //ставим флаг подтверждения
            } else { //не подтверждена
                friends.put(friendId, false); //сбрасываем флаг
            }
        }
        user.setFriends(friends);
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
