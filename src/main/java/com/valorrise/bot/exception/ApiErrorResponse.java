package com.valorrise.bot.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiErrorResponse {
    @JsonProperty("message")
    private String message;
}