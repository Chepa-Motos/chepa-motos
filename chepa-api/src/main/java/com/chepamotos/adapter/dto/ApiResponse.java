package com.chepamotos.adapter.dto;

import java.time.Clock;
import java.time.LocalDateTime;

public class ApiResponse<T> {

    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> of(T data, Clock clock) {
        ApiResponse<T> response = new ApiResponse<>();
        response.data = data;
        response.timestamp = LocalDateTime.now(clock).toString();
        return response;
    }

    public T getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
