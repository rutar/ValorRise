package com.valorrise.bot.model.mapper;

import com.valorrise.bot.model.domain.SolveResponse;
import com.valorrise.bot.model.dto.SolveResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolveResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(SolveResponseMapper.class);

    public static SolveResponse toEntity(SolveResponseDto dto) {
        if (dto == null) {
            logger.warn("Attempted to map null SolveResponseDto");
            return null;
        }
        return SolveResponse.builder()
                .success(dto.isSuccess())
                .lives(dto.getLives())
                .gold(dto.getGold())
                .score(dto.getScore())
                .highScore(dto.getHighScore())
                .turn(dto.getTurn())
                .message(dto.getMessage())
                .build();
    }

    public static SolveResponseDto toDto(SolveResponse entity) {
        if (entity == null) {
            logger.warn("Attempted to map null SolveResponse");
            return null;
        }
        return SolveResponseDto.builder()
                .success(entity.isSuccess())
                .lives(entity.getLives())
                .gold(entity.getGold())
                .score(entity.getScore())
                .highScore(entity.getHighScore())
                .turn(entity.getTurn())
                .message(entity.getMessage())
                .build();
    }
}