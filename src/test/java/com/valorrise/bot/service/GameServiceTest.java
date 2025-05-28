package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.exception.GameApiException;
import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.domain.Game;
import com.valorrise.bot.model.domain.Reputation;
import com.valorrise.bot.model.domain.SolveResponse;
import com.valorrise.bot.model.dto.GameDto;
import com.valorrise.bot.model.dto.ReputationDto;
import com.valorrise.bot.model.dto.SolveResponseDto;
import com.valorrise.bot.model.mapper.ReputationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameApiClient apiClient;

    @Mock
    private GameApiService gameApiService;

    @Mock
    private TaskSelectionService taskSelectionService;

    @Mock
    private ShopService shopService;

    @InjectMocks
    private GameService gameService;

    private GameDto gameDto;
    private Game game;
    private Advertisement advertisement;
    private SolveResponseDto solveResponseDto;
    private SolveResponse solveResponse;
    private ReputationDto reputationDto;
    private Reputation reputation;

    @BeforeEach
    void setUp() {
        // Setup test data
        gameDto = new GameDto();
        gameDto.setGameId("test-game-123");
        gameDto.setLives(5);
        gameDto.setGold(200);
        gameDto.setScore(0);
        gameDto.setTurn(1);

        game = new Game();
        game.setGameId("test-game-123");
        game.setLives(5);
        game.setGold(200);
        game.setScore(0);
        game.setTurn(1);

        advertisement = new Advertisement();
        advertisement.setAdId("test-ad-123%3D");
        advertisement.setReward(50);

        solveResponseDto = new SolveResponseDto();
        solveResponseDto.setSuccess(true);
        solveResponseDto.setLives(4);
        solveResponseDto.setGold(250);
        solveResponseDto.setScore(50);
        solveResponseDto.setTurn(2);
        solveResponseDto.setMessage("Success");

        solveResponse = new SolveResponse();
        solveResponse.setSuccess(true);
        solveResponse.setLives(4);
        solveResponse.setGold(250);
        solveResponse.setScore(50);
        solveResponse.setTurn(2);
        solveResponse.setMessage("Success");

        reputationDto = new ReputationDto();
        reputationDto.setPeople(5.0f);
        reputationDto.setState(5.0f);
        reputationDto.setUnderworld(5.0f);

        reputation = new Reputation();
        reputation.setPeople(5.0f);
        reputation.setState(5.0f);
        reputation.setUnderworld(5.0f);
    }

    @Nested
    @DisplayName("playGame() method tests")
    class PlayGameTests {

        @Test
        @DisplayName("Should successfully complete a game with normal flow")
        void shouldSuccessfullyCompleteGame() throws GameApiException {
            // Given
            when(apiClient.startGame()).thenReturn(gameDto);
            when(shopService.buyItem(any(Game.class), anyString())).thenReturn(game);
            when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Collections.singletonList(advertisement));
            when(taskSelectionService.selectBestTask(anyList())).thenReturn(advertisement);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=")).thenReturn(solveResponseDto);
            when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);

            // Configure solve response to end game (set lives to 0)
            SolveResponseDto endGameResponse = new SolveResponseDto();
            endGameResponse.setSuccess(true);
            endGameResponse.setLives(0);
            endGameResponse.setGold(300);
            endGameResponse.setScore(100);
            endGameResponse.setTurn(3);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123="))
                    .thenReturn(solveResponseDto)
                    .thenReturn(endGameResponse);

            // When
            assertDoesNotThrow(() -> gameService.playGame());

            // Then
            verify(apiClient).startGame();
            verify(taskSelectionService, atLeastOnce()).selectBestTask(anyList());
            verify(apiClient, atLeastOnce()).solveAdvertisement(anyString(), anyString());
            verify(apiClient).getReputation("test-game-123");
        }

        @Test
        @DisplayName("Should handle GameApiException when starting game")
        void shouldHandleGameApiExceptionOnStart() throws GameApiException {
            // Given
            when(apiClient.startGame()).thenThrow(new GameApiException("Server Error", 500));

            // When & Then
            assertDoesNotThrow(() -> gameService.playGame());
            verify(apiClient).startGame();
            verifyNoMoreInteractions(taskSelectionService, shopService);
        }

        @Test
        @DisplayName("Should buy health potion when lives are low")
        void shouldBuyHealthPotionWhenLivesAreLow() throws GameApiException {
            // Given
            GameDto lowLivesGameDto = new GameDto();
            lowLivesGameDto.setGameId("test-game-123");
            lowLivesGameDto.setLives(2);
            lowLivesGameDto.setGold(100);
            lowLivesGameDto.setScore(0);
            lowLivesGameDto.setTurn(1);

            Game updatedGame = new Game();
            updatedGame.setGameId("test-game-123");
            updatedGame.setLives(3);
            updatedGame.setGold(50);
            updatedGame.setScore(0);
            updatedGame.setTurn(1);

            when(apiClient.startGame()).thenReturn(lowLivesGameDto);
            when(shopService.buyHealthPotionIfNeeded(any(Game.class))).thenReturn(updatedGame);
            when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Arrays.asList(advertisement));
            when(taskSelectionService.selectBestTask(anyList())).thenReturn(advertisement);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=")).thenReturn(solveResponseDto);
            when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);

            // Configure to end game after one iteration
            SolveResponseDto endGameResponse = new SolveResponseDto();
            endGameResponse.setLives(0);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=")).thenReturn(endGameResponse);

            // When
            assertDoesNotThrow(() -> gameService.playGame());

            // Then
            verify(shopService).buyHealthPotionIfNeeded(any(Game.class));
        }

        @Test
        @DisplayName("Should stop game when score exceeds 1000")
        void shouldStopGameWhenScoreExceeds1000() throws GameApiException {
            // Given
            when(apiClient.startGame()).thenReturn(gameDto);
            when(shopService.buyItem(any(Game.class), anyString())).thenReturn(game);
            when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Arrays.asList(advertisement));
            when(taskSelectionService.selectBestTask(anyList())).thenReturn(advertisement);
            when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);

            // Configure solve response with high score
            SolveResponseDto highScoreResponse = new SolveResponseDto();
            highScoreResponse.setSuccess(true);
            highScoreResponse.setLives(4);
            highScoreResponse.setGold(250);
            highScoreResponse.setScore(1500);
            highScoreResponse.setTurn(2);

            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=")).thenReturn(highScoreResponse);

            // When
            assertDoesNotThrow(() -> gameService.playGame());

            // Then
            verify(apiClient).solveAdvertisement("test-game-123", "test-ad-123=");
            verify(apiClient).getReputation("test-game-123");
        }

        @Test
        @DisplayName("Should handle 404 error and end game")
        void shouldHandle404ErrorAndEndGame() throws GameApiException {
            // Given
            try (MockedStatic<ReputationMapper> mockedStatic = mockStatic(ReputationMapper.class)) {
                when(apiClient.startGame()).thenReturn(gameDto);
                when(shopService.buyItem(any(Game.class), anyString())).thenReturn(game);
                when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);
                mockedStatic.when(() -> ReputationMapper.toEntity(any(ReputationDto.class))).thenReturn(reputation);
                when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Arrays.asList(advertisement));
                when(taskSelectionService.selectBestTask(anyList())).thenReturn(advertisement);
                when(apiClient.solveAdvertisement("test-game-123", "test-ad-123="))
                        .thenThrow(new GameApiException("Game not found", 404));

                // When
                assertDoesNotThrow(() -> gameService.playGame());

                // Then
                verify(apiClient).solveAdvertisement("test-game-123", "test-ad-123=");
                verify(apiClient).getReputation("test-game-123");
            }
        }

        @Test
        @DisplayName("Should handle no valid advertisements")
        void shouldHandleNoValidAdvertisements() throws GameApiException {
            // Given
            try (MockedStatic<ReputationMapper> mockedStatic = mockStatic(ReputationMapper.class)) {
                when(apiClient.startGame()).thenReturn(gameDto);
                when(shopService.buyItem(any(Game.class), anyString())).thenReturn(game);
                when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);
                mockedStatic.when(() -> ReputationMapper.toEntity(any(ReputationDto.class))).thenReturn(reputation);
                when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Collections.emptyList());
                when(taskSelectionService.selectBestTask(anyList())).thenReturn(null);

                // When
                assertDoesNotThrow(() -> gameService.playGame());

                // Then
                verify(taskSelectionService).selectBestTask(anyList());
                verify(apiClient, never()).solveAdvertisement(anyString(), anyString());
                verify(apiClient).getReputation("test-game-123");
            }
        }

        @Test
        @DisplayName("Should buy upgrade items when conditions are met")
        void shouldBuyUpgradeItemsWhenConditionsMet() throws GameApiException {
            // Given
            GameDto richGameDto = new GameDto();
            richGameDto.setGameId("test-game-123");
            richGameDto.setLives(5);
            richGameDto.setGold(500);
            richGameDto.setScore(0);
            richGameDto.setTurn(1);

            Game updatedGame = new Game();
            updatedGame.setGameId("test-game-123");
            updatedGame.setLives(5);
            updatedGame.setGold(200);
            updatedGame.setScore(0);
            updatedGame.setTurn(1);

            when(apiClient.startGame()).thenReturn(richGameDto);
            when(shopService.buyItem(any(Game.class), anyString())).thenReturn(updatedGame);
            when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Arrays.asList(advertisement));
            when(taskSelectionService.selectBestTask(anyList())).thenReturn(advertisement);
            when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);

            // Configure to end game after one iteration
            SolveResponseDto endGameResponse = new SolveResponseDto();
            endGameResponse.setLives(0);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=")).thenReturn(endGameResponse);

            // When
            assertDoesNotThrow(() -> gameService.playGame());

            // Then
            verify(shopService).buyItem(any(Game.class), anyString());
        }
    }

    @Nested
    @DisplayName("selectUpgradeItem() method tests")
    class SelectUpgradeItemTests {

        @Test
        @DisplayName("Should return null when insufficient gold")
        void shouldReturnNullWhenInsufficientGold() throws Exception {
            // Given
            Game poorGame = new Game();
            poorGame.setGold(100);
            poorGame.setTurn(5);
            boolean[] purchasedUpgrades = new boolean[10];
            Reputation goodReputation = new Reputation();
            goodReputation.setPeople(5.0f);
            goodReputation.setState(5.0f);
            goodReputation.setUnderworld(5.0f);

            // When
            String result = invokeSelectUpgradeItem(poorGame, purchasedUpgrades, 0, 5, goodReputation);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should prioritize state reputation items when state is lowest")
        void shouldPrioritizeStateReputationItems() throws Exception {
            // Given
            Game game = new Game();
            game.setGold(400);
            game.setTurn(15);
            boolean[] purchasedUpgrades = new boolean[10];
            Reputation lowStateReputation = new Reputation();
            lowStateReputation.setPeople(5.0f);
            lowStateReputation.setState(2.0f);
            lowStateReputation.setUnderworld(5.0f);

            // When
            String result = invokeSelectUpgradeItem(game, purchasedUpgrades, 0, 5, lowStateReputation);

            // Then
            assertEquals("tricks", result);
        }

        @Test
        @DisplayName("Should prioritize underworld reputation items when underworld is lowest")
        void shouldPrioritizeUnderworldReputationItems() throws Exception {
            // Given
            Game game = new Game();
            game.setGold(450);
            game.setTurn(15);
            boolean[] purchasedUpgrades = new boolean[10];
            Reputation lowUnderworldReputation = new Reputation();
            lowUnderworldReputation.setPeople(5.0f);
            lowUnderworldReputation.setState(5.0f);
            lowUnderworldReputation.setUnderworld(2.0f);

            // When
            String result = invokeSelectUpgradeItem(game, purchasedUpgrades, 0, 5, lowUnderworldReputation);

            // Then
            assertEquals("cs", result);
        }

        @Test
        @DisplayName("Should prioritize people reputation items when people is lowest")
        void shouldPrioritizePeopleReputationItems() throws Exception {
            // Given
            Game game = new Game();
            game.setGold(400);
            game.setTurn(15);
            boolean[] purchasedUpgrades = new boolean[10];
            Reputation lowPeopleReputation = new Reputation();
            lowPeopleReputation.setPeople(2.0f);
            lowPeopleReputation.setState(5.0f);
            lowPeopleReputation.setUnderworld(5.0f);

            // When
            String result = invokeSelectUpgradeItem(game, purchasedUpgrades, 0, 5, lowPeopleReputation);

            // Then
            assertEquals("wingpot", result);
        }

        @Test
        @DisplayName("Should prefer cheap items when early in game")
        void shouldPreferCheapItemsWhenEarlyInGame() throws Exception {
            // Given
            Game earlyGame = new Game();
            earlyGame.setGold(200);
            earlyGame.setTurn(5);
            boolean[] purchasedUpgrades = new boolean[10];
            Reputation goodReputation = new Reputation();
            goodReputation.setPeople(5.0f);
            goodReputation.setState(5.0f);
            goodReputation.setUnderworld(5.0f);

            // When
            String result = invokeSelectUpgradeItem(earlyGame, purchasedUpgrades, 0, 5, goodReputation);

            // Then
            assertNotNull(result);
            // Should be one of the first 5 items (100 gold items)
            assertTrue(Arrays.asList("cs", "gas", "wax", "tricks", "wingpot").contains(result));
        }

        @Test
        @DisplayName("Should return null when all upgrades are purchased")
        void shouldReturnNullWhenAllUpgradesPurchased() throws Exception {
            // Given
            Game game = new Game();
            game.setGold(500);
            game.setTurn(15);
            boolean[] purchasedUpgrades = new boolean[10];
            Arrays.fill(purchasedUpgrades, true); // All items purchased
            Reputation goodReputation = new Reputation();
            goodReputation.setPeople(5.0f);
            goodReputation.setState(5.0f);
            goodReputation.setUnderworld(5.0f);

            // When
            String result = invokeSelectUpgradeItem(game, purchasedUpgrades, 0, 5, goodReputation);

            // Then
            assertNull(result);
        }

        // Helper method to invoke private selectUpgradeItem method
        private String invokeSelectUpgradeItem(Game game, boolean[] purchasedUpgrades,
                                               int tasksFailed, int tasksCompleted,
                                               Reputation reputation) throws Exception {
            var method = GameService.class.getDeclaredMethod("selectUpgradeItem",
                    Game.class, boolean[].class, int.class, int.class, Reputation.class);
            method.setAccessible(true);
            return (String) method.invoke(gameService, game, purchasedUpgrades,
                    tasksFailed, tasksCompleted, reputation);
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle reputation fetch failure gracefully")
        void shouldHandleReputationFetchFailure() throws GameApiException {

            // Given
            when(apiClient.startGame()).thenReturn(gameDto);
            when(shopService.buyItem(any(Game.class), anyString())).thenReturn(game);
            when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Arrays.asList(advertisement));
            when(taskSelectionService.selectBestTask(anyList())).thenReturn(advertisement);
            when(apiClient.getReputation("test-game-123")).thenThrow(new GameApiException( "Server Error", 500));

            // Configure to end game after one iteration
            SolveResponseDto endGameResponse = new SolveResponseDto();
            endGameResponse.setLives(0);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=")).thenReturn(endGameResponse);

            // When & Then
            assertDoesNotThrow(() -> gameService.playGame());
            verify(apiClient).getReputation("test-game-123");
        }

        @Test
        @DisplayName("Should decode URL-encoded advertisement IDs")
        void shouldDecodeUrlEncodedAdvertisementIds() throws GameApiException {

            // Given
            Advertisement encodedAd = new Advertisement();
            encodedAd.setAdId("test-ad-123%3D%26special");
            encodedAd.setReward(50);

            when(apiClient.startGame()).thenReturn(gameDto);
            when(shopService.buyItem(any(Game.class), anyString())).thenReturn(game);
            when(gameApiService.getAdvertisements("test-game-123")).thenReturn(Arrays.asList(encodedAd));
            when(taskSelectionService.selectBestTask(anyList())).thenReturn(encodedAd);
            when(apiClient.getReputation("test-game-123")).thenReturn(reputationDto);

            // Configure to end game after one iteration
            SolveResponseDto endGameResponse = new SolveResponseDto();
            endGameResponse.setLives(0);
            when(apiClient.solveAdvertisement("test-game-123", "test-ad-123=&special")).thenReturn(endGameResponse);

            // When
            assertDoesNotThrow(() -> gameService.playGame());

            // Then - verify that the decoded ID was used
            verify(apiClient).solveAdvertisement("test-game-123", "test-ad-123=&special");
        }
    }
}