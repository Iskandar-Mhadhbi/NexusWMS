package com.nexuswms.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String message,
        List<String> errors,
        LocalDateTime timestamp
) {
    public ApiErrorResponse(int status, String message, LocalDateTime timestamp) {
        this(status, message, null, timestamp);
    }
}