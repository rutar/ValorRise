package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.configuration.ApiConfiguration;
import com.valorrise.bot.model.domain.Game;
import com.valorrise.bot.model.domain.Item;
import com.valorrise.bot.model.dto.GameDto;
import com.valorrise.bot.model.dto.ItemDto;
import com.valorrise.bot.model.mapper.GameMapper;
import com.valorrise.bot.model.mapper.ItemMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopService {
    private static final Logger logger = LoggerFactory.getLogger(ShopService.class);
    private final GameApiClient apiClient;
    private final int minLivesToBuy;
    private final int minGoldToBuy;

    public ShopService(GameApiClient apiClient, ApiConfiguration config) {
        this.apiClient = apiClient;
        this.minLivesToBuy = config.getShop().getMinLivesToBuy();
        this.minGoldToBuy = config.getShop().getMinGoldToBuy();
    }

    @Retry(name = "gameApi")
    public Game buyHealthPotionIfNeeded(Game game) {
        if (game == null) {
            logger.warn("Cannot buy potion for null game");
            return null;
        }

        if (game.getLives() >= minLivesToBuy || game.getGold() < minGoldToBuy) {
            logger.info("No need to buy potion for game {}: lives={}, gold={}",
                    game.getGameId(), game.getLives(), game.getGold());
            return game;
        }

        try {
            List<ItemDto> itemDtos = apiClient.getShopItems(game.getGameId());
            List<Item> items = itemDtos.stream()
                    .map(ItemMapper::toEntity)
                    .toList();

            Item healthPotion = items.stream()
                    .filter(item -> "hpot".equals(item.getId()))
                    .findFirst()
                    .orElse(null);

            if (healthPotion == null) {
                logger.warn("No health potion found in shop for game: {}", game.getGameId());
                return game;
            }

            if (game.getGold() >= healthPotion.getCost()) {
                logger.info("Buying health potion for game: {}, cost: {}", game.getGameId(), healthPotion.getCost());
                GameDto updatedGameDto = apiClient.buyItem(game.getGameId(), healthPotion.getId());
                updatedGameDto.setGameId(game.getGameId());
                return GameMapper.toEntity(updatedGameDto);
            } else {
                logger.info("Insufficient gold for health potion: gameId={}, gold={}, cost={}",
                        game.getGameId(), game.getGold(), healthPotion.getCost());
                return game;
            }
        } catch (Exception e) {
            logger.error("Error buying health potion for game {}: {}", game.getGameId(), e.getMessage());
            return game; // Return unchanged game state on error
        }
    }
}