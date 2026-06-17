package com.linkedinagent.agent;

import com.linkedinagent.domain.ImageGenerationOutput;
import com.linkedinagent.entity.GeneratedImage;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedImageRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiImageClient;
import com.linkedinagent.util.ImageResizeUtil;
import com.linkedinagent.util.SupabaseStorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageAgent {

    public static final String AGENT_NAME = "ImageAgent";
    private static final int TARGET_SIZE = 1080;

    private final GeneratedPostRepository generatedPostRepository;
    private final GeneratedImageRepository generatedImageRepository;
    private final UserRepository userRepository;
    private final AgentLogService agentLogService;
    private final GeminiImageClient geminiImageClient;
    private final SupabaseStorageClient supabaseStorageClient;

    @Transactional
    public ImageGenerationOutput run(UUID userId, UUID runId, UUID postId) {
        long startMs = System.currentTimeMillis();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Generating image for post " + postId).getId();

        try {
            GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            if (post.getStatus() != PostStatus.approved) {
                throw new AgentException("Only approved posts can have images generated (status: " + post.getStatus() + ")");
            }

            if (generatedImageRepository.existsByPostId(postId)) {
                throw new AgentException("Image already exists for post " + postId);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            String brandFooter = resolveBrandFooter(user);
            String prompt = buildImagePrompt(post, brandFooter);
            String storagePath = "images/" + userId + "/" + postId + ".png";

            byte[] rawImage = geminiImageClient.generateImage(prompt);
            byte[] resizedImage = ImageResizeUtil.resizeToSquarePng(rawImage, TARGET_SIZE);
            String publicUrl = supabaseStorageClient.uploadPng(storagePath, resizedImage);

            GeneratedImage image = GeneratedImage.builder()
                    .postId(postId)
                    .storagePath(storagePath)
                    .publicUrl(publicUrl)
                    .promptUsed(prompt)
                    .width(TARGET_SIZE)
                    .height(TARGET_SIZE)
                    .build();

            try {
                image = generatedImageRepository.save(image);
            } catch (Exception e) {
                log.error("Failed to save generated image for post={}", postId, e);
                throw new AgentException("Failed to save generated image", e);
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Image saved at " + storagePath, null, System.currentTimeMillis() - startMs);

            log.info("ImageAgent completed for post={}, imageId={}", postId, image.getId());

            return new ImageGenerationOutput(
                    image.getId(),
                    postId,
                    storagePath,
                    publicUrl,
                    prompt,
                    TARGET_SIZE,
                    TARGET_SIZE
            );
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            if (e instanceof AgentException || e instanceof ResourceNotFoundException) {
                throw e;
            }
            throw new AgentException("Image generation failed", e);
        }
    }

    String buildImagePrompt(GeneratedPost post, String brandFooter) {
        List<String> keyPoints = normalizeKeyPoints(post.getKeyTakeaways());
        String title = post.getTitle() != null ? post.getTitle() : "LinkedIn Post";

        return "Professional LinkedIn infographic 1080x1080 modern flat design dark navy background "
                + "white and cyan typography. Title: %s. Three key point cards: %s | %s | %s. "
                + "Brand footer: %s. Clean minimal tech style."
                .formatted(title, keyPoints.get(0), keyPoints.get(1), keyPoints.get(2), brandFooter);
    }

    private List<String> normalizeKeyPoints(List<String> keyTakeaways) {
        List<String> points = new ArrayList<>();
        if (keyTakeaways != null) {
            points.addAll(keyTakeaways.stream()
                    .filter(point -> point != null && !point.isBlank())
                    .map(String::trim)
                    .toList());
        }

        while (points.size() < 3) {
            points.add("Key insight " + (points.size() + 1));
        }

        return points.subList(0, 3);
    }

    private String resolveBrandFooter(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return "LinkedIn AI Agent";
    }
}
