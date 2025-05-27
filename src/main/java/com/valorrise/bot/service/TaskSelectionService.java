package com.valorrise.bot.service;

import com.valorrise.bot.model.domain.Advertisement;
import com.valorrise.bot.model.domain.Weather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TaskSelectionService {
    private static final Logger logger = LoggerFactory.getLogger(TaskSelectionService.class);

    public Advertisement selectBestTask(List<Advertisement> ads, Weather weather) {
        if (ads == null || ads.isEmpty()) {
            logger.warn("No advertisements available");
            return null;
        }
        return ads.stream()
                .filter(ad -> !isTrap(ad) && ad.getExpiresIn() > 0)
                .filter(ad -> isWeatherCompatible(ad, weather))
                .max(Comparator.comparingDouble(this::calculateScore))
                .orElse(null);
    }

    private boolean isTrap(Advertisement ad) {
        return ad.getProbability().equalsIgnoreCase("Suicide") || ad.getReward() < 10;
    }

    private boolean isWeatherCompatible(Advertisement ad, Weather weather) {
        if (weather == null) {
            return true;
        }
        // Example: Avoid risky tasks in storm or heavy rain
        String weatherCode = weather.getCode();
        return switch (weatherCode) {
            case "SRO", "T E" -> !ad.getProbability().equalsIgnoreCase("Risky") &&
                    !ad.getProbability().equalsIgnoreCase("Suicide");
            default -> true; // NMR (Normal), etc.
        };
    }

    private double calculateScore(Advertisement ad) {
        double probability = parseProbability(ad.getProbability());
        return probability * ad.getReward() / (ad.getExpiresIn() + 1);
    }

    private double parseProbability(String probability) {
        return switch (probability) {
            case "Piece of Cake" -> 1.0;
            case "Sure thing" -> 0.9;
            case "Walk in the park" -> 0.8;
            case "Risky" -> 0.5;
            default -> 0.1;
        };
    }
}