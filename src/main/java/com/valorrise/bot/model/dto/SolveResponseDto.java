package com.valorrise.bot.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolveResponseDto {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("lives")
    private int lives;

    @JsonProperty("gold")
    private int gold;

    @JsonProperty("score")
    private int score;

    @JsonProperty("highScore")
    private int highScore;

    @JsonProperty("turn")
    private int turn;

    @JsonProperty("message")
    private String message;
}