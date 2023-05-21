package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterFormatException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService service;

    @Autowired
    public UserController(UserService service) {
        this.service = service;
    }

    ///////////////////////////// Получение данных ///////////////////////////

    //получение всех пользователей
    @GetMapping
    public List<User> getAllUsers() {
        return service.getAll();
    }

    //получение пользователя по идентификатору
    @GetMapping(value = "/{id}")
    public User getUser(@PathVariable("id") String userId) {
        return service.get(parseNumberParam(userId));
    }

    //получение списка друзей пользователя
    @GetMapping(value = "/{id}/friends")
    public List<User> getUserFriends(@PathVariable("id") String userId) {
        return service.getFriends(parseNumberParam(userId));
    }

    //получение списка общих друзей пользователей
    @GetMapping(value = "/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable String id, @PathVariable String otherId) {
        return service.getCommonFriends(parseNumberParam(id), parseNumberParam(otherId));
    }

    ////////////////////////////// Передача данных ///////////////////////////

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        return service.create(user);
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        return service.update(user);
    }

    //добавление пользователя в друзья
    @PutMapping(value = "/{id}/friends/{friendId}")
    public User addFriend(@PathVariable String id, @PathVariable String friendId) {
        return service.addFriend(parseNumberParam(id), parseNumberParam(friendId));
    }

    ////////////////////////////// Удаление данных ///////////////////////////

    //удаление из друзей пользователя с заданным идентификатором
    @DeleteMapping(value = "/{id}/friends/{friendId}")
    public User deleteFriend(@PathVariable String id, @PathVariable String friendId) {
        return service.deleteFriend(parseNumberParam(id), parseNumberParam(friendId));
    }

    //удаление всех пользователей (нужно для тестов)
    @DeleteMapping
    public void deleteAll() {
        service.deleteAll();
    }

    ////////////////////////// Конвертация параметров ////////////////////////

    protected long parseNumberParam(String param) {
        try {
            return Long.parseLong(param);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterFormatException("Задан нечисловой параметр: ", param);
        }
    }
}
