package ru.yandex.practicum.filmorate.exception;

public class ObjectAlreadyExistException extends RuntimeException {
    private final String key;
    private final long id;

    public ObjectAlreadyExistException(String key, long id) {
        this.key = key;
        this.id = id;
    }

    @Override
    public String getMessage() {
        switch (key) {
            case "User":
                return "Пользователь с идентификатором " + id + " уже существует.";
            case "Film":
                return "Фильм с идентификатором " + id + " уже существует.";
            default:
                return "Объект с идентификатором " + id + " уже существует.";
        }
    }
}
