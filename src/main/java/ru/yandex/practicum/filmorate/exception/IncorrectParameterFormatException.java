package ru.yandex.practicum.filmorate.exception;

public class IncorrectParameterFormatException extends RuntimeException {
    private final String message;
    private final String parameter;

    public IncorrectParameterFormatException(String message, String parameter) {
        this.message = message;
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }

    @Override
    public String getMessage() {
        return message + parameter;
    }
}
