package com.linkedinagent.util;

import com.linkedinagent.domain.LinkedInRegisterUploadResponse;
import com.linkedinagent.exception.LinkedInApiException;
import com.linkedinagent.exception.LinkedInTokenExpiredException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkedInApiClient {

    private static final String REGISTER_UPLOAD_URL = "https://api.linkedin.com/v2/assets?action=registerUpload";
    private static final String UGC_POSTS_URL = "https://api.linkedin.com/v2/ugcPosts";
    private static final String RESTLI_VERSION = "2.0.0";
    private static final String IMAGE_RECIPE = "urn:li:digitalmediaRecipe:feedshare-image";

    private final RestTemplate restTemplate;

    @Retry(name = "linkedin")
    public String uploadImageAndGetAssetUrn(String accessToken, String personUrn, byte[] imageBytes) {
        LinkedInRegisterUploadResponse registerResponse = registerUpload(accessToken, personUrn);
        String uploadUrl = registerResponse.value().uploadMechanism()
                .mediaUploadHttpRequest().uploadUrl();
        String assetUrn = registerResponse.value().asset();

        uploadBinary(uploadUrl, imageBytes, accessToken);
        return assetUrn;
    }

    @Retry(name = "linkedin")
    public String publishImagePost(String accessToken, String personUrn, String assetUrn, String text, String title) {
        HttpHeaders headers = linkedInHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("author", personUrn);
        body.put("lifecycleState", "PUBLISHED");
        body.put("specificContent", Map.of(
                "com.linkedin.ugc.ShareContent", Map.of(
                        "shareCommentary", Map.of("text", text),
                        "shareMediaCategory", "IMAGE",
                        "media", List.of(Map.of(
                                "status", "READY",
                                "description", Map.of("text", title != null ? title : "Post image"),
                                "media", assetUrn,
                                "title", Map.of("text", title != null ? title : "Post")
                        ))
                )
        ));
        body.put("visibility", Map.of(
                "com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"
        ));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    UGC_POSTS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);

            String postId = response.getHeaders().getFirst("X-RestLi-Id");
            if (postId == null || postId.isBlank()) {
                postId = response.getHeaders().getFirst("x-restli-id");
            }
            if (postId == null || postId.isBlank()) {
                throw new LinkedInApiException("LinkedIn publish response missing post ID header");
            }
            return postId;
        } catch (HttpStatusCodeException e) {
            handleLinkedInError(e);
            throw new LinkedInApiException("LinkedIn publish failed: " + e.getStatusCode(), e);
        }
    }

    public String buildPostUrl(String linkedinPostId) {
        return "https://www.linkedin.com/feed/update/" + linkedinPostId;
    }

    public String toPersonUrn(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new LinkedInApiException("LinkedIn profile ID is not configured");
        }
        if (profileId.startsWith("urn:li:person:")) {
            return profileId;
        }
        return "urn:li:person:" + profileId;
    }

    private LinkedInRegisterUploadResponse registerUpload(String accessToken, String personUrn) {
        HttpHeaders headers = linkedInHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> registerUploadRequest = Map.of(
                "registerUploadRequest", Map.of(
                        "recipes", List.of(IMAGE_RECIPE),
                        "owner", personUrn,
                        "serviceRelationships", List.of(Map.of(
                                "relationshipType", "OWNER",
                                "identifier", "urn:li:userGeneratedContent"
                        ))
                )
        );

        try {
            ResponseEntity<LinkedInRegisterUploadResponse> response = restTemplate.exchange(
                    REGISTER_UPLOAD_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(registerUploadRequest, headers),
                    LinkedInRegisterUploadResponse.class);

            if (response.getBody() == null
                    || response.getBody().value() == null
                    || response.getBody().value().asset() == null) {
                throw new LinkedInApiException("LinkedIn register upload returned empty response");
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            handleLinkedInError(e);
            throw new LinkedInApiException("LinkedIn register upload failed: " + e.getStatusCode(), e);
        }
    }

    private void uploadBinary(String uploadUrl, byte[] imageBytes, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        try {
            restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.PUT,
                    new HttpEntity<>(imageBytes, headers),
                    Void.class);
        } catch (HttpStatusCodeException e) {
            handleLinkedInError(e);
            throw new LinkedInApiException("LinkedIn image upload failed: " + e.getStatusCode(), e);
        }
    }

    private HttpHeaders linkedInHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("X-Restli-Protocol-Version", RESTLI_VERSION);
        return headers;
    }

    private void handleLinkedInError(HttpStatusCodeException e) {
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.error("LinkedIn token expired or invalid: {}", e.getResponseBodyAsString());
            throw new LinkedInTokenExpiredException("LinkedIn access token expired or invalid");
        }
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("LinkedIn rate limit hit: {}", e.getResponseBodyAsString());
            throw new LinkedInApiException("LinkedIn rate limit exceeded");
        }
        log.error("LinkedIn API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
    }
}
