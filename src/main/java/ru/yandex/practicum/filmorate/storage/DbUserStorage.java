package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component("userDb")
public class DbUserStorage implements UserStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DbUserStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    ////////////////////////////////// Чтение ////////////////////////////////

    //возвращает пользователя по идентификатору
    @Override
    public Optional<User> get(long id) {
        String sqlQuery = "select * from users where id = ?";
        List<User> list = jdbcTemplate.getJdbcTemplate().query(sqlQuery, User::mapRowToUser, id);
        if (list.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(list.get(0)); //пользователь может быть лишь один
        }
    }

    //возвращает список всех пользователей
    @Override
    public List<User> getAll() {
        String sqlQuery = "select * from users";
        return jdbcTemplate.query(sqlQuery, User::mapRowToUser);
    }

    ////////////////////////////////// Создание //////////////////////////////

    //создает в базе нового пользователя
    @Override
    public void create(User user) {
        //добавляем пользователя и возвращаем присвоенный ему идентификатор
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
        long id = simpleJdbcInsert.executeAndReturnKey(user.toMap()).longValue();
        //устанавливаем пользователю правильный идентификатор
        user.setId(id);
    }

    ///////////////////////////////// Обновление /////////////////////////////

    //обновляет пользователя в базе (по идентификатору)
    @Override
    public boolean update(User user) {
        //формируем блок параметров
        Map<String, Object> params = user.toMap(); //все кроме id
        params.put("id", user.getId()); //id добавляем отдельно
        //определяем запрос
        String sqlQuery = "update users set " +
                "login = :login, name = :name, email = :email, birthday = :birthday where id = :id";
        //выполняем обновление
        return jdbcTemplate.update(sqlQuery, params) > 0; //возвращаем признак успеха
    }

    ////////////////////////////////// Удаление //////////////////////////////

    //удаляет пользователя по идентификатору
    @Override
    public boolean delete(long id) {
        String sqlQuery = "delete from users where id = ?";
        return jdbcTemplate.getJdbcTemplate().update(sqlQuery, id) > 0;
    }

    //удаляет всех пользователей
    @Override
    public int deleteAll() {
        String sqlQuery = "delete from users";
        return jdbcTemplate.getJdbcTemplate().update(sqlQuery);
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////////// Поддержка друзей ///////////////////////////
    //////////////////////////////////////////////////////////////////////////

    ///////////////////////////////// Добавление /////////////////////////////

    //добавление друга (подписчика)
    @Override
    public boolean addFriend(long userId, long friendId) {
        //В Postgre заменить конструкцией insert ... on conflict do nothing
        String sqlQuery = "merge into friends (user_id, friend_id) " +
                "values (:user_id, :friend_id)";
        return jdbcTemplate.update(sqlQuery, mapIds(userId, friendId)) > 0;
    }

    ///////////////////////////////// Получение //////////////////////////////

    //получение всех подписчиков
    @Override
    public List<User> getFriends(long id) {
        String sqlQuery = "select u.* from users as u " +
                "inner join friends as f on f.friend_id = u.id " +
                "where f.user_id = ? " +
                "order by u.id asc";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, User::mapRowToUser, id);
    }

    //получение идентификаторов всех подписчиков
    public Set<Long> getFriendIds(long id) {
        String sqlQuery = "select friend_id from friends where user_id = ?";
        return new HashSet<>(jdbcTemplate.getJdbcTemplate().query(sqlQuery,
                (rs, n) -> rs.getLong("friend_id"), id));
    }

    //получение подтвержденных друзей (взаимных подписчиков)
    @Override
    public List<User> getAcknowledgedFriends(long id) {
        String sqlQuery = "select u.* from users as u " +
                "where u.id in (select uf.friend_id from friends as uf " +
                "inner join friends as fu on uf.user_id = :user_id and fu.friend_id = :user_id " +
                "where uf.friend_id = fu.user_id) " +
                "order by u.id asc";
        Map<String, Long> params = new HashMap<>();
        params.put("user_id", id);
        return jdbcTemplate.query(sqlQuery, params, User::mapRowToUser);
    }

    //получение идентификаторов подтвержденных друзей
    public Set<Long> getAcknowledgedFriendIds(long id) {
        String sqlQuery = "select uf.friend_id from friends as uf " +
                "inner join friends as fu on uf.user_id = :user_id and fu.friend_id = :user_id " +
                "where uf.friend_id = fu.user_id";
        Map<String, Long> params = new HashMap<>();
        params.put("user_id", id);
        return new HashSet<>(jdbcTemplate.query(sqlQuery, params,
                (rs, num) -> rs.getLong("friend_id")));
    }

    //получение общих подписчиков (неподтвержденных друзей)
    @Override
    public List<User> getCommonFriends(long id1, long id2) {
        String sqlQuery = "select u.* from users as u " +
                "where u.id in (select f1.friend_id from friends as f1 " +
                "inner join friends as f2 on f1.friend_id = f2.friend_id " +
                "where f1.user_id = ? and f2.user_id = ?) " +
                "order by u.id asc";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, User::mapRowToUser, id1, id2);
    }

    ////////////////////////////////// Удаление //////////////////////////////

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
    public void deleteAllSubscribes(long userId) {
        String sqlQuery = "delete from friends where friend_id = ?";
        jdbcTemplate.getJdbcTemplate().update(sqlQuery, userId);
    }

    //////////////////////////// Функция отображения /////////////////////////

    //преобразует пару "пользователь-друг" в блок параметров
    private Map<String, Long> mapIds(long userId, long friendId) {
        Map<String, Long> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("friend_id", friendId);
        return map;
    }
}
