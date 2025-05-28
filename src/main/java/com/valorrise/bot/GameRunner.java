package com.valorrise.bot;

import com.valorrise.bot.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GameRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(GameRunner.class);
    private final GameService gameService;

    @Value("${game.interactive:true}")
    private boolean interactive;

    public GameRunner(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void run(String... args) {
        logger.info("🚀 Dragons of Mugloar Game Bot Initialized! Interactive mode: {}", interactive);

        if (!interactive) {
            // Non-interactive mode (e.g., for tests): Run a single game
            logger.info("🎮 Starting a single game in non-interactive mode...");
            playGameWithErrorHandling();
            logger.info("🏁 Single game completed.");
            return;
        }

        // Interactive mode: Run games in a loop with user input
        while (true) {
            logger.info("🎮 Starting a new game...");
            playGameWithErrorHandling();

            if (handleUserInput()) {
                logger.info("👋 Exiting Dragons of Mugloar. Farewell, brave adventurer!");
                break;
            }
            logger.info("🔄 Continuing with a new game...");
        }
    }

    private void playGameWithErrorHandling() {
        try {
            gameService.playGame();
        } catch (Exception e) {
            logger.error("🚨 Error during game execution: {}", e.getMessage(), e);
        }
    }

    private boolean handleUserInput() {
        logger.info("🎲 Ready for a new challenge? ('y'|'n')");
        try {
            // Read a single character from System.in
            int input = System.in.read();
            // Clear any remaining newline characters from the input stream
            while (System.in.available() > 0) {
                System.in.read();
            }
            // Convert input to lowercase character
            char userInput = Character.toLowerCase((char) input);
            logger.info("Received input: {}", userInput);
            return userInput == 'n';
        } catch (IOException e) {
            logger.error("🚨 Error reading user input: {}", e.getMessage(), e);
            // Default to continuing the game on input error
            return false;
        }
    }
}