package com.valorrise.bot.service;

import com.valorrise.bot.model.domain.Advertisement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TaskSelectionService {
    private static final Logger logger = LoggerFactory.getLogger(TaskSelectionService.class);
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    public Advertisement selectBestTask(List<Advertisement> ads) {
        if (ads == null || ads.isEmpty()) {
            logger.warn("No advertisements available");
            return null;
        }

        return ads.stream()
                .map(this::decodeAdvertisement)
                .filter(ad -> !isTrap(ad) && ad.getExpiresIn() > 0)
                .max(Comparator.comparingDouble(this::calculateScore))
                .orElse(null);
    }

    private Advertisement decodeAdvertisement(Advertisement ad) {
        if (!ad.isEncrypted()) {
            return ad;
        }

        try {
            String decodedAdId = decodeField(ad.getAdId());
            String decodedMessage = decodeField(ad.getMessage());
            String decodedProbability = decodeField(ad.getProbability());

            return Advertisement.builder()
                    .adId(decodedAdId)
                    .message(decodedMessage)
                    .reward(ad.getReward())
                    .expiresIn(ad.getExpiresIn())
                    .encrypted(ad.isEncrypted())
                    .probability(decodedProbability)
                    .build();
        } catch (Exception e) {
            logger.error("Failed to decode fields for adId: {}, message: {}, probability: {}. Error: {}",
                    ad.getAdId(), ad.getMessage(), ad.getProbability(), e.getMessage());
            return ad; // Fallback to original
        }
    }

    private String decodeField(String input) {
        // Try Base64 first if it matches pattern
        if (BASE64_PATTERN.matcher(input).matches()) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(input);
                String decoded = new String(decodedBytes);
                // Check if result is readable (contains letters/spaces, not gibberish)
                if (isReadable(decoded)) {
                    logger.debug("Base64 decoded: {} -> {}", input, decoded);
                    return decoded;
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Not Base64: {}. Error: {}", input, e.getMessage());
            }
        }

        // Try ROT13
        String rot13Decoded = rot13(input);
        if (!rot13Decoded.equals(input) && isReadable(rot13Decoded)) {
            logger.debug("ROT13 decoded: {} -> {}", input, rot13Decoded);
            return rot13Decoded;
        }

        // Fallback to original
        logger.debug("No decoding applied for: {}", input);
        return input;
    }

    private String rot13(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                result.append((char) (base + ((c - base + 13) % 26)));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private boolean isReadable(String text) {
        // Basic readability: contains letters/spaces, not just random chars
        return text.matches(".*[a-zA-Z\\s].*") && !text.contains("\uFFFD");
    }

    private boolean isTrap(Advertisement ad) {
        String probability = ad.getProbability().toLowerCase();
        return probability.contains("suicide") || ad.getReward() < 10;
    }

    private double calculateScore(Advertisement ad) {
        double probability = parseProbability(ad.getProbability());
        return probability * ad.getReward() / (ad.getExpiresIn() + 1);
    }

    private double parseProbability(String probability) {
        String normalized = probability.toLowerCase().replace(" ", "");
        return switch (normalized) {
            case "pieceofcake" -> 1.0;
            case "surething" -> 0.9;
            case "walkinthepark" -> 0.8;
            case "risky" -> 0.5;
            case "playingwithfire" -> 0.3;
            case "suicidemission" -> 0.1;
            default -> 0.1;
        };
    }
}