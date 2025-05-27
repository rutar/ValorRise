package com.valorrise.bot.api.client;

import com.valorrise.bot.model.domain.Reputation;
import com.valorrise.bot.model.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "game-api", url = "${game.api.base-url}", configuration = FeignConfig.class)
public interface GameApiClient {
    @PostMapping("/game/start")
    GameDto startGame();

    @GetMapping("/{gameId}/messages")
    List<AdvertisementDto> getAdvertisements(@PathVariable("gameId") String gameId);

    @PostMapping("/{gameId}/solve/{adId}")
    SolveResponseDto solveAdvertisement(@PathVariable("gameId") String gameId, @PathVariable("adId") String adId);

    @GetMapping("/{gameId}/shop")
    List<ItemDto> getShopItems(@PathVariable("gameId") String gameId);

    @PostMapping("/{gameId}/investigate/reputation")
    ReputationDto getReputation(@PathVariable("gameId") String gameId);


    @PostMapping("/{gameId}/shop/buy/{itemId}")
    GameDto buyItem(@PathVariable("gameId") String gameId, @PathVariable("itemId") String itemId);

}