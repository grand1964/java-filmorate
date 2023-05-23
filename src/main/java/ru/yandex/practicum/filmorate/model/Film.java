package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.storage.Storable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film implements Storable {
    private long id;
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private LocalDate releaseDate;
    @Min(value = 1)
    private int duration;
    private Set<Long> likes;

    public Set<Long> getLikes() {
        if (likes == null) {
            likes = new HashSet<>();
        }
        return likes;
    }

    public boolean addLike(long userId) {
        if (isLikePresent(userId)) {
            return false;
        }
        likes.add(userId);
        return true;
    }

    public boolean removeLike(long userId) {
        if (!isLikePresent(userId)) {
            return false;
        }
        likes.remove(userId);
        return true;
    }

    private boolean isLikePresent(long userId) {
        if (likes == null) {
            likes = new HashSet<>();
            return false;
        }
        return likes.contains(userId);
    }
}
