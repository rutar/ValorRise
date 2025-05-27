package com.valorrise.bot.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String gameId;
    private int lives;
    private int gold;
    private int score;
    private int highScore;
    private int turn;
}