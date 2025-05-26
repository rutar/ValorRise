package com.valorrise.bot.model.mapper;

import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.dto.AdvertisementDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvertisementMapper {
    private static final Logger logger = LoggerFactory.getLogger(AdvertisementMapper.class);

    public static Advertisement toEntity(AdvertisementDto dto) {
        if (dto == null) {
            logger.warn("Attempted to map null AdvertisementDto");
            return null;
        }
        return Advertisement.builder()
                .adId(dto.getAdId())
                .message(dto.getMessage())
                .reward(dto.getReward())
                .expiresIn(dto.getExpiresIn())
                .probability(dto.getProbability())
                .build();
    }

    public static AdvertisementDto toDto(Advertisement entity) {
        if (entity == null) {
            logger.warn("Attempted to map null Advertisement");
            return null;
        }
        return AdvertisementDto.builder()
                .adId(entity.getAdId())
                .message(entity.getMessage())
                .reward(entity.getReward())
                .expiresIn(entity.getExpiresIn())
                .probability(entity.getProbability())
                .build();
    }
}