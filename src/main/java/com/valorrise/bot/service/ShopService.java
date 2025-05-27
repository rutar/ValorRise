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

        return buyItem(game, "hpot");
    }

    @Retry(name = "gameApi")
    public Game buyItem(Game game, String itemId) {
        if (game == null) {
            logger.warn("Cannot buy item for null game");
            return null;
        }

        try {
            List<ItemDto> itemDtos = apiClient.getShopItems(game.getGameId());
            List<Item> items = itemDtos.stream()
                    .map(ItemMapper::toEntity)
                    .toList();

            Item targetItem = items.stream()
                    .filter(item -> itemId.equals(item.getId()))
                    .findFirst()
                    .orElse(null);

            if (targetItem == null) {
                logger.warn("Item {} not found in shop for game: {}", itemId, game.getGameId());
                return game;
            }

            if (game.getGold() >= targetItem.getCost() + minGoldToBuy) {
                logger.info("Buying item {} for game: {}, cost: {}", itemId, game.getGameId(), targetItem.getCost());
                GameDto updatedGameDto = apiClient.buyItem(game.getGameId(), targetItem.getId());
                updatedGameDto.setGameId(game.getGameId());
                return GameMapper.toEntity(updatedGameDto);
            } else {
                logger.info("Insufficient gold for item {}: gameId={}, gold={}, cost={}, minGoldToBuy={}",
                        itemId, game.getGameId(), game.getGold(), targetItem.getCost(), minGoldToBuy);
                return game;
            }
        } catch (Exception e) {
            logger.error("Error buying item {} for game {}: {}", itemId, game.getGameId(), e.getMessage());
            return game; // Return unchanged game state on error
        }
    }
}