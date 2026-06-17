package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.GeminiReviewAnalysis;
import com.linkedinagent.domain.ReviewOutput;
import com.linkedinagent.domain.ReviewScoreBreakdown;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.ResearchData;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.ResearchDataRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.CosineSimilarityUtil;
import com.linkedinagent.util.GeminiRateLimiter;
import com.linkedinagent.util.ReadabilityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAgent {

    public static final String AGENT_NAME = "ReviewAgent";
    private static final int APPROVAL_THRESHOLD = 85;
    private static final int MAX_SCORE_PER_DIMENSION = 25;
    private static final double SIMILARITY_THRESHOLD = 0.85;
    private static final int MAX_COMPARISON_POSTS = 10;

    private final GeneratedPostRepository generatedPostRepository;
    private final ResearchDataRepository researchDataRepository;
    private final AgentLogService agentLogService;
    private final GeminiRateLimiter geminiRateLimiter;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReviewOutput run(UUID userId, UUID runId, UUID postId) {
        long startMs = System.currentTimeMillis();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Reviewing post " + postId).getId();

        try {
            GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            if (post.getStatus() != PostStatus.draft) {
                throw new AgentException("Only draft posts can be reviewed (current status: " + post.getStatus() + ")");
            }

            String researchSummary = loadResearchSummary(post.getResearchId());
            GeminiReviewAnalysis geminiReview = requestGeminiReview(post, researchSummary);
            int readabilityScore = ReadabilityUtil.toReadabilityScore(post.getFullText());
            OriginalityResult originality = scoreOriginality(userId, post);

            ReviewScoreBreakdown breakdown = new ReviewScoreBreakdown(
                    clampScore(geminiReview.grammarClarity()),
                    originality.score(),
                    readabilityScore,
                    clampScore(geminiReview.technicalAccuracy()),
                    originality.maxSimilarity(),
                    originality.tooSimilar()
            );

            int totalScore = breakdown.total();
            boolean approved = totalScore >= APPROVAL_THRESHOLD && !originality.tooSimilar();

            Map<String, Object> qualityFeedback = buildQualityFeedback(breakdown, geminiReview, post.getFullText());
            String rejectionReason = approved ? null : resolveRejectionReason(geminiReview, breakdown, originality);

            post.setQualityScore(totalScore);
            post.setQualityFeedback(qualityFeedback);
            post.setStatus(approved ? PostStatus.approved : PostStatus.rejected);
            post.setRejectionReason(rejectionReason);

            try {
                generatedPostRepository.save(post);
            } catch (Exception e) {
                log.error("Failed to save review results for post={}", postId, e);
                throw new AgentException("Failed to save review results", e);
            }

            String outputSummary = "Score " + totalScore + "/100 — " + (approved ? "approved" : "rejected");
            agentLogService.completeLog(logId, AgentStatus.success, outputSummary, null,
                    System.currentTimeMillis() - startMs);

            log.info("ReviewAgent completed for post={}, score={}, approved={}", postId, totalScore, approved);

            return new ReviewOutput(
                    postId,
                    totalScore,
                    approved,
                    post.getStatus(),
                    breakdown,
                    qualityFeedback,
                    rejectionReason
            );
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            if (e instanceof AgentException || e instanceof ResourceNotFoundException) {
                throw e;
            }
            throw new AgentException("Post review failed", e);
        }
    }

    private String loadResearchSummary(UUID researchId) {
        if (researchId == null) {
            return "No research context available.";
        }
        return researchDataRepository.findById(researchId)
                .map(ResearchData::getSummary)
                .orElse("No research context available.");
    }

    private GeminiReviewAnalysis requestGeminiReview(GeneratedPost post, String researchSummary) {
        String promptText = """
                You are a LinkedIn content quality reviewer for professional tech posts.
                Evaluate the post below for grammar/clarity and technical accuracy against the research context.

                RESEARCH CONTEXT:
                %s

                POST TO REVIEW:
                %s

                Score each dimension from 0 to 25 (integers only):
                - grammarClarity: grammar, spelling, clarity, flow
                - technicalAccuracy: factual correctness vs research context

                Return ONLY valid JSON (no markdown):
                {
                  "grammarClarity": 22,
                  "technicalAccuracy": 20,
                  "feedback": "specific improvement notes",
                  "rejectionReason": "only if critical issues, otherwise empty string"
                }
                """.formatted(researchSummary, post.getFullText());

        String response = geminiRateLimiter.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getText();

        return parseReviewAnalysis(response);
    }

    private OriginalityResult scoreOriginality(UUID userId, GeneratedPost currentPost) {
        List<GeneratedPost> recentPosts = generatedPostRepository.findRecentByUserId(
                userId, PageRequest.of(0, MAX_COMPARISON_POSTS + 1));

        List<String> textsToEmbed = new ArrayList<>();
        textsToEmbed.add(currentPost.getFullText());

        for (GeneratedPost previous : recentPosts) {
            if (!previous.getId().equals(currentPost.getId())
                    && previous.getFullText() != null
                    && !previous.getFullText().isBlank()) {
                textsToEmbed.add(previous.getFullText());
            }
        }

        if (textsToEmbed.size() == 1) {
            return new OriginalityResult(25, 0.0, false);
        }

        List<float[]> embeddings = geminiRateLimiter.embedBatch(textsToEmbed);
        float[] currentEmbedding = embeddings.getFirst();

        double maxSimilarity = 0.0;
        for (int i = 1; i < embeddings.size(); i++) {
            maxSimilarity = Math.max(maxSimilarity,
                    CosineSimilarityUtil.similarity(currentEmbedding, embeddings.get(i)));
        }

        boolean tooSimilar = maxSimilarity > SIMILARITY_THRESHOLD;
        int score;
        if (tooSimilar) {
            score = 0;
        } else {
            score = (int) Math.round(25.0 * (1.0 - maxSimilarity / SIMILARITY_THRESHOLD));
        }

        return new OriginalityResult(Math.clamp(score, 0, MAX_SCORE_PER_DIMENSION), maxSimilarity, tooSimilar);
    }

    private Map<String, Object> buildQualityFeedback(
            ReviewScoreBreakdown breakdown,
            GeminiReviewAnalysis geminiReview,
            String postText) {
        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("grammar_clarity", breakdown.grammarClarity());
        feedback.put("originality", breakdown.originality());
        feedback.put("readability", breakdown.readability());
        feedback.put("technical_accuracy", breakdown.technicalAccuracy());
        feedback.put("max_similarity", breakdown.maxSimilarity());
        feedback.put("too_similar", breakdown.tooSimilar());
        feedback.put("flesch_reading_ease", ReadabilityUtil.fleschReadingEase(postText));
        feedback.put("gemini_feedback", geminiReview.feedback());
        return feedback;
    }

    private String resolveRejectionReason(
            GeminiReviewAnalysis geminiReview,
            ReviewScoreBreakdown breakdown,
            OriginalityResult originality) {

        if (originality.tooSimilar()) {
            return "Post is too similar to a previous post (similarity="
                    + String.format("%.2f", originality.maxSimilarity()) + ")";
        }
        if (geminiReview.rejectionReason() != null && !geminiReview.rejectionReason().isBlank()) {
            return geminiReview.rejectionReason();
        }
        return "Quality score " + breakdown.total() + "/100 is below approval threshold of " + APPROVAL_THRESHOLD;
    }

    private GeminiReviewAnalysis parseReviewAnalysis(String response) {
        try {
            String json = extractJson(response);
            GeminiReviewAnalysis analysis = objectMapper.readValue(json, GeminiReviewAnalysis.class);
            if (analysis.grammarClarity() < 0 || analysis.technicalAccuracy() < 0) {
                throw new AgentException("Gemini returned invalid review scores");
            }
            return analysis;
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini review response: {}", response, e);
            throw new AgentException("Failed to parse Gemini review analysis");
        }
    }

    private int clampScore(int score) {
        return Math.clamp(score, 0, MAX_SCORE_PER_DIMENSION);
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AgentException("Gemini response did not contain JSON");
        }
        return response.substring(start, end + 1);
    }

    private record OriginalityResult(int score, double maxSimilarity, boolean tooSimilar) {
    }
}
