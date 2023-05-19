package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import java.util.HashSet;
import java.util.Optional;

@Component
public class InMemoryUserStorage extends InMemoryStorage<User> {
    @Override
    public Optional<User> create(User user) {
        if (user.getFriends() == null) { //список друзей не инициализирован
            user.setFriends(new HashSet<>()); //создаем его
        }
        return super.create(user); //вызываем общий метод создания объекта
    }

    @Override
    public Optional<User> update(User user) {
        if (user.getFriends() == null) { //список друзей не инициализирован
            user.setFriends(new HashSet<>()); //создаем его
        }
        return super.update(user); //вызываем общий метод создания объекта
    }
}
