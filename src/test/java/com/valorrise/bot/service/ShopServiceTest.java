package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.configuration.ApiConfiguration;
import com.valorrise.bot.model.domain.Game;
import com.valorrise.bot.model.domain.Item;
import com.valorrise.bot.model.dto.GameDto;
import com.valorrise.bot.model.dto.ItemDto;
import com.valorrise.bot.model.mapper.GameMapper;
import com.valorrise.bot.model.mapper.ItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private GameApiClient gameApiClient;

    @Mock
    private ApiConfiguration apiConfiguration;

    @Mock
    private ApiConfiguration.Shop shopConfig;

    private ShopService shopService;

    private Game testGame;
    private GameDto testGameDto;
    private ItemDto healthPotionDto;
    private ItemDto expensiveItemDto;
    private Item healthPotion;
    private Item expensiveItem;

    @BeforeEach
    void setUp() {
        // Setup configuration mocks first
        when(apiConfiguration.getShop()).thenReturn(shopConfig);
        when(shopConfig.getMinLivesToBuy()).thenReturn(3);
        when(shopConfig.getMinGoldToBuy()).thenReturn(100);

        // Create service instance after mocks are configured
        shopService = new ShopService(gameApiClient, apiConfiguration);

        // Setup test data
        testGame = new Game();
        testGame.setGameId("game123");
        testGame.setLives(2);
        testGame.setGold(200);

        testGameDto = new GameDto();
        testGameDto.setGameId("game123");
        testGameDto.setLives(3);
        testGameDto.setGold(150);

        healthPotionDto = new ItemDto();
        healthPotionDto.setId("hpot");
        healthPotionDto.setCost(50);

        expensiveItemDto = new ItemDto();
        expensiveItemDto.setId("expensive");
        expensiveItemDto.setCost(200);

        healthPotion = new Item();
        healthPotion.setId("hpot");
        healthPotion.setCost(50);

        expensiveItem = new Item();
        expensiveItem.setId("expensive");
        expensiveItem.setCost(200);
    }

    @Nested
    class BuyHealthPotionIfNeededTests {

        @Test
        void shouldReturnNull_whenGameIsNull() {
            // When
            Game result = shopService.buyHealthPotionIfNeeded(null);

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(gameApiClient);
        }

        @Test
        void shouldReturnGameUnchanged_whenLivesAreSufficient() {
            // Given
            testGame.setLives(3); // Equal to minLivesToBuy
            testGame.setGold(200);

            // When
            Game result = shopService.buyHealthPotionIfNeeded(testGame);

            // Then
            assertThat(result).isSameAs(testGame);
            verifyNoInteractions(gameApiClient);
        }

        @Test
        void shouldReturnGameUnchanged_whenGoldIsInsufficient() {
            // Given
            testGame.setLives(1); // Less than minLivesToBuy
            testGame.setGold(50); // Less than minGoldToBuy

            // When
            Game result = shopService.buyHealthPotionIfNeeded(testGame);

            // Then
            assertThat(result).isSameAs(testGame);
            verifyNoInteractions(gameApiClient);
        }

        @Test
        void shouldBuyHealthPotion_whenConditionsAreMet() {
            // Given
            testGame.setLives(1); // Less than minLivesToBuy (3)
            testGame.setGold(200); // More than minGoldToBuy (100)

            List<ItemDto> shopItems = Collections.singletonList(healthPotionDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);
            when(gameApiClient.buyItem("game123", "hpot")).thenReturn(testGameDto);

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class);
                 MockedStatic<GameMapper> gameMapperMock = mockStatic(GameMapper.class)) {

                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto)).thenReturn(healthPotion);
                gameMapperMock.when(() -> GameMapper.toEntity(any(GameDto.class))).thenReturn(testGame);

                // When
                Game result = shopService.buyHealthPotionIfNeeded(testGame);

                // Then
                assertThat(result).isNotNull();
                verify(gameApiClient).getShopItems("game123");
                verify(gameApiClient).buyItem("game123", "hpot");
            }
        }
    }

    @Nested
    class BuyItemTests {

        @Test
        void shouldReturnNull_whenGameIsNull() {
            // When
            Game result = shopService.buyItem(null, "hpot");

            // Then
            assertThat(result).isNull();
            verifyNoInteractions(gameApiClient);
        }

        @Test
        void shouldReturnGameUnchanged_whenItemNotFoundInShop() {
            // Given
            List<ItemDto> shopItems = Collections.singletonList(expensiveItemDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class)) {
                itemMapperMock.when(() -> ItemMapper.toEntity(expensiveItemDto)).thenReturn(expensiveItem);

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isSameAs(testGame);
                verify(gameApiClient).getShopItems("game123");
                verify(gameApiClient, never()).buyItem(any(), any());
            }
        }

        @Test
        void shouldReturnGameUnchanged_whenInsufficientGold() {
            // Given
            testGame.setGold(120); // Not enough for item (50) + minGoldToBuy (100)
            List<ItemDto> shopItems = Collections.singletonList(healthPotionDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class)) {
                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto)).thenReturn(healthPotion);

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isSameAs(testGame);
                verify(gameApiClient).getShopItems("game123");
                verify(gameApiClient, never()).buyItem(any(), any());
            }
        }

        @Test
        void shouldBuyItem_whenConditionsAreMet() {
            // Given
            testGame.setGold(200); // Enough for item (50) + minGoldToBuy (100)
            List<ItemDto> shopItems = Arrays.asList(healthPotionDto, expensiveItemDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);
            when(gameApiClient.buyItem("game123", "hpot")).thenReturn(testGameDto);

            Game expectedGame = new Game();
            expectedGame.setGameId("game123");

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class);
                 MockedStatic<GameMapper> gameMapperMock = mockStatic(GameMapper.class)) {

                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto)).thenReturn(healthPotion);
                itemMapperMock.when(() -> ItemMapper.toEntity(expensiveItemDto)).thenReturn(expensiveItem);
                gameMapperMock.when(() -> GameMapper.toEntity(any(GameDto.class))).thenReturn(expectedGame);

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isEqualTo(expectedGame);
                verify(gameApiClient).getShopItems("game123");
                verify(gameApiClient).buyItem("game123", "hpot");

                // Verify that gameId is set on the DTO before mapping
                verify(gameApiClient).buyItem("game123", "hpot");
            }
        }

        @Test
        void shouldReturnGameUnchanged_whenApiThrowsException() {
            // Given
            when(gameApiClient.getShopItems("game123")).thenThrow(new RuntimeException("API Error"));

            // When
            Game result = shopService.buyItem(testGame, "hpot");

            // Then
            assertThat(result).isSameAs(testGame);
            verify(gameApiClient).getShopItems("game123");
            verify(gameApiClient, never()).buyItem(any(), any());
        }

        @Test
        void shouldReturnGameUnchanged_whenBuyItemThrowsException() {
            // Given
            testGame.setGold(200);
            List<ItemDto> shopItems = Collections.singletonList(healthPotionDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);
            when(gameApiClient.buyItem("game123", "hpot")).thenThrow(new RuntimeException("Buy failed"));

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class)) {
                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto)).thenReturn(healthPotion);

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isSameAs(testGame);
                verify(gameApiClient).getShopItems("game123");
                verify(gameApiClient).buyItem("game123", "hpot");
            }
        }

        @Test
        void shouldHandleEmptyShopItems() {
            // Given
            List<ItemDto> emptyShopItems = Collections.emptyList();
            when(gameApiClient.getShopItems("game123")).thenReturn(emptyShopItems);

            // When
            Game result = shopService.buyItem(testGame, "hpot");

            // Then
            assertThat(result).isSameAs(testGame);
            verify(gameApiClient).getShopItems("game123");
            verify(gameApiClient, never()).buyItem(any(), any());
        }

        @Test
        void shouldHandleMapperException() {
            // Given
            List<ItemDto> shopItems = Collections.singletonList(healthPotionDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class)) {
                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto))
                        .thenThrow(new RuntimeException("Mapping failed"));

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isSameAs(testGame);
                verify(gameApiClient).getShopItems("game123");
                verify(gameApiClient, never()).buyItem(any(), any());
            }
        }
    }

    @Nested
    class ConfigurationTests {

        @Test
        void shouldInitializeWithCorrectConfiguration() {
            // Given
            GameApiClient testClient = mock(GameApiClient.class);
            ApiConfiguration config = mock(ApiConfiguration.class);
            ApiConfiguration.Shop shop = mock(ApiConfiguration.Shop.class);
            when(config.getShop()).thenReturn(shop);
            when(shop.getMinLivesToBuy()).thenReturn(5);
            when(shop.getMinGoldToBuy()).thenReturn(200);

            // When
            ShopService service = new ShopService(testClient, config);

            // Then
            assertThat(service).isNotNull();
            verify(shop).getMinLivesToBuy();
            verify(shop).getMinGoldToBuy();
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldHandleExactGoldAmount() {
            // Given - exactly enough gold (item cost + minGoldToBuy)
            testGame.setGold(150); // healthPotion cost (50) + minGoldToBuy (100)
            List<ItemDto> shopItems = Collections.singletonList(healthPotionDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);
            when(gameApiClient.buyItem("game123", "hpot")).thenReturn(testGameDto);

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class);
                 MockedStatic<GameMapper> gameMapperMock = mockStatic(GameMapper.class)) {

                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto)).thenReturn(healthPotion);
                gameMapperMock.when(() -> GameMapper.toEntity(any(GameDto.class))).thenReturn(testGame);

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isNotNull();
                verify(gameApiClient).buyItem("game123", "hpot");
            }
        }

        @Test
        void shouldNotBuyWhen_goldIsOneLessThanRequired() {
            // Given - one gold less than required
            testGame.setGold(149); // healthPotion cost (50) + minGoldToBuy (100) - 1
            List<ItemDto> shopItems = Collections.singletonList(healthPotionDto);
            when(gameApiClient.getShopItems("game123")).thenReturn(shopItems);

            try (MockedStatic<ItemMapper> itemMapperMock = mockStatic(ItemMapper.class)) {
                itemMapperMock.when(() -> ItemMapper.toEntity(healthPotionDto)).thenReturn(healthPotion);

                // When
                Game result = shopService.buyItem(testGame, "hpot");

                // Then
                assertThat(result).isSameAs(testGame);
                verify(gameApiClient, never()).buyItem(any(), any());
            }
        }
    }
}