package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private HttpServer httpServer;
    private MoviesStore moviesStore;

    public MoviesServer(MoviesStore ms, final int port) throws IOException {
        moviesStore = ms;
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/movies", new MoviesHandler(moviesStore));
    }

    public void start() {
        httpServer.start();

    }

    public void stop() {
        httpServer.stop(0);
    }

    public void addMovie(String title, int year) {
        moviesStore.addMovie(new Movie(title, year));
    }


}