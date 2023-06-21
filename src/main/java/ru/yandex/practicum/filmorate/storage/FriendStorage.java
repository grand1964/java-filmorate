package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface FriendStorage {
    boolean addFriend(long userId, long friendId);

    void addFriendsOfUser(User user);

    List<User> getFriends(long id);

    List<User> getAcknowledgedFriends(long id);

    List<User> getCommonFriends(long id1, long id2);

    boolean deleteFriend(long userId, long friendId);
}
