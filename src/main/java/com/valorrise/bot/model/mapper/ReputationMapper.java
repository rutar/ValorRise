package com.valorrise.bot.model.mapper;

import com.valorrise.bot.model.domain.Reputation;
import com.valorrise.bot.model.dto.ReputationDto;
import org.springframework.stereotype.Component;

@Component
public class ReputationMapper {

    public static Reputation toEntity(ReputationDto reputationDto) {
        if (reputationDto == null) {
            return null;
        }
        return Reputation.builder()
                .people(reputationDto.getPeople())
                .state(reputationDto.getState())
                .underworld(reputationDto.getUnderworld())
                .build();
    }

    public static ReputationDto toDto(Reputation reputation) {
        if (reputation == null) {
            return null;
        }
        return ReputationDto.builder()
                .people(reputation.getPeople())
                .state(reputation.getState())
                .underworld(reputation.getUnderworld())
                .build();
    }
}