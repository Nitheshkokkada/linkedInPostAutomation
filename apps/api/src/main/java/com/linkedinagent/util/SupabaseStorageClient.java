package com.linkedinagent.util;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.exception.StorageException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class SupabaseStorageClient {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public SupabaseStorageClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Retry(name = "supabase")
    public String uploadPng(String objectPath, byte[] pngBytes) {
        validateConfiguration();

        String uploadUrl = buildObjectUrl(objectPath);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(appProperties.getSupabase().getServiceKey());
        headers.setContentType(MediaType.IMAGE_PNG);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(pngBytes, headers),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new StorageException("Supabase upload failed with status " + response.getStatusCode());
            }

            return buildPublicUrl(objectPath);
        } catch (HttpStatusCodeException e) {
            log.error("Supabase upload failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new StorageException("Supabase upload failed: " + e.getStatusCode(), e);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Supabase upload error for path={}", objectPath, e);
            throw new StorageException("Supabase upload failed", e);
        }
    }

    @Retry(name = "supabase")
    public byte[] download(String objectPath) {
        validateConfiguration();

        String downloadUrl = buildObjectUrl(objectPath);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(appProperties.getSupabase().getServiceKey());

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class);

            if (response.getBody() == null || response.getBody().length == 0) {
                throw new StorageException("Supabase returned empty file for path " + objectPath);
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Supabase download failed: status={}", e.getStatusCode());
            throw new StorageException("Supabase download failed: " + e.getStatusCode(), e);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Supabase download error for path={}", objectPath, e);
            throw new StorageException("Supabase download failed", e);
        }
    }

    public String buildPublicUrl(String objectPath) {
        validateConfiguration();
        return appProperties.getSupabase().getUrl()
                + "/storage/v1/object/public/"
                + appProperties.getSupabase().getStorageBucket()
                + "/"
                + objectPath;
    }

    private String buildObjectUrl(String objectPath) {
        return appProperties.getSupabase().getUrl()
                + "/storage/v1/object/"
                + appProperties.getSupabase().getStorageBucket()
                + "/"
                + objectPath;
    }

    private void validateConfiguration() {
        if (appProperties.getSupabase().getUrl() == null || appProperties.getSupabase().getUrl().isBlank()) {
            throw new StorageException("Supabase URL is not configured");
        }
        if (appProperties.getSupabase().getServiceKey() == null || appProperties.getSupabase().getServiceKey().isBlank()) {
            throw new StorageException("Supabase service key is not configured");
        }
    }
}
