package ru.yandex.practicum.filmorate.exception;

public class ObjectAlreadyExistException extends RuntimeException {
    private final long id;

    public ObjectAlreadyExistException(long id) {
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "Объект с идентификатором " + id + " уже существует.";
    }
}
