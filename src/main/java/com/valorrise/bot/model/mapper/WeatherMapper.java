package com.valorrise.bot.model.mapper;

import com.valorrise.bot.model.domain.Weather;
import com.valorrise.bot.model.dto.WeatherDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherMapper {
    private static final Logger logger = LoggerFactory.getLogger(WeatherMapper.class);

    public static Weather toEntity(WeatherDto dto) {
        if (dto == null) {
            logger.warn("Attempted to map null WeatherDto");
            return null;
        }
        return Weather.builder()
                .code(dto.getCode())
                .description(dto.getDescription())
                .build();
    }

    public static WeatherDto toDto(Weather entity) {
        if (entity == null) {
            logger.warn("Attempted to map null Weather");
            return null;
        }
        return WeatherDto.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .build();
    }
}