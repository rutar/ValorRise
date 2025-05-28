package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.exception.GameApiException;
import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.domain.Game;
import com.valorrise.bot.model.domain.Reputation;
import com.valorrise.bot.model.domain.SolveResponse;
import com.valorrise.bot.model.dto.GameDto;
import com.valorrise.bot.model.dto.SolveResponseDto;
import com.valorrise.bot.model.mapper.GameMapper;
import com.valorrise.bot.model.mapper.ReputationMapper;
import com.valorrise.bot.model.mapper.SolveResponseMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final GameApiClient apiClient;
    private final GameApiService gameApiService;
    private final TaskSelectionService taskSelectionService;
    private final ShopService shopService;

    // List of upgrade items (excluding healing potion)
    private static final List<String> UPGRADE_ITEMS = Arrays.asList(
            "cs", "gas", "wax", "tricks", "wingpot", // 100 gold items
            "ch", "rf", "iron", "mtrix", "wingpotmax" // 300 gold items
    );

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
            assert game != null;
            logger.info("🎮 New Adventure Begins! Game ID: {}, Lives: ❤️ {}, Gold: 💰 {}",
                    game.getGameId(), game.getLives(), game.getGold());

            // Statistics tracking
            int tasksCompleted = 0;
            int tasksFailed = 0;
            int totalRewards = 0;
            int itemsPurchased = 0;
            Reputation finalReputation = new Reputation();

            // Track purchased items to avoid duplicates
            boolean[] purchasedUpgrades = new boolean[UPGRADE_ITEMS.size()];

            // Game loop
            while (game.getLives() > 0) {
                try {
                    // Update reputation every 5 turns
                    if (game.getTurn() % 5 == 0 && game.getTurn() > 0) {
                        try {
                            finalReputation = ReputationMapper.toEntity(apiClient.getReputation(game.getGameId()));
                            logger.info("📊 Reputation Check at Turn {} for Game {}: People: 😊 {}, State: 🏰 {}, Underworld: 👹 {}",
                                    game.getTurn(), game.getGameId(), finalReputation.getPeople(),
                                    finalReputation.getState(), finalReputation.getUnderworld());
                        } catch (GameApiException e) {
                            logger.warn("⚠️ Failed to fetch reputation for Game {} at Turn {}: {}",
                                    game.getGameId(), game.getTurn(), e.getMessage());
                        }
                    }

                    // Buy health potion if lives are low
                    if (game.getLives() <= 2 && game.getGold() >= 50) {
                        game = shopService.buyHealthPotionIfNeeded(game);
                        itemsPurchased++;
                        logger.info("🧪 Purchased Healing Potion! Lives: ❤️ {}, Gold: 💰 {}",
                                game.getLives(), game.getGold());
                    }
                    if (game.getLives() <= 0) {
                        logger.info("💀 Game Over after potion purchase: Lives: ❤️ {}", game.getLives());
                        break;
                    }

                    // Buy an upgrade if conditions are met
                    if (game.getLives() >= 3 && game.getGold() >= 150) {
                        String itemToBuy = selectUpgradeItem(game, purchasedUpgrades, tasksFailed, tasksCompleted, finalReputation);
                        if (itemToBuy != null) {
                            int goldBefore = game.getGold();
                            Game updatedGame = shopService.buyItem(game, itemToBuy);
                            if (updatedGame.getGold() < goldBefore) { // Verify purchase
                                itemsPurchased++;
                                int itemIndex = UPGRADE_ITEMS.indexOf(itemToBuy);
                                purchasedUpgrades[itemIndex] = true;
                                logger.info("🛡️ Upgraded with {}! Lives: ❤️ {}, Gold: 💰 {}",
                                        itemToBuy, updatedGame.getLives(), updatedGame.getGold());
                                game = updatedGame;
                            } else {
                                logger.info("🛑 Failed to buy upgrade {}: insufficient gold or error", itemToBuy);
                            }
                        }
                    }


                    // Fetch and select task
                    Advertisement bestAd = taskSelectionService.selectBestTask(
                            gameApiService.getAdvertisements(game.getGameId()));
                    if (bestAd == null) {
                        logger.warn("🚫 No valid tasks available for Game: {}", game.getGameId());
                        break;
                    }

                    // Decode adId to handle URL-encoded characters
                    String decodedAdId = URLDecoder.decode(bestAd.getAdId(), StandardCharsets.UTF_8);

                    // Log the chosen advertisement's name and ID
                    logger.info("📜 Selected Task: '{}' (ID: {})", bestAd.getMessage(), decodedAdId);

                    // Solve task
                    SolveResponseDto responseDto = apiClient.solveAdvertisement(game.getGameId(), decodedAdId);
                    SolveResponse response = SolveResponseMapper.toEntity(responseDto);
                    assert response != null;
                    logger.info("🎯 Task '{}' (ID: {}) Attempted! Success: {}, Lives: ❤️ {}, Gold: 💰 {}, Score: 🏆 {}",
                            bestAd.getMessage(), decodedAdId, response.isSuccess() ? "✅ Yes" : "❌ No",
                            response.getLives(), response.getGold(), response.getScore());

                    // Update game state
                    game.setLives(response.getLives());
                    game.setGold(response.getGold());
                    game.setScore(response.getScore());
                    game.setTurn(response.getTurn());

                    // Update statistics
                    if (response.isSuccess()) {
                        tasksCompleted++;
                        totalRewards += bestAd.getReward();
                    } else {
                        tasksFailed++;
                        logger.warn("😓 Task '{}' (ID: {}) Failed: {}",
                                bestAd.getMessage(), decodedAdId, response.getMessage());
                    }

                    // Check if score exceeds 1000
                    if (game.getScore() > 1000) {
                        logger.info("🏅 Victory! Score exceeded 1000 for Game: {}", game.getGameId());
                        break;
                    }
                } catch (GameApiException e) {
                    logger.error("🚨 API Error for Game {}: Status={}, Message={}",
                            game.getGameId(), e.getStatus(), e.getMessage());
                    if (e.getStatus() == 404) {
                        logger.info("💀 Game {} not found, ending adventure", game.getGameId());
                        break;
                    }
                    // Continue loop for transient errors (handled by Resilience4j)
                }
            }

            // Log final statistics in a tabulated format
            logger.info("🏁 Game Over for Game: {} | Final Score: 🏆 {}",
                    game.getGameId(), game.getScore());
            logger.info("""
                            📈 Final Statistics:
                              Turns Taken: {}
                              Tasks Completed: ✅ {}
                              Tasks Failed: ❌ {}
                              Total Rewards: 💰 {}
                              Gold Remaining: 💰 {}
                              Items Purchased: 🛍️ {}
                              Reputation: People 😊 {} | State 🏰 {} | Underworld 👹 {}""",
                    game.getTurn(), tasksCompleted, tasksFailed, totalRewards, game.getGold(), itemsPurchased,
                    finalReputation.getPeople(), finalReputation.getState(), finalReputation.getUnderworld());

        } catch (GameApiException e) {
            logger.error("🚫 Failed to start adventure: Status={}, Message={}",
                    e.getStatus(), e.getMessage());
        }
    }

    private String selectUpgradeItem(Game game, boolean[] purchasedUpgrades, int tasksFailed, int tasksCompleted, Reputation reputation) {
        // Prioritize cheaper items (100 gold) if early in game, tasks are failing, or any reputation is low
        boolean preferCheapItems = game.getTurn() < 10 || tasksFailed > tasksCompleted ||
                reputation.getPeople() < 0 || reputation.getState() < 0 || reputation.getUnderworld() < 0;
        int minGoldRequired = preferCheapItems ? 150 : 350; // Ensure enough gold for item + potion

        if (game.getGold() < minGoldRequired) {
            return null;
        }

        // Find the lowest reputation to prioritize upgrades
        float minReputation = Math.min(Math.min(reputation.getPeople(), reputation.getState()), reputation.getUnderworld());
        if (minReputation == reputation.getState() && reputation.getState() < 5) {
            if (!purchasedUpgrades[UPGRADE_ITEMS.indexOf("tricks")]) {
                return "tricks"; // Book of Tricks for low state reputation
            } else if (!purchasedUpgrades[UPGRADE_ITEMS.indexOf("mtrix")]) {
                return "mtrix"; // Book of Megatricks
            }
        } else if (minReputation == reputation.getUnderworld() && reputation.getUnderworld() < 5) {
            if (!purchasedUpgrades[UPGRADE_ITEMS.indexOf("cs")]) {
                return "cs"; // Claw Sharpening for low underworld reputation
            } else if (!purchasedUpgrades[UPGRADE_ITEMS.indexOf("ch")]) {
                return "ch"; // Claw Honing
            }
        } else if (minReputation == reputation.getPeople() && reputation.getPeople() < 5) {
            if (!purchasedUpgrades[UPGRADE_ITEMS.indexOf("wingpot")]) {
                return "wingpot"; // Potion of Stronger Wings for low people reputation
            } else if (!purchasedUpgrades[UPGRADE_ITEMS.indexOf("wingpotmax")]) {
                return "wingpotmax"; // Potion of Awesome Wings
            }
        }

        // Try cheaper items first if preferred
        if (preferCheapItems) {
            for (int i = 0; i < 5; i++) { // First 5 items are 100 gold
                if (!purchasedUpgrades[i]) {
                    return UPGRADE_ITEMS.get(i);
                }
            }
        }

        // Try expensive items if cheaper ones are purchased or not preferred
        for (int i = 5; i < UPGRADE_ITEMS.size(); i++) { // Last 5 items are 300 gold
            if (!purchasedUpgrades[i]) {
                return UPGRADE_ITEMS.get(i);
            }
        }

        // Return null if all upgrades are purchased
        return null;
    }
}