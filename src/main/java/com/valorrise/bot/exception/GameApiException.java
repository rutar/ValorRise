package com.valorrise.bot.exception;

public class GameApiException extends RuntimeException {
    private final int status;

    public GameApiException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}