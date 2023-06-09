package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Set;

public interface UserStorage extends Storage<User> {
    boolean addFriend(long userId, long friendId);

    List<User> getFriends(long id);

    Set<Long> getFriendIds(long id);

    List<User> getAcknowledgedFriends(long id);

    Set<Long> getAcknowledgedFriendIds(long id);

    List<User> getCommonFriends(long id1, long id2);

    boolean deleteFriend(long userId, long friendId);

    void deleteAllFriends(long userId);

    void deleteAllSubscribes(long userId);
}
