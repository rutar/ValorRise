package com.valorrise.bot.model.mapper;


import com.valorrise.bot.model.domain.Game;
import com.valorrise.bot.model.dto.GameDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GameMapper {
    private static final Logger log = LoggerFactory.getLogger(GameMapper.class);

    public static Game toEntity(GameDto dto) {
        if (dto == null) {
            log.warn("Attempted to map null GameDto");
            return null;
        }
        return Game.builder()
                .gameId(dto.getGameId())
                .lives(dto.getLives())
                .gold(dto.getGold())
                .score(dto.getScore())
                .turn(dto.getTurn())
                .build();
    }

    public static GameDto toDto(Game entity) {
        if (entity == null) {
            log.warn("Attempted to map null Game");
            return null;
        }
        return GameDto.builder()
                .gameId(entity.getGameId())
                .lives(entity.getLives())
                .gold(entity.getGold())
                .score(entity.getScore())
                .turn(entity.getTurn())
                .build();
    }
}