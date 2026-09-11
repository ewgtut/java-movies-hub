package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;

public class ErrorObject {
    private String error;
    private List<String> details;

    public ErrorObject(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public ErrorObject() {
        details = new ArrayList<>();
    }

    public void setError(String error) {
        this.error = error;
    }

    public void addDetail(String detail) {
        this.details.add(detail);
    }
}
