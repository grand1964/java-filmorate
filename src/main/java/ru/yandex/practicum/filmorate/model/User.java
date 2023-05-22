package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.storage.Storable;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class User implements Storable {
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
    private Set<Long> friends;

    public Set<Long> getFriends() {
        if (friends == null) {
            friends = new HashSet<>();
        }
        return friends;
    }

    public boolean addFriend(long userId) {
        if (isFriendPresent(userId)) {
            return false;
        }
        friends.add(userId);
        return true;
    }

    public boolean removeFriend(long userId) {
        if (!isFriendPresent(userId)) {
            return false;
        }
        friends.remove(userId);
        return true;
    }

    private boolean isFriendPresent(long friendId) {
        if (friends == null) {
            friends = new HashSet<>();
            return false;
        }
        return friends.contains(friendId);
    }
}
