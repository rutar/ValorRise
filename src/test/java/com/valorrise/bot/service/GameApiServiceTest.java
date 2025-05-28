package com.valorrise.bot.service;

import com.valorrise.bot.api.client.GameApiClient;
import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.dto.AdvertisementDto;
import com.valorrise.bot.model.mapper.AdvertisementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameApiServiceTest {

    @Mock
    private GameApiClient gameApiClient;

    @InjectMocks
    private GameApiService gameApiService;

    private AdvertisementDto advertisementDto1;
    private AdvertisementDto advertisementDto2;
    private Advertisement advertisement1;
    private Advertisement advertisement2;

    @BeforeEach
    void setUp() {
        // Setup test data
        advertisementDto1 = new AdvertisementDto();
        advertisementDto1.setAdId("ad1");
        advertisementDto1.setMessage("Test Ad 1");

        advertisementDto2 = new AdvertisementDto();
        advertisementDto2.setAdId("ad2");
        advertisementDto2.setMessage("Test Ad 2");

        advertisement1 = new Advertisement();
        advertisement1.setAdId("ad1");
        advertisement1.setMessage("Test Ad 1");

        advertisement2 = new Advertisement();
        advertisement2.setAdId("ad2");
        advertisement2.setMessage("Test Ad 2");
    }

    @Test
    void getAdvertisements_shouldReturnMappedAdvertisements_whenClientReturnsData() {
        // Given
        String gameId = "game123";
        List<AdvertisementDto> dtos = Arrays.asList(advertisementDto1, advertisementDto2);

        when(gameApiClient.getAdvertisements(gameId)).thenReturn(dtos);

        try (MockedStatic<AdvertisementMapper> mapperMock = mockStatic(AdvertisementMapper.class)) {
            mapperMock.when(() -> AdvertisementMapper.toEntity(advertisementDto1))
                    .thenReturn(advertisement1);
            mapperMock.when(() -> AdvertisementMapper.toEntity(advertisementDto2))
                    .thenReturn(advertisement2);

            // When
            List<Advertisement> result = gameApiService.getAdvertisements(gameId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(advertisement1, advertisement2);

            verify(gameApiClient).getAdvertisements(gameId);
            mapperMock.verify(() -> AdvertisementMapper.toEntity(advertisementDto1));
            mapperMock.verify(() -> AdvertisementMapper.toEntity(advertisementDto2));
        }
    }

    @Test
    void getAdvertisements_shouldReturnEmptyList_whenClientReturnsEmptyList() {
        // Given
        String gameId = "game123";
        List<AdvertisementDto> emptyDtos = Collections.emptyList();

        when(gameApiClient.getAdvertisements(gameId)).thenReturn(emptyDtos);

        // When
        List<Advertisement> result = gameApiService.getAdvertisements(gameId);

        // Then
        assertThat(result).isEmpty();
        verify(gameApiClient).getAdvertisements(gameId);
    }

    @Test
    void getAdvertisements_shouldReturnEmptyList_whenClientReturnsNull() {
        // Given
        String gameId = "game123";

        when(gameApiClient.getAdvertisements(gameId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> gameApiService.getAdvertisements(gameId))
                .isInstanceOf(NullPointerException.class);

        verify(gameApiClient).getAdvertisements(gameId);
    }

    @Test
    void getAdvertisements_shouldPropagateException_whenClientThrowsException() {
        // Given
        String gameId = "game123";
        RuntimeException expectedException = new RuntimeException("API Error");

        when(gameApiClient.getAdvertisements(gameId)).thenThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> gameApiService.getAdvertisements(gameId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API Error");

        verify(gameApiClient).getAdvertisements(gameId);
    }

    @Test
    void getAdvertisements_shouldHandleSingleAdvertisement() {
        // Given
        String gameId = "game123";
        List<AdvertisementDto> singleDto = Collections.singletonList(advertisementDto1);

        when(gameApiClient.getAdvertisements(gameId)).thenReturn(singleDto);

        try (MockedStatic<AdvertisementMapper> mapperMock = mockStatic(AdvertisementMapper.class)) {
            mapperMock.when(() -> AdvertisementMapper.toEntity(advertisementDto1))
                    .thenReturn(advertisement1);

            // When
            List<Advertisement> result = gameApiService.getAdvertisements(gameId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(advertisement1);

            verify(gameApiClient).getAdvertisements(gameId);
            mapperMock.verify(() -> AdvertisementMapper.toEntity(advertisementDto1));
        }
    }

    @Test
    void getAdvertisements_shouldPropagateMapperException_whenMapperThrowsException() {
        // Given
        String gameId = "game123";
        List<AdvertisementDto> dtos = Collections.singletonList(advertisementDto1);
        RuntimeException mapperException = new RuntimeException("Mapping error");

        when(gameApiClient.getAdvertisements(gameId)).thenReturn(dtos);

        try (MockedStatic<AdvertisementMapper> mapperMock = mockStatic(AdvertisementMapper.class)) {
            mapperMock.when(() -> AdvertisementMapper.toEntity(any(AdvertisementDto.class)))
                    .thenThrow(mapperException);

            // When & Then
            assertThatThrownBy(() -> gameApiService.getAdvertisements(gameId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping error");

            verify(gameApiClient).getAdvertisements(gameId);
            mapperMock.verify(() -> AdvertisementMapper.toEntity(advertisementDto1));
        }
    }

    @Test
    void getAdvertisements_shouldPassCorrectGameId_toClient() {
        // Given
        String gameId = "specific-game-id-123";
        List<AdvertisementDto> dtos = Collections.emptyList();

        when(gameApiClient.getAdvertisements(gameId)).thenReturn(dtos);

        // When
        gameApiService.getAdvertisements(gameId);

        // Then
        verify(gameApiClient).getAdvertisements(eq(gameId));
    }

    @Test
    void constructor_shouldAcceptClient() {
        // Given
        GameApiClient mockClient = mock(GameApiClient.class);

        // When
        GameApiService service = new GameApiService(mockClient);

        // Then
        assertThat(service).isNotNull();
    }
}