package ru.yandex.practicum.filmorate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.filmorate.exception.*;
import ru.yandex.practicum.filmorate.model.ErrorResponse;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice(basePackages = "ru.yandex.practicum.filmorate")
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ObjectNotExistException e) {
        return new ErrorResponse(e.getMessage(), "404");
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleObjectAlreadyExist(ObjectAlreadyExistException e) {
        return new ErrorResponse(e.getMessage(), "409");
    }

    @ExceptionHandler({ValidateException.class, ConstraintViolationException.class,
            MethodArgumentNotValidException.class, IncorrectParameterFormatException.class,
            MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidate(RuntimeException e) {
        return new ErrorResponse(e.getMessage(), "400");
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleIncorrectParameter(IncorrectParameterException e) {
        return new ErrorResponse("Ошибка с полем " + e.getParameter(), "404");
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpectedError(Throwable e) {
        return new ErrorResponse("Произошла непредвиденная ошибка: " + e.getCause(), "500");
    }
}
