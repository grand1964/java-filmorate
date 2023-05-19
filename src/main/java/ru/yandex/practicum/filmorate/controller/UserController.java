package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {
    private final UserService service;

    @Autowired
    public UserController(UserService service) {
        this.service = service;
    }

    ///////////////////////////// Получение данных ///////////////////////////

    //получение всех пользователей
    @GetMapping(value = "/users")
    public List<User> getAllUsers() {
        return service.getAll();
    }

    //получение пользователя по идентификатору
    @GetMapping(value = "/users/{id}")
    public User getUser(@PathVariable("id") String userId) {
        return service.get(userId);
    }

    //получение списка друзей пользователя
    @GetMapping(value = "/users/{id}/friends")
    public List<User> getUserFriends(@PathVariable("id") String userId) {
        return service.getFriends(userId);
    }

    //получение списка общих друзей пользователей
    @GetMapping(value = "/users/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Map<String, String> params) {
        return service.getCommonFriends(params.get("id"), params.get("otherId"));
    }

    ////////////////////////////// Передача данных ///////////////////////////

    @PostMapping(value = "/users")
    public User createUser(@Valid @RequestBody User user) {
        return service.create(user);
    }

    @PutMapping(value = "/users")
    public User updateUser(@RequestBody User user) {
        return service.update(user);
    }

    //добавление пользователя в друзья
    @PutMapping(value = "/users/{id}/friends/{friendId}")
    public User addFriend(@PathVariable Map<String, String> params) {
        return service.addFriend(params.get("id"), params.get("friendId"));
    }

    ////////////////////////////// Удаление данных ///////////////////////////

    //удаление из друзей пользователя с заданным идентификатором
    @DeleteMapping(value = "/users/{id}/friends/{friendId}")
    public User deleteFriend(@PathVariable Map<String, String> params) {
        return service.deleteFriend(params.get("id"), params.get("friendId"));
    }

    //удаление всех пользователей (нужно для тестов)
    @DeleteMapping(value = "/users")
    public void deleteAll() {
        service.deleteAll();
    }
}
