package com.valorrise.bot;

import com.valorrise.bot.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GameRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(GameRunner.class);
    private final GameService gameService;

    public GameRunner(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void run(String... args) {
        logger.info("Starting Dragons of Mugloar game bot");
        try {
            gameService.playGame();
            logger.info("Game execution completed");
        } catch (Exception e) {
            logger.error("Error during game execution: {}", e.getMessage(), e);
        }
    }
}