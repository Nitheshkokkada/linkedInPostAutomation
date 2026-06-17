package com.linkedinagent.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadabilityUtilTest {

    @Test
    void fleschReadingEaseReturnsHigherScoreForSimpleText() {
        String simple = "This is a short sentence. It is easy to read. Words are simple.";
        String complex = "Notwithstanding the multifaceted methodological paradigms inherent to "
                + "epistemological frameworks, practitioners must operationalize comprehensive strategies.";

        double simpleEase = ReadabilityUtil.fleschReadingEase(simple);
        double complexEase = ReadabilityUtil.fleschReadingEase(complex);

        assertThat(simpleEase).isGreaterThan(complexEase);
    }

    @Test
    void toReadabilityScoreReturnsValueBetweenZeroAndTwentyFive() {
        String text = "Professional teams adopt AI agents to accelerate delivery. "
                + "The results are measurable and practical for most organizations.";

        int score = ReadabilityUtil.toReadabilityScore(text);

        assertThat(score).isBetween(0, 25);
    }

    @Test
    void countWordsHandlesBlankText() {
        assertThat(ReadabilityUtil.countWords("")).isZero();
        assertThat(ReadabilityUtil.countWords("one two three")).isEqualTo(3);
    }

    @Test
    void countSentencesReturnsAtLeastOne() {
        assertThat(ReadabilityUtil.countSentences("No punctuation here")).isEqualTo(1);
        assertThat(ReadabilityUtil.countSentences("One. Two! Three?")).isEqualTo(3);
    }
}
