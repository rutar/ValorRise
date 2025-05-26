package com.valorrise.bot.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolveResponse {
    private boolean success;
    private int lives;
    private int gold;
    private int score;
    private int highScore;
    private int turn;
    private String message;
}