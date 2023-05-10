package ru.yandex.practicum.filmorate.exception;

public class ObjectNotExistException extends RuntimeException {
    private final String key;
    private final long id;

    public ObjectNotExistException(String key, long id) {
        this.key = key;
        this.id = id;
    }

    @Override
    public String getMessage() {
        switch (key) {
            case "User":
                return "Пользователя с идентификатором " + id + " не существует.";
            case "Film":
                return "Фильма с идентификатором " + id + " не существует.";
            default:
                return "Объекта с идентификатором " + id + " не существует.";
        }
    }
}
