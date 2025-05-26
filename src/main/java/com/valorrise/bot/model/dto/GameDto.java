package com.valorrise.bot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {
    private String gameId;
    private int lives;
    private int gold;
    private int score;
    private int turn;
}