package com.valorrise.bot.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valorrise.bot.exception.ApiErrorResponse;
import com.valorrise.bot.exception.GameApiException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class GameApiErrorDecoder implements ErrorDecoder {
    private static final Logger logger = LoggerFactory.getLogger(GameApiErrorDecoder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = "Unknown error occurred";
        ApiErrorResponse apiError;

        // Attempt to parse error response body
        try (InputStream bodyIs = response.body().asInputStream()) {
            apiError = objectMapper.readValue(bodyIs, ApiErrorResponse.class);
            errorMessage = apiError.getMessage() != null ? apiError.getMessage() : errorMessage;
        } catch (IOException e) {
            logger.warn("Failed to parse error response body for method: {}, status: {}", methodKey, response.status());
        }

        // Log the error
        logger.error("API error for method: {}, status: {}, message: {}", methodKey, response.status(), errorMessage);

        // Map HTTP status codes to custom exceptions
        return switch (response.status()) {
            case 400 -> new GameApiException("Bad request: " + errorMessage, response.status());
            case 404 -> new GameApiException("Resource not found: " + errorMessage, response.status());
            case 429 -> new GameApiException("Rate limit exceeded: " + errorMessage, response.status());
            case 500, 502, 503, 504 -> new GameApiException("Server error: " + errorMessage, response.status());
            default -> new GameApiException("Unexpected error: " + errorMessage, response.status());
        };
    }
}