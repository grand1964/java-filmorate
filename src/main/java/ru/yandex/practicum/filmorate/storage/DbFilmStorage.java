package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component("FilmDb")
public class DbFilmStorage implements FilmStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DbFilmStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //////////////////////////////////////////////////////////////////////////
    ///////////////////////// Реализация операций CRUD ///////////////////////
    //////////////////////////////////////////////////////////////////////////

    ////////////////////////////////// Чтение ////////////////////////////////

    //проверка наличия фильма
    @Override
    public boolean contains(long id) {
        String sqlQuery = "select id from films where id = ?";
        List<Long> ids = jdbcTemplate.getJdbcTemplate().query(
                sqlQuery, (rs, n) -> rs.getLong("id"), id);
        return ids.size() > 0;
    }

    //получение фильма по идентификатору
    @Override
    public Optional<Film> get(long id) {
        String sqlQuery = "select f.*, l.user_id, g.id, g.name, m.name from films as f " +
                "left join mpa as m on f.mpa_id = m.id " +
                "left join likes as l on f.id = l.film_id " +
                "left join film_genres as fg on fg.film_id = f.id " +
                "left join genres as g on g.id = fg.genre_id " +
                "where f.id = ?";
        Map<Long, Film> map = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(sqlQuery, (rs) -> {
            Film.storeFullRow(rs, map);
        }, id);
        if (map.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(map.get(id)); //фильм может быть лишь один
        }
    }

    //получение всех фильмов со всеми связями
    @Override
    public List<Film> getAll() {
        String sqlQuery = "select f.*, l.user_id, g.id, g.name, m.name from films as f " +
                "left join mpa as m on f.mpa_id = m.id " +
                "left join likes as l on f.id = l.film_id " +
                "left join film_genres as fg on fg.film_id = f.id " +
                "left join genres as g on g.id = fg.genre_id";
        Map<Long, Film> map = new HashMap<>();
        jdbcTemplate.getJdbcTemplate().query(sqlQuery, (rs) -> {
            Film.storeFullRow(rs, map);
        });
        return map.values().stream().sorted(Comparator.comparingLong(Film::getId)).collect(Collectors.toList());
    }

    ////////////////////////////////// Создание //////////////////////////////

    @Override
    public void create(Film film) {
        //добавляем фильм и возвращаем присвоенный ему идентификатор
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("films")
                .usingGeneratedKeyColumns("id");
        long id = simpleJdbcInsert.executeAndReturnKey(film.toMap()).longValue();
        //устанавливаем фильму правильный идентификатор
        film.setId(id);
    }

    ///////////////////////////////// Обновление /////////////////////////////

    @Override
    public boolean update(Film film) {
        //формируем блок параметров
        Map<String, Object> params = film.toMap(); //из таблицы films (кроме id)
        params.put("id", film.getId()); //id устанавливаем отдельно
        //определяем запрос
        String sqlQuery = "update films set name = :name, description = :description, " +
                "release_date = :release_date, duration = :duration, " +
                "mpa_id = :mpa_id where id = :id";
        //выполняем обновление
        return jdbcTemplate.update(sqlQuery, params) > 0; //признак обновления
    }

    ////////////////////////////////// Удаление //////////////////////////////

    @Override
    public boolean delete(long id) {
        String sqlQuery = "delete from films where id = ?";
        return jdbcTemplate.getJdbcTemplate().update(sqlQuery, id) > 0;
    }

    @Override
    public int deleteAll() {
        String sqlQuery = "delete from films";
        return jdbcTemplate.getJdbcTemplate().update(sqlQuery);
    }
}
