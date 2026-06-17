package com.linkedinagent.util;

import com.linkedinagent.domain.PostContent;

import java.util.List;
import java.util.stream.Collectors;

public final class PostContentAssembler {

    private PostContentAssembler() {
    }

    public static String assembleFullText(PostContent content) {
        StringBuilder sb = new StringBuilder();

        sb.append(content.hook().trim()).append("\n\n");
        sb.append(content.body().trim()).append("\n\n");
        sb.append("✅ Key Takeaways:\n");

        List<String> takeaways = content.keyTakeaways();
        for (int i = 0; i < takeaways.size(); i++) {
            sb.append(i + 1).append(". ").append(takeaways.get(i).trim()).append('\n');
        }

        sb.append('\n').append(content.callToAction().trim()).append("\n\n");

        String hashtags = content.hashtags().stream()
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                .collect(Collectors.joining(" "));
        sb.append(hashtags);

        return sb.toString().trim();
    }

    public static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    public static int countPostWords(PostContent content) {
        String combined = content.hook() + " " + content.body();
        return countWords(combined);
    }
}
