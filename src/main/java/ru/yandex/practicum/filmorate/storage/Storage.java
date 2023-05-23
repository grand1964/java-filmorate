package ru.yandex.practicum.filmorate.storage;

import java.util.List;
import java.util.Optional;

public interface Storage<T extends Storable> {
    Optional<T> get(long id);

    List<T> getAll();

    Optional<T> create(T object);

    Optional<T> update(T object);

    Optional<T> delete(long id);

    void deleteAll();
}
