package ru.yandex.practicum.filmorate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    public User getUser(@PathVariable("id") long userId) {
        return service.get(userId);
    }

    //получение списка друзей пользователя
    @GetMapping(value = "/{id}/friends")
    public List<User> getUserFriends(@PathVariable("id") long userId) {
        return service.getFriends(userId);
    }

    //получение списка общих друзей пользователей
    @GetMapping(value = "/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable long id, @PathVariable long otherId) {
        return service.getCommonFriends(id, otherId);
    }

    ////////////////////////////// Передача данных ///////////////////////////

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        return service.create(user);
    }

    @PutMapping
    public User updateUser(@ Valid @RequestBody User user) {
        return service.update(user);
    }

    //добавление пользователя в друзья
    @PutMapping(value = "/{id}/friends/{friendId}")
    public User addFriend(@PathVariable long id, @PathVariable long friendId) {
        return service.addFriend(id, friendId);
    }

    ////////////////////////////// Удаление данных ///////////////////////////

    //удаление из друзей пользователя с заданным идентификатором
    @DeleteMapping(value = "/{id}/friends/{friendId}")
    public User deleteFriend(@PathVariable long id, @PathVariable long friendId) {
        return service.deleteFriend(id, friendId);
    }

    //удаление всех пользователей (нужно для тестов)
    @DeleteMapping
    public void deleteAll() {
        service.deleteAll();
    }
}
