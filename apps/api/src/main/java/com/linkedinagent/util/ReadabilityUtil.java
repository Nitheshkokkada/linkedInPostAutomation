package com.linkedinagent.util;

public final class ReadabilityUtil {

    private static final double IDEAL_EASE_MIN = 50.0;
    private static final double IDEAL_EASE_MAX = 70.0;
    private static final double IDEAL_EASE_TARGET = 60.0;

    private ReadabilityUtil() {
    }

    /**
     * Flesch Reading Ease score (0–100+, higher = easier to read).
     */
    public static double fleschReadingEase(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        int wordCount = countWords(text);
        int sentenceCount = countSentences(text);
        int syllableCount = countSyllables(text);

        if (wordCount == 0 || sentenceCount == 0) {
            return 0.0;
        }

        double wordsPerSentence = (double) wordCount / sentenceCount;
        double syllablesPerWord = (double) syllableCount / wordCount;

        return 206.835 - (1.015 * wordsPerSentence) - (84.6 * syllablesPerWord);
    }

    /**
     * Maps Flesch Reading Ease to a 0–25 quality sub-score for LinkedIn professional content.
     */
    public static int toReadabilityScore(String text) {
        double ease = fleschReadingEase(text);

        if (ease >= IDEAL_EASE_MIN && ease <= IDEAL_EASE_MAX) {
            return 25;
        }
        if (ease < 20.0 || ease > 90.0) {
            return 5;
        }

        double distance = Math.abs(ease - IDEAL_EASE_TARGET);
        int score = (int) Math.round(25 - distance * 0.4);
        return Math.clamp(score, 0, 25);
    }

    static int countWords(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }

    static int countSentences(String text) {
        String[] sentences = text.split("[.!?]+");
        int count = 0;
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    static int countSyllables(String text) {
        int total = 0;
        for (String word : text.split("\\s+")) {
            total += countWordSyllables(word);
        }
        return Math.max(total, 1);
    }

    static int countWordSyllables(String word) {
        String cleaned = word.toLowerCase().replaceAll("[^a-z]", "");
        if (cleaned.isEmpty()) {
            return 0;
        }
        if (cleaned.length() <= 3) {
            return 1;
        }

        String processed = cleaned
                .replaceAll("(?:[^laeiouy]es|ed|[^laeiouy]e)$", "")
                .replaceAll("^y", "");

        String[] vowelGroups = processed.split("[^aeiouy]+");
        int syllables = 0;
        for (String group : vowelGroups) {
            if (!group.isEmpty()) {
                syllables++;
            }
        }
        return Math.max(syllables, 1);
    }
}
