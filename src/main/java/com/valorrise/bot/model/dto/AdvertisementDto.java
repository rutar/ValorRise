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
public class AdvertisementDto {
    private String adId;
    private String message;
    private int reward;
    private int expiresIn;
    private boolean encrypted;
    private String probability;
}