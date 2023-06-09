package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

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

    @Override
    public Optional<Film> get(long id) {
        String sqlQuery = "select * from films where id = ?";
        List<Film> list = jdbcTemplate.getJdbcTemplate().query(sqlQuery, Film::mapRowToFilm, id);
        if (list.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(list.get(0)); //фильм может быть лишь один
        }
    }

    @Override
    public List<Film> getAll() {
        String sqlQuery = "select * from films";
        return jdbcTemplate.query(sqlQuery, Film::mapRowToFilm);
    }

    ////////////////////////////////// Создание //////////////////////////////

    @Override
    public void create(Film film) {
        //добавляем пользователя и возвращаем присвоенный ему идентификатор
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

    //////////////////////////////////////////////////////////////////////////
    /////////////////////////// Действия с фильмами //////////////////////////
    //////////////////////////////////////////////////////////////////////////

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
        //!!! В Postgre заменить конструкцией insert ... on conflict do nothing
        String sqlQuery = "merge into likes (film_id, user_id) " +
                "values (:film_id, :user_id)";
        return jdbcTemplate.update(sqlQuery, mapLikeIds(filmId, userId)) > 0;
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

    //////////////////////////// Поддержка жанров ////////////////////////////

    //возвращает жанр по идентификатору
    @Override
    public Optional<Genre> getGenre(long genreId) {
        String sqlQuery = "select * from genres where id = ?";
        List<Genre> genres = jdbcTemplate.getJdbcTemplate().query(
                sqlQuery, Genre::mapRowToGenre, genreId);
        if (genres.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(genres.get(0));
        }
    }

    //возвращает полный список жанров из базы
    @Override
    public List<Genre> getAllGenres() {
        String sqlQuery = "select * from genres order by id asc";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, Genre::mapRowToGenre);
    }

    //добавляет жанр требуемому фильму
    @Override
    public void addFilmGenre(long filmId, long genreId) {
        //В Postgre заменить конструкцией insert ... on conflict do nothing
        String sqlQuery = "merge into film_genres (film_id, genre_id) " +
                "values (:film_id, :genre_id)";
        jdbcTemplate.update(sqlQuery, mapGenreIds(filmId, genreId));
    }

    //возвращает все жанры заданного фильма
    @Override
    public List<Genre> getFilmGenres(long filmId) {
        String sqlQuery = "select g.id, g.name from film_genres as f " +
                "inner join genres as g on f.genre_id = g.id " +
                "where f.film_id = ? " +
                "order by f.film_id asc";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, Genre::mapRowToGenre, filmId);
    }

    //обнуляет массив жанров фильма
    @Override
    public void deleteFilmGenres(long filmId) {
        String sqlQuery = "delete from film_genres where film_id = ?";
        jdbcTemplate.getJdbcTemplate().update(sqlQuery, filmId);
    }

    /////////////////////////// Поддержка рейтингов //////////////////////////

    //возвращает рейтинг по идентификатору
    @Override
    public Optional<Mpa> getMpa(long mpaId) {
        String sqlQuery = "select * from mpa where id = ?";
        List<Mpa> mpa = jdbcTemplate.getJdbcTemplate().query(sqlQuery, Mpa::mapRowToMpa, mpaId);
        if (mpa.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(mpa.get(0));
        }
    }

    //возвращает все рейтинги из базы
    @Override
    public List<Mpa> getAllMpa() {
        String sqlQuery = "select * from mpa order by id";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, Mpa::mapRowToMpa);
    }

    //возвращает хит фильмов (по числу лайков)
    @Override
    public List<Film> getTopFilms(Long count) {
        String sqlQuery = "select f.* from films as f " +
                "left join likes as l on f.id = l.film_id " +
                "group by f.id " +
                "order by count(l.user_id) desc " +
                "limit ?";
        return jdbcTemplate.getJdbcTemplate().query(sqlQuery, Film::mapRowToFilm, count);
    }

    //////////////////////////////////////////////////////////////////////////
    //////////////////////////// Поддержка маппинга //////////////////////////
    //////////////////////////////////////////////////////////////////////////

    //преобразует пару "фильм-лайк" в блок параметров
    private Map<String, Long> mapLikeIds(long filmId, long userId) {
        Map<String, Long> map = new HashMap<>();
        map.put("film_id", filmId);
        map.put("user_id", userId);
        return map;
    }

    //преобразует пару "фильм-жанр" в блок параметров
    private Map<String, Long> mapGenreIds(long filmId, long genreId) {
        Map<String, Long> map = new HashMap<>();
        map.put("film_id", filmId);
        map.put("genre_id", genreId);
        return map;
    }
}
