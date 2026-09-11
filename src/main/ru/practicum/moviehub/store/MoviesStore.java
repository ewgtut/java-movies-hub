package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private int id = 1;
    private Map<Integer, Movie> movieMap = new HashMap<>();

    public void addMovie(Movie movie) {
        movieMap.put(id, movie);
        id++;
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movieMap.values());
    }

    public int getId() {
        return id;
    }

    public boolean containsId(int id) {
        return movieMap.containsKey(id);
    }

    public Movie getMovieById(int id) {
        return movieMap.get(id);
    }

    public void deleteMovieById(int id) {
        movieMap.remove(id);
    }

    public List<Movie> getMoviesByYear(int year) {
        return movieMap.values().stream().filter((m) -> m.getYear() == year).toList();
    }
}