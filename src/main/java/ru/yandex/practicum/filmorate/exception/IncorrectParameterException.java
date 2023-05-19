package ru.yandex.practicum.filmorate.exception;

public class IncorrectParameterException extends RuntimeException {
    private final String message;
    private final String parameter;

    public IncorrectParameterException(String message, String parameter) {
        this.message = message;
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }

    @Override
    public String getMessage() {
        return String.format(message, parameter);
    }
}
