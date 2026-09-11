package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorObject;
import ru.practicum.moviehub.api.MovieWithId;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class MoviesHandler implements HttpHandler {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private MoviesStore moviesStore;

    private static GsonBuilder gsonBuilder = new GsonBuilder();
    Gson gson = gsonBuilder.create();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Endpoint endpoint = getEndpoint(exchange.getRequestURI().getPath() + "?" + exchange.getRequestURI().getRawQuery(), exchange.getRequestMethod());
        switch (endpoint) {
            case GET_MOVIES:
                handleGetMovies(exchange);
                break;
            case POST_MOVIE:
                handlePostMovie(exchange);
                break;
            case GET_MOVIE:
                handleGetMovieById(exchange);
            case DELETE_MOVIE:
                handleDeleteMovie(exchange);
                break;
            case GET_MOVIES_FILTERED:
                handleGetMoviesFiltered(exchange);
                break;
            default:
                break;
        }
    }

    private void writeResponse(HttpExchange exchange,
                               String responseString,
                               int responseCode) throws IOException {

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(responseCode, 0);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseString.getBytes(DEFAULT_CHARSET));
        }
        exchange.close();
    }

    private Endpoint getEndpoint(String path, String method) {
        String[] pathParts = path.split("/");
        switch (method) {
            case "GET":
                if (pathParts.length == 2) {
                    if (pathParts[1].contains("?year=")) {
                        return Endpoint.GET_MOVIES_FILTERED;
                    } else {
                        return Endpoint.GET_MOVIES;
                    }
                } else if (pathParts.length == 3) {
                    return Endpoint.GET_MOVIE;
                }
            case "POST":
                return Endpoint.POST_MOVIE;
            case "DELETE":
                if (pathParts.length == 3) {
                    return Endpoint.DELETE_MOVIE;
                }
        }
        return Endpoint.UNKNOWN;
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        String response = gson.toJson(moviesStore.getMovies());
        writeResponse(exchange, response, 200);
    }

    private void handleGetMoviesFiltered(HttpExchange exchange) throws IOException {
        String response = "";
        int code = 500;
        ErrorObject errorObject = new ErrorObject();
        String yearString = "";
        try {
            yearString = exchange.getRequestURI().getRawQuery().split("year=")[1];
            int yearInt = Integer.parseInt(yearString);
            code = 200;
            response = gson.toJson(moviesStore.getMoviesByYear(yearInt));
        } catch (NumberFormatException e) {
            code = 400;
            errorObject.setError("Некорректный год");
            errorObject.addDetail(String.format("Некорректный год %s", yearString));
            response = gson.toJson(errorObject);
        } catch (ArrayIndexOutOfBoundsException e) {
            code = 400;
            errorObject.setError("Некорректный год");
            errorObject.addDetail(String.format("Некорректный год %s", yearString));
            response = gson.toJson(errorObject);
        } catch (Exception e) {
            code = 400;
            response = gson.toJson(errorObject);
        }
        writeResponse(exchange, response, code);
    }

    private void handleGetMovieById(HttpExchange exchange) throws IOException {
        String response = "";
        int code = 500;
        ErrorObject errorObject = new ErrorObject();
        int id;
        var idString = exchange.getRequestURI().getPath().split("/")[2];
        try {
            id = Integer.parseInt(idString);
            if (!moviesStore.containsId(id)) {
                code = 404;
                errorObject.setError("Id не найден");
                errorObject.addDetail(String.format("Фильм с ID %s не найден", idString));
                throw new java.lang.IllegalArgumentException(String.format("Id %s не найден", idString));
            }
            code = 200;
            response = gson.toJson(moviesStore.getMovieById(id));
        } catch (NumberFormatException e) {
            code = 400;
            errorObject.setError("Некорректный ID");
            errorObject.addDetail(String.format("Некорректный ID %s", idString));
            response = gson.toJson(errorObject);
        } catch (Exception e) {
            response = gson.toJson(errorObject);
        }
        writeResponse(exchange, response, code);
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        String response = "";
        int code = 500;
        ErrorObject errorObject = new ErrorObject();
        int id;
        var idString = exchange.getRequestURI().getPath().split("/")[2];
        try {
            id = Integer.parseInt(idString);
            if (!moviesStore.containsId(id)) {
                code = 404;
                errorObject.setError("Id не найден");
                errorObject.addDetail(String.format("Фильм с ID %s не найден", idString));
                throw new java.lang.IllegalArgumentException(String.format("Id %s не найден", idString));
            }
            code = 201;
            moviesStore.deleteMovieById(id);
        } catch (NumberFormatException e) {
            code = 404;
            errorObject.setError("Некорректный ID");
            errorObject.addDetail(String.format("Некорректный ID %s", idString));
            response = gson.toJson(errorObject);
        } catch (Exception e) {
            response = gson.toJson(errorObject);
        }
        writeResponse(exchange, response, code);
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        String response;
        int code = 500;

        ErrorObject errorObject = new ErrorObject();
        try {
            if (!exchange.getRequestHeaders().containsKey("Content-Type") || !exchange.getRequestHeaders().get("Content-Type").contains("application/json")) {
                code = 415;
                errorObject.setError("Неподходящий хэдер Content-Type, все хэдеры запроса добавлены в описание");
                exchange.getRequestHeaders().entrySet().stream().forEach((entry) -> errorObject.addDetail(entry.toString()));
                throw new Exception("Неподходящий хэдер Content-Type");
            }
            JsonElement jsonElement = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), DEFAULT_CHARSET));
            JsonObject movieJson = jsonElement.getAsJsonObject();

            String title = movieJson.get("title").getAsString();
            int year = movieJson.get("year").getAsInt();
            if (title.length() > 100 || title.isBlank() || year < 1888 || year > 2026) {
                code = 422;
                errorObject.setError("Неподходящие данные фильма, название не должно быть пустым, год должен быть между 1888 и 2026");
                errorObject.addDetail(String.format("Название фильма %s", title));
                errorObject.addDetail(String.format("Год фильма %s", year));
                throw new java.lang.IllegalArgumentException(String.format("Неподходящие данные фильма для добавления"));
            }
            var movie = new Movie(title, year);
            moviesStore.addMovie(movie);
            response = gson.toJson(new MovieWithId(moviesStore.getId() - 1, movie));
            code = 201;
        } catch (Exception e) {
            response = gson.toJson(errorObject);
        }
        writeResponse(exchange, response, code);
    }
}

enum Endpoint {GET_MOVIES, GET_MOVIES_FILTERED, DELETE_MOVIE, GET_MOVIE, POST_MOVIE, UNKNOWN}