package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component("LikeDb")
public class DbLikeStorage implements LikeStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DbLikeStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //////////////////////////// Поддержка лайков ////////////////////////////

    //выдает список пользователей, поставивших лайки на фильм
    @Override
    public List<User> getLikes(long filmId) {
        String sqlQuery = "select u.id, u.login, u.name, u.email, u.birthday from users as u " +
                "inner join likes as l on l.user_id = u.id " +
                "where l.film_id = ? " +
                "order by u.id asc";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, User::mapRowToUser, filmId);
    }

    //выдает список идентификаторов пользователей, поставивших лайки на фильм
    @Override
    public Set<Long> getLikeIds(long filmId) {
        String sqlQuery = "select user_id from likes where film_id = ?";
        return new HashSet<>(jdbcTemplate.getJdbcTemplate().query(sqlQuery,
                (rs, n) -> rs.getLong("user_id"), filmId));
    }

    //добавляет лайк фильму
    @Override
    public boolean addLike(long filmId, long userId) {
        //В Postgre заменить конструкцией insert ... on conflict do nothing
        String sqlQuery = "merge into likes (film_id, user_id) " +
                "values (:film_id, :user_id)";
        return jdbcTemplate.update(sqlQuery, mapLikeIds(filmId, userId)) > 0;
    }

    @Override
    public void storeAllLikes(Long filmId, Set<Long> likes) {
        List<Long> list = new ArrayList<>(likes);
        jdbcTemplate.getJdbcTemplate().batchUpdate(
                "merge into likes (film_id, user_id) values (?, ?)",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, filmId);
                        ps.setLong(2, list.get(i));
                    }

                    public int getBatchSize() {
                        return list.size();
                    }
                });
    }

    //убирает лайк с фильма
    @Override
    public boolean deleteLike(long filmId, long userId) {
        String sqlQuery = "delete from likes where film_id = :film_id  and user_id = :user_id";
        return jdbcTemplate.update(sqlQuery, mapLikeIds(filmId, userId)) > 0;
    }

    //удаляет все лайки с фильма
    @Override
    public void deleteAllLikes(long filmId) {
        String sqlQuery = "delete from likes where film_id = ?";
        jdbcTemplate.getJdbcTemplate().update(sqlQuery, filmId);
    }

    //возвращает хит фильмов (по числу лайков)
    @Override
    public List<Film> getTopFilms(Long count) {
        String sqlQuery = "select f.*, l.user_id, g.id, g.name, m.name from films as f " +
                "left join mpa as m on f.mpa_id = m.id " +
                "left join likes as l on f.id = l.film_id " +
                "left join film_genres as fg on fg.film_id = f.id " +
                "left join genres as g on g.id = fg.genre_id " +
                "where f.id in" +
                "(select id from films left join likes " +
                "on id = film_id group by id " +
                "order by count(user_id) desc " +
                "limit ?)";
        Map<Long, Film> map = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(sqlQuery, (rs) -> {
            Film.storeFullRow(rs, map);
        }, count);
        return map.values().stream()
                .sorted(Comparator.comparingInt(f -> ((Film) f).getLikes().size()).reversed())
                .collect(Collectors.toList());
    }

    //////////////////////////// Поддержка маппинга //////////////////////////

    //преобразует пару "фильм-лайк" в блок параметров
    private Map<String, Long> mapLikeIds(long filmId, long userId) {
        Map<String, Long> map = new HashMap<>();
        map.put("film_id", filmId);
        map.put("user_id", userId);
        return map;
    }
}
