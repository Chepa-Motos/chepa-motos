package com.chepamotos.adapter.dto;

import java.time.LocalDateTime;

public class ApiErrorResponse {

    private String code;
    private String message;
    private String timestamp;

    public static ApiErrorResponse of(String code, String message) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.code = code;
        response.message = message;
        response.timestamp = LocalDateTime.now().toString();
        return response;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
