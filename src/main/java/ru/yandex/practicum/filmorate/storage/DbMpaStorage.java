package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

@Component("mpaDb")
public class DbMpaStorage implements MpaStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DbMpaStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
}
