package com.linkedinagent.util;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageClientTest {

    @Mock
    private RestTemplate restTemplate;

    private SupabaseStorageClient storageClient;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getSupabase().setUrl("https://example.supabase.co");
        properties.getSupabase().setServiceKey("test-service-key");
        properties.getSupabase().setStorageBucket("linkedin-ai-images");
        storageClient = new SupabaseStorageClient(restTemplate, properties);
    }

    @Test
    void uploadPngReturnsPublicUrl() {
        byte[] png = new byte[]{1, 2, 3};
        String path = "images/user/post.png";

        when(restTemplate.exchange(
                eq("https://example.supabase.co/storage/v1/object/linkedin-ai-images/" + path),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<byte[]>>any(),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        String publicUrl = storageClient.uploadPng(path, png);

        assertThat(publicUrl).isEqualTo(
                "https://example.supabase.co/storage/v1/object/public/linkedin-ai-images/" + path);
    }

    @Test
    void uploadThrowsWhenServiceKeyMissing() {
        AppProperties properties = new AppProperties();
        properties.getSupabase().setUrl("https://example.supabase.co");
        properties.getSupabase().setStorageBucket("bucket");
        SupabaseStorageClient client = new SupabaseStorageClient(restTemplate, properties);

        assertThatThrownBy(() -> client.uploadPng("path.png", new byte[]{1}))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("service key");
    }
}
