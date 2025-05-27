package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.dto.AdvertisementDto;
import com.valorrise.bot.model.mapper.AdvertisementMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameApiService {
    private final GameApiClient client;

    public GameApiService(GameApiClient client) {
        this.client = client;
    }

    @Retry(name = "gameApi")
    public List<Advertisement> getAdvertisements(String gameId) {
        List<AdvertisementDto> dtos = client.getAdvertisements(gameId);
        return dtos.stream()
                .map(AdvertisementMapper::toEntity)
                .collect(Collectors.toList());
    }
}