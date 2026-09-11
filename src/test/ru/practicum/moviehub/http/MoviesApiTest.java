package ru.practicum.moviehub.http;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class MoviesApiTest {

    private static final int PORT = 8080;
    private static final String urlString = "http://localhost:" + PORT + "/movies";
    private static final URI url = URI.create(urlString);
    private static MoviesServer server;
    private static GsonBuilder gsonBuilder;
    private static Gson gson;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    @BeforeEach
    void beforeEach() throws IOException {
        server = new MoviesServer(new MoviesStore(), PORT);
        //Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
        client = HttpClient.newBuilder()
//             .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void afterEach() {
        if (server != null) {
            server.stop();
        }

    }

    @AfterAll
    static void afterAll() {

    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode(), "Ожидается код возврата 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        Assertions.assertEquals(0, movieList.size(), "Ожидается пустой список фильмов");

    }

    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {


        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Accept", "application/json")
                .GET()
                .build();

        //добавляем два фильма, ожидаем увидеть два фильма
        server.addMovie("Movie1", 1999);
        server.addMovie("Movie2", 1998);

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, resp.statusCode(), "Ожидается код возврата 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        Assertions.assertEquals(2, movieList.size(), "Ожидается два фильма");
    }

    @Test
    void postMovie_whenSuccessfullyAdded_returnsMovieWithId() throws Exception {
        //название и год фильма для добавления
        String movieName = "Movie1";
        int movieYear = 1996;


        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(201, resp.statusCode(), "Ожидается код возврата 201 Created");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        Assertions.assertEquals(1, jsonObject.get("id").getAsInt(), "Ожидается фильм с ид 1");
        Assertions.assertEquals(movieName, jsonObject.get("title").getAsString(), "Ожидается фильм с названием" + movieName);
        Assertions.assertEquals(movieYear, jsonObject.get("year").getAsInt(), "Ожидается фильм с годом" + movieYear);
    }

    @Test
    void postMovie_whenMissingContentType_returnsErrorObject() throws Exception {
        String movieName = "Movie1";
        int movieYear = 1996;

        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(415, resp.statusCode(), "Ожидается код возврата 415");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        Assertions.assertTrue(jsonObject.get("error").getAsString().contains("Неподходящий хэдер Content-Type"), "Ожидается описание ошибки с отсутствием Content-Type");
    }

    @Test
    void postMovie_whenWrongMovieYear_returnsErrorObjects() throws Exception {
        //название и год фильма для добавления
        String movieName = "Movie1";
        int movieYear = 1796;


        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(422, resp.statusCode(), "Ожидается код возврата 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        Assertions.assertTrue(jsonObject.get("error").getAsString().contains("Неподходящие данные фильма"), "Ожидается описание ошибки с неправильными данными фильма");
        List<String> details = gson.fromJson(jsonObject.get("details").toString(), new ListOfStringsToken().getType());
        Assertions.assertTrue(details.get(1).contains(Integer.valueOf(movieYear).toString()));
    }

    @Test
    void getMovie_whenCorrectMovieAndId_returnsMovie() throws Exception {
        //название и год фильма для добавления
        String movieName = "Movie1";
        int movieYear = 1996;

        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        //добавляем фильм
        client.send(request, HttpResponse.BodyHandlers.ofString());

        //читаем фильм с id 1
        request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/1"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resp.statusCode(), "Ожидается код возврата 200");
        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals(movieName, jsonObject.get("title").getAsString(), "Ожидается название ранее добавленного фильма");
        Assertions.assertEquals(movieYear, jsonObject.get("year").getAsInt(), "Ожидается год ранее добавленного фильма");
    }

    @Test
    void getMovie_whenWrongMovieIdRequested_returnsErrorObject() throws Exception {
        //название и год фильма для добавления
        String movieName = "Movie1";
        int movieYear = 1996;

        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        //добавляем фильм
        client.send(request, HttpResponse.BodyHandlers.ofString());

        //читаем фильм с id 2
        request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/2"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, resp.statusCode(), "Ожидается код возврата 404");
        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals("Id не найден", jsonObject.get("error").getAsString(), "Ожидается сообщение об ошибке");

        List<String> details = gson.fromJson(jsonObject.get("details").toString(), new ListOfStringsToken().getType());
        Assertions.assertEquals("Фильм с ID 2 не найден", details.get(0), "Ожидается сообщение с деталями ошибки");
    }

    @Test
    void getMovie_whenWrongIdProvided_returnsErrorObject() throws Exception {
        //название и год фильма для добавления
        String movieName = "Movie1";
        int movieYear = 1996;

        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        //добавляем фильм
        client.send(request, HttpResponse.BodyHandlers.ofString());

        //читаем фильм с id 2
        request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/NotANumber"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, resp.statusCode(), "Ожидается код возврата 400");
        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals("Некорректный ID", jsonObject.get("error").getAsString(), "Ожидается сообщение об ошибке");

        List<String> details = gson.fromJson(jsonObject.get("details").toString(), new ListOfStringsToken().getType());
        Assertions.assertEquals("Некорректный ID NotANumber", details.get(0), "Ожидается сообщение с деталями ошибки");
    }

    @Test
    void deleteMovie_whenCorrectId_returnsCode() throws Exception {
        //название и год фильма для добавления
        String movieName = "Movie1";
        int movieYear = 1996;

        String jsonMovie = gson.toJson(new Movie(movieName, movieYear));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        //добавляем фильм
        client.send(request, HttpResponse.BodyHandlers.ofString());

        //удаляем фильм с id 1
        request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(201, resp.statusCode(), "Ожидается код возврата 201");
        //читаем фильм с id 1

        request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/1"))
                .header("Accept", "application/json")
                .GET()
                .build();

        resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, resp.statusCode(), "Ожидается код возврата 404");
        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals("Id не найден", jsonObject.get("error").getAsString(), "Ожидается сообщение об ошибке");

    }

    @Test
    void deleteMovie_whenNoMovieWithId_returnsCode() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, resp.statusCode(), "Ожидается код возврата 404");


        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals("Id не найден", jsonObject.get("error").getAsString(), "Ожидается сообщение об ошибке");
        List<String> details = gson.fromJson(jsonObject.get("details").toString(), new ListOfStringsToken().getType());
        Assertions.assertEquals("Фильм с ID 1 не найден", details.get(0), "Ожидается сообщение с деталями ошибки");
    }

    @Test
    void deleteMovie_whenIdNotNumber_returnsCode() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "/NAN"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, resp.statusCode(), "Ожидается код возврата 404");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals("Некорректный ID", jsonObject.get("error").getAsString(), "Ожидается сообщение об ошибке");
        List<String> details = gson.fromJson(jsonObject.get("details").toString(), new ListOfStringsToken().getType());
        Assertions.assertEquals("Некорректный ID NAN", details.get(0), "Ожидается сообщение с деталями ошибки");
    }

    @Test
    void getMovies_whenContainsMoviesByYear_returnsMoviesArray() throws Exception {
        //название и год фильмов для добавления
        String movieName1 = "Movie1";
        int movieYear1 = 1996;
        String movieName2 = "Movie2";


        String jsonMovie = gson.toJson(new Movie(movieName1, movieYear1));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        //добавляем фильм
        client.send(request, HttpResponse.BodyHandlers.ofString());

        jsonMovie = gson.toJson(new Movie(movieName2, movieYear1));

        request = HttpRequest.newBuilder()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMovie))
                .build();
        //добавляем фильм
        client.send(request, HttpResponse.BodyHandlers.ofString());
        //Запрашиваем фильмы

        request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "?year=" + movieYear1))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resp.statusCode(), "Ожидается код возврата 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        Assertions.assertEquals(2, movieList.size(), "Ожидаются два фильма");
        Assertions.assertTrue(movieList.contains(new Movie(movieName1, movieYear1)), "Ожидается первый фильм");
        Assertions.assertTrue(movieList.contains(new Movie(movieName2, movieYear1)), "Ожидается второй фильм");
    }

    @Test
    void getMovies_whenNoMoviesByYear_returnsMoviesArray() throws Exception {

        //Запрашиваем фильмы
        int movieYear = 1966;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "?year=" + movieYear))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resp.statusCode(), "Ожидается код возврата 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        Assertions.assertEquals(0, movieList.size(), "Ожидаются два фильма");
    }

    @Test
    void getMovies_whenYearParamIsNotANumber_returnsErrorObject() throws Exception {

        //Запрашиваем фильмы
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString + "?year=" + "NotANumber"))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, resp.statusCode(), "Ожидается код возврата 200");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        Assertions.assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        Assertions.assertTrue(body.startsWith("{") && body.endsWith("}"), "Ожидается JSON-объект");
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assertions.assertEquals("Некорректный год", jsonObject.get("error").getAsString(), "Ожидается сообщение об ошибке");
    }

}