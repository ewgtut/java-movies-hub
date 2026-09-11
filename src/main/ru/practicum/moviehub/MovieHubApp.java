package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MovieHubApp {
    public static void main(String[] args) {
        try {
            var ms = new MoviesStore();
            final MoviesServer server = new MoviesServer(new MoviesStore(), 8080);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            server.start();
            System.out.println("HTTP-сервер запущен!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}