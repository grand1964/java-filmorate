package ru.yandex.practicum.filmorate.exception;

public class IncorrectParameterException extends RuntimeException {
    private final String message;
    private final Long parameter;

    public IncorrectParameterException(String message, Long parameter) {
        this.message = message;
        this.parameter = parameter;
    }

    public Long getParameter() {
        return parameter;
    }

    @Override
    public String getMessage() {
        return String.format(message, parameter);
    }
}
