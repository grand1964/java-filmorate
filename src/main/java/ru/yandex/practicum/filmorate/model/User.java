package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class User {
    private long id;
    @NotBlank
    private String login;
    private String name;
    @NotBlank
    @Email
    private String email;
    @NotNull
    @PastOrPresent
    private LocalDate birthday;
}
