package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterException;
import ru.yandex.practicum.filmorate.exception.IncorrectParameterFormatException;
import ru.yandex.practicum.filmorate.exception.ObjectAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.ObjectNotExistException;
import ru.yandex.practicum.filmorate.storage.Storable;
import ru.yandex.practicum.filmorate.storage.Storage;

import java.util.List;
import java.util.Optional;

public abstract class AbstractService<T extends Storable> {
    protected Storage<T> storage;
    protected static Logger log;

    public AbstractService(Storage<T> storage) {
        this.storage = storage;
        log = LoggerFactory.getLogger(getClass());
    }

    ///////////////////////////// Чтение объектов ////////////////////////////

    //получение объекта по идентификатору
    public T get(String paramId) {
        long id = validateId(paramId);
        if (id == -1) {
            throw new IncorrectParameterException("Задан ошибочный идентификатор: ", paramId);
        }
        return storage.get(id).orElseThrow();
    }

    //получение списка всех объектов
    public List<T> getAll() {
        return storage.getAll();
    }

    ///////////////////////////// Запись объектов ////////////////////////////

    //добавление объекта
    public T create(T object) {
        validate(object); //проверяем корректность входного объекта
        Optional<T> outObject = storage.create(object); //пытаемся записать его в хранилище
        if (outObject.isEmpty()) { //такой объект уже есть
            log.error("Объект с идентификатором " + object.getId() + " уже существует.");
            throw new ObjectAlreadyExistException(object.getId());
        }
        return outObject.orElseThrow();
    }

    //обновление объекта
    public T update(T object) {
        validate(object); //проверяем корректность входного объекта
        Optional<T> outObject = storage.update(object); //пытаемся записать его в хранилище
        if (outObject.isEmpty()) { //объекта с таким идентификатором нет
            log.error("Объект с идентификатором " + object.getId() + " не существует.");
            throw new ObjectNotExistException(object.getId());
        }
        return outObject.orElseThrow();
    }

    //////////////////////////// Удаление объектов ///////////////////////////

    //удаление по идентификатору
    public T delete(String param) {
        long id = validateId(param);
        Optional<T> outObject = storage.delete(id);
        if (outObject.isEmpty()) { //объекта с таким идентификатором нет
            String message = "Объект с идентификатором %s не существует.";
            log.error(String.format(message, param));
            throw new IncorrectParameterException(message, param);
        }
        return outObject.orElseThrow();
    }

    //удаление всех объектов
    public void deleteAll() {
        storage.deleteAll();
    }

    //////////////////////////// Валидация объектов //////////////////////////

    protected abstract void validate(T object);

    protected long validateId(String paramId) {
        try {
            return Long.parseLong(paramId);
        } catch (NumberFormatException e) {
            log.error("Задан нечисловой параметр: " + paramId);
            throw new IncorrectParameterFormatException("Задан нечисловой параметр: ", paramId);
        }
    }
}
