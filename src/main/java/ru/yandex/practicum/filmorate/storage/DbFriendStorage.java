package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("friendDb")
public class DbFriendStorage extends DbBaseUserStorage implements FriendStorage {

    public DbFriendStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    ///////////////////////////// Добавление друзей //////////////////////////

    //добавление друга (подписчика)
    @Override
    public boolean addFriend(long userId, long friendId) {
        //В Postgre заменить конструкцией insert ... on conflict do nothing
        String sqlQuery = "merge into friends (user_id, friend_id) " +
                "values (:user_id, :friend_id)";
        return jdbcTemplate.update(sqlQuery, mapIds(userId, friendId)) > 0;
    }

    //добавление в friends всех дружеских связей заданного пользователя
    @Override
    public void addFriendsOfUser(User user) {
        long userId = user.getId();
        //читаем друзей
        Map<Long, Boolean> friends = user.getFriends();
        if (friends == null) { //друзья не заданы
            user.setFriends(new HashMap<>()); //устанавливаем пустой набор друзей
            return; //в базе менять нечего
        }
        //устанавливаем прямые связи
        List<Long> friendIds = new ArrayList<>(friends.keySet());
        jdbcTemplate.getJdbcTemplate().batchUpdate(
                "merge into friends (user_id, friend_id) values (?, ?) ",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, userId);
                        ps.setLong(2, friendIds.get(i));
                    }

                    public int getBatchSize() {
                        return friendIds.size();
                    }
                });
        //устанавливаем обратные связи
        List<Long> ackFriendIds = friendIds.stream().filter(friends::get).collect(Collectors.toList());
        jdbcTemplate.getJdbcTemplate().batchUpdate(
                "merge into friends (user_id, friend_id) values (?, ?) ",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, ackFriendIds.get(i));
                        ps.setLong(2, userId);
                    }

                    public int getBatchSize() {
                        return ackFriendIds.size();
                    }
                });
    }

    ///////////////////////////// Получение друзей ///////////////////////////

    //получение всех подписчиков
    @Override
    public List<User> getFriends(long id) {
        //читаем друзей с неподтвержденными связями
        String sqlQuery = "select u.*, f.friend_id from users as u " +
                "inner join friends as uf on uf.friend_id = u.id " +
                "left join friends as f on f.user_id = u.id " +
                "where uf.user_id = ? " +
                "order by u.id asc";
        Map<Long, User> friendsMap = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(sqlQuery, (rs) -> {
            User.mapFullRowToUser(rs, friendsMap);
        }, id);
        //создаем таблицу подтверждений
        String sqlSubquery = "select friend_id from friends where user_id = :user_id"; //запрос всех друзей
        Map<String, Long> params = new HashMap<>();
        params.put("user_id", id);
        Map<Long, List<Long>> ackMap = getAcknowledgedFriendsForSet(sqlSubquery, params);
        return getUsersWithAcknowledgedLinks(friendsMap, ackMap);
    }

    //получение подтвержденных друзей (взаимных подписчиков)
    @Override
    public List<User> getAcknowledgedFriends(long id) {
        //читаем друзей с неподтвержденными связями
        String sqlQuery = "select u.*, l.friend_id from friends as l " +
                "right join users as u on u.id = l.user_id " +
                "where u.id in (select uf.friend_id from friends as uf " +
                "inner join friends as fu on uf.friend_id = fu.user_id " +
                "where uf.user_id = :user_id and fu.friend_id = :user_id) " +
                "order by u.id asc";
        Map<String, Long> params = new HashMap<>();
        params.put("user_id", id);
        Map<Long, User> friendsMap = new HashMap<>();
        jdbcTemplate.query(sqlQuery, params, (rs) -> {
            User.mapFullRowToUser(rs, friendsMap);
        });
        //создаем отображение подтверждений
        String sqlSubquery = "select friend_id from friends where user_id = :user_id"; //запрос всех друзей
        Map<Long, List<Long>> ackMap = getAcknowledgedFriendsForSet(sqlSubquery, params); //подтвержденные связи
        //выставляем подтверждения в результатах первого запроса
        return getUsersWithAcknowledgedLinks(friendsMap, ackMap);
    }

    //получение общих подписчиков (неподтвержденных друзей)
    @Override
    public List<User> getCommonFriends(long id1, long id2) {
        //читаем общих друзей с неподтвержденными связями
        String sqlQuery = "select u.*, l.friend_id from friends as l " +
                "right join users as u on u.id = l.user_id " +
                "inner join friends as f1 on u.id = f1.friend_id " +
                "inner join friends as f2 on f1.friend_id = f2.friend_id " +
                "where f1.user_id = ? and f2.user_id = ? " +
                "order by u.id asc";
        Map<Long, User> friendsMap = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(
                sqlQuery, (rs) -> {
                    User.mapFullRowToUser(rs, friendsMap);
                }, id1, id2);
        //создаем отображение подтверждений
        String sqlSubquery = "select f1.friend_id from friends as f1 " +
                "inner join friends as f2 on f1.friend_id = f2.friend_id " +
                "where f1.user_id = :id1 and f2.user_id = :id2"; //запрос идентификаторов общих друзей
        Map<String, Long> params = new HashMap<>();
        params.put("id1", id1);
        params.put("id2", id2);
        Map<Long, List<Long>> ackMap = getAcknowledgedFriendsForSet(sqlSubquery, params);
        //выставляем подтверждения в результатах первого запроса
        return getUsersWithAcknowledgedLinks(friendsMap, ackMap);
    }

    ////////////////////////////// Удаление друзей ///////////////////////////

    //удаление подписчика
    @Override
    public boolean deleteFriend(long userId, long friendId) {
        String sqlQuery = "delete from friends where user_id = :user_id and friend_id = :friend_id";
        return jdbcTemplate.update(sqlQuery, mapIds(userId, friendId)) > 0;
    }

    //удаление всех подписчиков
    @Override
    public void deleteAllFriends(long userId) {
        String sqlQuery = "delete from friends where user_id = ?";
        jdbcTemplate.getJdbcTemplate().update(sqlQuery, userId);
    }

    //удаление пользователя из всех подписок
    @Override
    public void deleteAllSubscribes(long userId) {
        String sqlQuery = "delete from friends where friend_id = ?";
        jdbcTemplate.getJdbcTemplate().update(sqlQuery, userId);
    }
}
