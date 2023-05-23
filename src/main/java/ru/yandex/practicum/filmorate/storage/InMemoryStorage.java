package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class InMemoryStorage<T extends Storable> implements Storage<T> {
    protected long currentId;
    protected Map<Long, T> data;

    public InMemoryStorage() {
        currentId = 0;
        data = new HashMap<>();
    }

    /////////////////////////// Реализация Storage ///////////////////////////

    @Override
    public Optional<T> get(long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<T> getAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public Optional<T> create(T object) {
        if (data.containsKey(object.getId())) { //объект уже существует
            return Optional.empty(); //ошибка, возвращаем пустой объект
        }
        //далее - случай корректного объекта
        object.setId(++currentId); //присваиваем ему идентификатор
        data.put(currentId, object); //запоминаем его
        log.info("Добавлен новый объект: " + object);
        return Optional.of(object);
    }

    @Override
    public Optional<T> update(T object) {
        if (!data.containsKey(object.getId())) { //объекта с таким идентификатором не существует
            return Optional.empty(); //ошибка, возвращаем пустой объект
        }
        //далее - случай корректного объекта
        data.put(object.getId(), object); //обновляем объект
        log.info("Объект с идентификатором " + object.getId() + " заменен на " + object);
        return Optional.of(object);
    }

    @Override
    public Optional<T> delete(long id) {
        if (!data.containsKey(id)) {
            return Optional.empty();
        }
        T object = data.get(id); //читаем удаляемый объект
        data.remove(id); //удаляем его их хранилища
        log.info("Объект с идентификатором " + id + " удален");
        return Optional.of(object); //возвращаем удаленный объект
    }

    @Override
    public void deleteAll() {
        data.clear(); //очищаем данные
        currentId = 0; //сбрасываем счетчик идентификаторов
    }
}
