package com.linkedinagent.util;

import com.linkedinagent.domain.PostContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostContentAssemblerTest {

    @Test
    void assemblesFullTextInLinkedInFormat() {
        PostContent content = new PostContent(
                "Did you know Spring Boot 3 changed everything?",
                "The new observability features are a game changer for production systems.",
                List.of("Native tracing support", "Improved actuator endpoints", "Better metrics export"),
                "What's your experience with Spring Boot 3?",
                List.of("SpringBoot", "Java", "DevOps")
        );

        String fullText = PostContentAssembler.assembleFullText(content);

        assertThat(fullText).contains("Did you know Spring Boot 3 changed everything?");
        assertThat(fullText).contains("✅ Key Takeaways:");
        assertThat(fullText).contains("1. Native tracing support");
        assertThat(fullText).contains("2. Improved actuator endpoints");
        assertThat(fullText).contains("3. Better metrics export");
        assertThat(fullText).contains("What's your experience with Spring Boot 3?");
        assertThat(fullText).contains("#SpringBoot #Java #DevOps");
    }

    @Test
    void addsHashPrefixWhenMissing() {
        PostContent content = new PostContent(
                "Hook", "Body text here.", List.of("a", "b", "c"), "CTA", List.of("tag1"));
        String fullText = PostContentAssembler.assembleFullText(content);
        assertThat(fullText).endsWith("#tag1");
    }

    @Test
    void countsWordsCorrectly() {
        assertThat(PostContentAssembler.countWords("one two three")).isEqualTo(3);
        assertThat(PostContentAssembler.countWords("")).isZero();
        assertThat(PostContentAssembler.countWords(null)).isZero();
    }

    @Test
    void countsPostWordsFromHookAndBody() {
        PostContent content = new PostContent(
                "word one two", "word four five six",
                List.of("a", "b", "c"), "cta", List.of("tag"));
        assertThat(PostContentAssembler.countPostWords(content)).isEqualTo(6);
    }
}
