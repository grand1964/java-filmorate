package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component("userDb")
public class DbUserStorage extends DbBaseUserStorage implements UserStorage {

    public DbUserStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    ////////////////////////////////// Чтение ////////////////////////////////

    //проверка наличия пользователя
    @Override
    public boolean contains(long id) {
        String sqlQuery = "select id from users where id = ?";
        List<Long> ids = jdbcTemplate.getJdbcTemplate().query(
                sqlQuery, (rs, n) -> rs.getLong("id"), id);
        return ids.size() > 0;
    }

    //возвращает пользователя по идентификатору
    @Override
    public Optional<User> get(long id) {
        //читаем пользователя с неподтвержденными связями
        String sqlQuery = "select u.*, f.friend_id from users as u " +
                "left join friends as f on u.id = f.user_id " +
                "where u.id = ?";
        Map<Long, User> map = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(sqlQuery, (rs) -> {
            User.mapFullRowToUser(rs, map);
        }, id);
        if (map.size() == 0) { //пользователя нет
            return Optional.empty(); //возвращаем пустой результат
        }
        //создаем таблицу подтверждений
        String sqlSubquery = "select id from users where id = :user_id"; //запрос всех друзей
        Map<String, Long> params = new HashMap<>();
        params.put("user_id", id);
        Map<Long, List<Long>> ackMap = getAcknowledgedFriendsForSet(sqlSubquery, params);
        //возвращаем пользователя (он может быть только один)
        return Optional.of(getUsersWithAcknowledgedLinks(map, ackMap).get(0));
    }

    //возвращает список всех пользователей
    @Override
    public List<User> getAll() {
        String sqlQuery = "select u.*, uf.friend_id from users as u " +
                "left join friends as uf on u.id = uf.user_id ";
        Map<Long, User> map = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(sqlQuery, (rs) -> {
            User.storeFullRowForAll(rs, map);
        });
        return map.values().stream().sorted(Comparator.comparingLong(User::getId)).collect(Collectors.toList());
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
}
