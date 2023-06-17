package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("GenreDb")
public class DbGenreStorage implements GenreStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DbGenreStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    public void setFilmGenres(Long filmId, List<Genre> genres) {
        jdbcTemplate.getJdbcTemplate().batchUpdate(
                "merge into film_genres (film_id, genre_id) values (?, ?) ",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, filmId);
                        Genre genre = genres.get(i);
                        ps.setLong(2, genre.getId());
                    }
                    public int getBatchSize() {
                        return genres.size();
                    }
                });
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
}
