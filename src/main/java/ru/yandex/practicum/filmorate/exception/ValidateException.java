package ru.yandex.practicum.filmorate.exception;

public class ValidateException extends RuntimeException {
    private final String message;

    public ValidateException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
