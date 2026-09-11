package ru.practicum.moviehub.api;

import ru.practicum.moviehub.model.Movie;

// для возврата JSONa с ид после успешного добавления фильма в список
public class MovieWithId {
    private int id;
    private String title;
    private int year;

    public MovieWithId(int id, Movie movie) {
        this.id = id;
        this.title = movie.getTitle();
        this.year = movie.getYear();
    }
}
