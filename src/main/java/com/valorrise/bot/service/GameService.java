package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.exception.GameApiException;
import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.domain.Game;
import com.valorrise.bot.model.domain.SolveResponse;
import com.valorrise.bot.model.dto.GameDto;
import com.valorrise.bot.model.dto.SolveResponseDto;
import com.valorrise.bot.model.mapper.GameMapper;
import com.valorrise.bot.model.mapper.SolveResponseMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final GameApiClient apiClient;
    private final GameApiService gameApiService;
    private final TaskSelectionService taskSelectionService;
    private final ShopService shopService;

    public GameService(GameApiClient apiClient, GameApiService gameApiService,
                       TaskSelectionService taskSelectionService, ShopService shopService) {
        this.apiClient = apiClient;
        this.gameApiService = gameApiService;
        this.taskSelectionService = taskSelectionService;
        this.shopService = shopService;
    }

    @Retry(name = "gameApi")
    public void playGame() {
        try {
            // Start a new game
            GameDto gameDto = apiClient.startGame();
            Game game = GameMapper.toEntity(gameDto);
            logger.info("Started game: {}, lives: {}, gold: {}", game.getGameId(), game.getLives(), game.getGold());

            // Game loop
            while (game.getLives() > 0) {
                try {
                    // Buy health potion if needed
                    game = shopService.buyHealthPotionIfNeeded(game);
                    if (game.getLives() <= 0) {
                        logger.info("Game over after buying potion: lives={}", game.getLives());
                        break;
                    }

                    // Fetch and select task
                    Advertisement bestAd = taskSelectionService.selectBestTask(
                            gameApiService.getAdvertisements(game.getGameId()));
                    if (bestAd == null) {
                        logger.warn("No valid advertisements for game: {}", game.getGameId());
                        break;
                    }
                    logger.debug("Selected task: {}, reward: {}, probability: {}",
                            bestAd.getAdId(), bestAd.getReward(), bestAd.getProbability());

                    // Solve task
                    SolveResponseDto responseDto = apiClient.solveAdvertisement(game.getGameId(), bestAd.getAdId());
                    SolveResponse response = SolveResponseMapper.toEntity(responseDto);
                    logger.info("Solved task {}, success: {}, lives: {}, gold: {}, score: {}",
                            bestAd.getAdId(), response.isSuccess(), response.getLives(), response.getGold(), response.getScore());

                    // Update game state
                    game.setLives(response.getLives());
                    game.setGold(response.getGold());
                    game.setScore(response.getScore());
                    game.setTurn(response.getTurn());

                    if (!response.isSuccess()) {
                        logger.warn("Task failed: {}", response.getMessage());
                    }
                } catch (GameApiException e) {
                    logger.error("API error for game {}: status={}, message={}",
                            game.getGameId(), e.getStatus(), e.getMessage());
                    if (e.getStatus() == 404) {
                        logger.info("Game {} not found, ending game", game.getGameId());
                        break;
                    }
                    // Continue loop for transient errors (handled by Resilience4j)
                }
            }

            logger.info("Game over for game: {}, final score: {}, high score: {}",
                    game.getGameId(), game.getScore(), game.getHighScore());
        } catch (GameApiException e) {
            logger.error("Failed to start game: status={}, message={}", e.getStatus(), e.getMessage());
        }
    }
}