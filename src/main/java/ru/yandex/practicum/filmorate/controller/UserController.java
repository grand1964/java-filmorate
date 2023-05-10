package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.exception.ValidateException;
import ru.yandex.practicum.filmorate.model.User;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Long, User> users;
    private long currentId;

    public UserController() {
        currentId = 0;
        users = new HashMap<>();
    }

    ///////////////////////////// Обработчики REST ///////////////////////////

    @GetMapping
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        validateLogin(user.getLogin());
        if (users.containsKey(user.getId())) {
            log.error("Пользователь с идентификатором " + user.getId() + " уже существует.");
            throw new ObjectAlreadyExistException("User", user.getId());
        }
        user.setId(++currentId);
        String name = user.getName();
        if ((name == null) || (name.isBlank())) {
            user.setName(user.getLogin());
        }
        users.put(currentId, user);
        log.info("Добавлен новый пользователь: " + user);
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        validateLogin(user.getLogin());
        if (!users.containsKey(user.getId())) {
            log.error("Пользователя с идентификатором " + user.getId() + " не существует.");
            throw new ObjectNotExistException("User", user.getId());
        }
        users.put(user.getId(), user);
        log.info("Фильм с идентификатором " + user.getId() + " заменен на " + user);
        return user;
    }

    //////////////////////////////// Валидатор ///////////////////////////////

    private static void validateLogin(String login) {
        int spacePosition = login.indexOf(' ');
        if (spacePosition > -1) {
            log.error("Ошибка валидации.");
            throw new ValidateException("Логин не должен содержать пробелы: " + login);
        }
    }
}
