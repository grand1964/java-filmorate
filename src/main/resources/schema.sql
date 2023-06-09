CREATE TABLE IF NOT EXISTS users(
    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    login varchar(40) NOT NULL,
    name varchar(40),
    email varchar(40) NOT NULL,
    birthday date,
    CONSTRAINT users_pk PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS friends(
    user_id INTEGER,
    friend_id INTEGER,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE IF NOT EXISTS films(
    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name varchar(40) NOT NULL,
    description varchar(200),
    release_date date NOT NULL,
    duration INTEGER,
    mpa_id INTEGER,
    CONSTRAINT films_pk PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS likes(
    film_id INTEGER,
    user_id INTEGER,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES films (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE IF NOT EXISTS genres(
    id INTEGER,
    name varchar(40),
    PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS film_genres(
    film_id INTEGER,
    genre_id INTEGER,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES films (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres (id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE IF NOT EXISTS mpa(
    id INTEGER,
    name varchar(40),
    PRIMARY KEY (id)
);