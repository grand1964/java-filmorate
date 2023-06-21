package ru.yandex.practicum.filmorate.model;

import lombok.*;
import ru.yandex.practicum.filmorate.storage.Storable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
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
    private Map<Long, Boolean> friends;

    ////////////////////// Обновление коллекции друзей ///////////////////////

    public void addFriend(long friendId, boolean ack) {
        if (friends == null) {
            friends = new HashMap<>();
        }
        friends.put(friendId, ack);
    }

    /////////////////////////////// Конвертация //////////////////////////////

    //упаковка полей пользователя в отображение "поле -> значение" (используется как параметр в запросах)
    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<>();
        values.put("login", login);
        values.put("name", name);
        values.put("email", email);
        values.put("birthday", birthday);
        return values;
    }

    //распаковка строки таблицы users в пользователя
    public static User mapRowToUser(ResultSet resultSet, int rowNum) throws SQLException {
        User user = User.builder()
                .id(resultSet.getLong("users.id"))
                .login(resultSet.getString("users.login"))
                .name(resultSet.getString("users.name"))
                .email(resultSet.getString("users.email"))
                .birthday(resultSet.getDate("users.birthday").toLocalDate())
                .build();
        user.setFriends(new HashMap<>());
        return user;
    }

    //распаковывает пользователя с неподтвержденными связями и сохраняет его в map
    public static void mapFullRowToUser(ResultSet rs, Map<Long, User> map) throws SQLException {
        User user;
        //читаем идентификатор
        long userId = rs.getLong("id");
        if (!map.containsKey(userId)) { //пользователя еще не было в map
            user = mapRowToUser(rs, 0); //создаем его из набора
        } else { //он уже был
            user = map.get(userId); //читаем его из map
        }
        long friendId = rs.getLong("friend_id"); //идентификатор друга
        if (friendId > 0) { //друг есть
            user.addFriend(friendId, false); //устанавливаем неподтвержденную связь
        }
        map.put(userId, user); //сохраняем пользователя
    }

    //распаковывает пользователя с подтвержденными связями в map (только для getAll)
    public static void storeFullRowForAll(ResultSet rs, Map<Long, User> map) throws SQLException {
        User user;
        //читаем идентификатор
        long userId = rs.getLong("id");
        if (!map.containsKey(userId)) { //пользователя еще не было в map
            user = mapRowToUser(rs, 0); //создаем его из набора
        } else { //он уже был
            user = map.get(userId); //читаем его из map
        }
        long friendId = rs.getLong("friend_id"); //идентификатор друга
        if (friendId <= 0) { //друга нет
            map.put(userId, user); //сохраняем пользователя
            return; //и выходим
        }
        boolean ack = false; //условие взаимности
        User friend = null;
        if (map.containsKey(friendId)) { //друг уже встречался в наборе
            friend = map.get(friendId); //читаем его
            ack = (friend.getFriends() != null) && (friend.getFriends().containsKey(userId));
        }
        if (!ack) { //дружба не взаимная
            user.addFriend(friendId, false);
        } else { //взаимная
            user.addFriend(friendId, true);
            friend.addFriend(userId, true);
        }
        //пишем пользователя в набор
        map.put(userId, user);
    }
}