package com.valorrise.bot.model.mapper;

import com.valorrise.bot.model.domain.Item;
import com.valorrise.bot.model.dto.ItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemMapper {
    private static final Logger logger = LoggerFactory.getLogger(ItemMapper.class);

    public static Item toEntity(ItemDto dto) {
        if (dto == null) {
            logger.warn("Attempted to map null ItemDto");
            return null;
        }
        return Item.builder()
                .id(dto.getId())
                .name(dto.getName())
                .cost(dto.getCost())
                .build();
    }

    public static ItemDto toDto(Item entity) {
        if (entity == null) {
            logger.warn("Attempted to map null Item");
            return null;
        }
        return ItemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cost(entity.getCost())
                .build();
    }
}