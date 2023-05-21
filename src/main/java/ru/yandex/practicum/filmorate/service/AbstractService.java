package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.storage.Storable;
import ru.yandex.practicum.filmorate.storage.Storage;

import java.util.List;

public abstract class AbstractService<T extends Storable> {
    protected Storage<T> storage;
    protected Logger log;

    public AbstractService(Storage<T> storage) {
        this.storage = storage;
        log = LoggerFactory.getLogger(getClass());
    }

    ///////////////////////////// Чтение объектов ////////////////////////////

    //получение объекта по идентификатору
    public T get(long id) {
        return storage.get(id).orElseThrow(() -> {
            log.error("Задан ошибочный идентификатор: " + id);
            return new IncorrectParameterException("Задан ошибочный идентификатор: ", id);
        });
    }

    //получение списка всех объектов
    public List<T> getAll() {
        return storage.getAll();
    }

    ///////////////////////////// Запись объектов ////////////////////////////

    //добавление объекта
    public T create(T object) {
        validate(object); //проверяем корректность входного объекта
        return storage.create(object).orElseThrow(() -> {
            log.error("Объект с идентификатором " + object.getId() + " уже существует.");
            return new ObjectAlreadyExistException(object.getId());
        });
    }

    //обновление объекта
    public T update(T object) {
        validate(object); //проверяем корректность входного объекта
        return storage.update(object).orElseThrow(() -> {
            log.error("Объект с идентификатором " + object.getId() + " не существует.");
            return new ObjectNotExistException(object.getId());
        });
    }

    //////////////////////////// Удаление объектов ///////////////////////////

    //удаление по идентификатору
    public T delete(long id) {
        return storage.delete(id).orElseThrow(() -> {
            String message = "Объект с идентификатором %s не существует.";
            log.error(String.format(message, id));
            return new IncorrectParameterException(message, id);
        });
    }

    //удаление всех объектов
    public void deleteAll() {
        storage.deleteAll();
    }

    //////////////////////////// Валидация объектов //////////////////////////

    protected abstract void validate(T object);
}
