package com.valorrise.bot.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement {
    private String adId;
    private String message;
    private int reward;
    private int expiresIn;
    private String probability;
}