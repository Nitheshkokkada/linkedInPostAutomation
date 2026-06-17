package com.linkedinagent.agent;

import com.linkedinagent.domain.ImageGenerationOutput;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.GeneratedImage;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.repository.GeneratedImageRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiImageClient;
import com.linkedinagent.util.SupabaseStorageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageAgentTest {

    @Mock
    private GeneratedPostRepository generatedPostRepository;

    @Mock
    private GeneratedImageRepository generatedImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AgentLogService agentLogService;

    @Mock
    private GeminiImageClient geminiImageClient;

    @Mock
    private SupabaseStorageClient supabaseStorageClient;

    @InjectMocks
    private ImageAgent imageAgent;

    @Test
    void runGeneratesUploadsAndSavesImage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        GeneratedPost post = GeneratedPost.builder()
                .id(postId)
                .userId(userId)
                .title("AI Agents in Production")
                .keyTakeaways(List.of("Automate research", "Keep humans in loop", "Measure outcomes"))
                .status(PostStatus.approved)
                .build();

        User user = User.builder().id(userId).fullName("Jane Doe").build();
        byte[] pngBytes = createPng(512, 512);

        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(AgentLog.builder().id(logId).build());
        when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
        when(generatedImageRepository.existsByPostId(postId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(geminiImageClient.generateImage(anyString())).thenReturn(pngBytes);
        when(supabaseStorageClient.uploadPng(anyString(), any(byte[].class)))
                .thenReturn("https://example.supabase.co/storage/v1/object/public/bucket/images/" + userId + "/" + postId + ".png");

        GeneratedImage savedImage = GeneratedImage.builder()
                .id(imageId)
                .postId(postId)
                .build();
        when(generatedImageRepository.save(any(GeneratedImage.class))).thenReturn(savedImage);

        ImageGenerationOutput output = imageAgent.run(userId, runId, postId);

        assertThat(output.imageId()).isEqualTo(imageId);
        assertThat(output.postId()).isEqualTo(postId);
        assertThat(output.width()).isEqualTo(1080);
        assertThat(output.height()).isEqualTo(1080);
        assertThat(output.promptUsed()).contains("AI Agents in Production");
        assertThat(output.promptUsed()).contains("Jane Doe");

        ArgumentCaptor<GeneratedImage> imageCaptor = ArgumentCaptor.forClass(GeneratedImage.class);
        verify(generatedImageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getStoragePath()).isEqualTo("images/" + userId + "/" + postId + ".png");

        verify(agentLogService).completeLog(eq(logId), eq(AgentStatus.success), anyString(), eq(null), anyLong());
    }

    @Test
    void buildImagePromptUsesSpecFormat() {
        GeneratedPost post = GeneratedPost.builder()
                .title("Spring Boot 3")
                .keyTakeaways(List.of("Observability", "Native images", "Faster startup"))
                .build();

        String prompt = imageAgent.buildImagePrompt(post, "Acme Corp");

        assertThat(prompt).contains("1080x1080");
        assertThat(prompt).contains("Title: Spring Boot 3");
        assertThat(prompt).contains("Observability | Native images | Faster startup");
        assertThat(prompt).contains("Brand footer: Acme Corp");
    }

    private byte[] createPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.CYAN);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
