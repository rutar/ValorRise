package com.valorrise.bot.service;

import com.valorrise.bot.model.domain.Advertisement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TaskSelectionService {
    private static final Logger logger = LoggerFactory.getLogger(TaskSelectionService.class);
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=]+$");

    public Advertisement selectBestTask(List<Advertisement> advertisements) {
        if (advertisements == null || advertisements.isEmpty()) {
            logger.warn("No advertisements provided for task selection");
            return null;
        }

        Advertisement bestAd = null;
        double bestScore = -1;

        for (Advertisement ad : advertisements) {
            // Decode fields if encrypted
            Advertisement decodedAd = decodeAdvertisement(ad);
            // Skip traps
            if (isTrap(decodedAd)) {
                logger.debug("Skipping trap task: {}, probability: {}, reward: {}",
                        decodedAd.getAdId(), decodedAd.getProbability(), decodedAd.getReward());
                continue;
            }


            double score = decodedAd.getReward() * parseProbability(decodedAd.getProbability());

            if (score > bestScore) {
                bestScore = Math.round(score * 100.0) / 100.0;
                bestAd = decodedAd;
            }
        }

        if (bestAd != null) {
            logger.debug("Selected task: {}, score: {}, reward: {}",
                    bestAd.getAdId(), bestScore, bestAd.getReward());
        } else {
            logger.warn("No valid task selected");
        }

        return bestAd;
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
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Try Base64 first if it matches pattern
        if (BASE64_PATTERN.matcher(input).matches()) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(input);
                String decoded = new String(decodedBytes);
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
        return text != null && text.matches(".*[a-zA-Z\\s].*") && !text.contains("\uFFFD");
    }

    private boolean isTrap(Advertisement ad) {
        String probability = ad.getProbability().toLowerCase().replaceAll("\\s", "");
        List<String> validProbabilities = Arrays.asList(
                "surething",
                "pieceofcake",
                "walkinthepark",
                "hmmm....",
                "quitelikely",
                "gamble",
                "risky",
                "ratherdetrimental"
        );
        return !validProbabilities.contains(probability) ||
                ad.getReward() < 5 ||
                ad.getExpiresIn() <= 0;
    }

    private double parseProbability(String probability) {
        String normalized = probability.toLowerCase().replace(" ", "");
        return switch (normalized) {
            case "surething" -> 0.98;
            case "pieceofcake" -> 0.96;
            case "walkinthepark" -> 0.87;
            case "hmmm...." -> 0.78;
            case "quitelikely" -> 0.75;
            case "gamble" -> 0.55;
            case "risky" -> 0.46;
            case "ratherdetrimental" -> 0.33;
            case "playingwithfire" -> 0.25;
            default -> 0.1;
        };
    }
}