package ru.yandex.practicum.filmorate.exception;

public class ObjectNotExistException extends RuntimeException {
    private final long id;

    public ObjectNotExistException(long id) {
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "Объекта с идентификатором " + id + " не существует.";
    }
}
