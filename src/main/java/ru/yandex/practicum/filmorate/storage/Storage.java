package ru.yandex.practicum.filmorate.storage;

import java.util.List;
import java.util.Optional;

public interface Storage<T extends Storable> {
    boolean contains(long id);

    Optional<T> get(long id);

    List<T> getAll();

    void create(T object);

    boolean update(T object);

    boolean delete(long id);

    int deleteAll();
}
