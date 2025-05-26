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
    @JsonProperty("adId")
    private String adId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("reward")
    private int reward;

    @JsonProperty("expiresIn")
    private int expiresIn;

    @JsonProperty("probability")
    private String probability;
}