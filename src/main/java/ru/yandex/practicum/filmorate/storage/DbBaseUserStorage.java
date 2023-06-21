package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbBaseUserStorage {
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DbBaseUserStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        строит таблицу подтвержденных дружеских связей для набора пользователей
        sqlSubquery - запрос, возвращающий требуемый набор пользователей
        params - параметры для запроса
     */
    protected Map<Long, List<Long>> getAcknowledgedFriendsForSet(String sqlSubquery, Map<String, ?> params) {
        String sqlQuery = String.format("select uf.* from friends as uf " +
                "inner join friends as fu on uf.friend_id = fu.user_id " +
                "where uf.user_id = fu.friend_id and uf.user_id in (%s)", sqlSubquery);
        Map<Long, List<Long>> map = new HashMap<>();
        jdbcTemplate.query(sqlQuery, params, rs -> {
            storeAcknowledgedLink(rs, map);
        });
        return map;
    }

    /*
        Возвращает список пользователей с подтвержденными связями
        usersMap - набор пользователей с неподтвержденными связями
        ackMap - таблица подтверждений для данного набора
     */
    protected List<User> getUsersWithAcknowledgedLinks(Map<Long, User> subscribers, Map<Long, List<Long>> ack) {
        for (Long friendId : subscribers.keySet()) { //цикл по друзьям
            Map<Long, Boolean> friends = subscribers.get(friendId).getFriends(); //список всех друзей
            List<Long> ackFriends = ack.get(friendId); //подтвержденные связи для friendId
            if (ackFriends != null) { //они есть
                for (Long ackId : ack.get(friendId)) { //цикл по подтвержденным связям
                    friends.put(ackId, true); //подтверждаем дружбу
                }
            }
        }
        return new ArrayList<>(subscribers.values());
    }

    //////////////////////////// Функция отображения /////////////////////////

    //преобразует пару "пользователь-друг" в блок параметров
    protected Map<String, Long> mapIds(long userId, long friendId) {
        Map<String, Long> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("friend_id", friendId);
        return map;
    }

    //преобразует результаты запроса в таблицу подтвержденных друзей
    protected void storeAcknowledgedLink(ResultSet rs, Map<Long, List<Long>> map) throws SQLException {
        long userId = rs.getLong("user_id"); //идентификатор пользователя
        List<Long> list;
        if (map.containsKey(userId)) { //он уже есть в таблице
            list = map.get(userId); //читаем его связи
        } else { //его еще не было
            //сохраняем его с пустым списком
            list = new ArrayList<>();
            map.put(userId, list);
        }
        long friendId = rs.getLong("friend_id"); //читаем друга
        list.add(friendId); //вносим его в список
    }
}
