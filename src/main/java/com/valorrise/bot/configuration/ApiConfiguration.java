package com.valorrise.bot.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "game")
@Validated
@Data
public class ApiConfiguration {
    @NotNull
    private Api api;

    @NotNull
    private Shop shop;

    @Data
    public static class Api {
        @NotNull
        private String baseUrl;

        @Min(1000)
        private int timeout;
    }

    @Data
    public static class Shop {
        @Min(1)
        private int minLivesToBuy;

        @Min(10)
        private int minGoldToBuy;
    }
}