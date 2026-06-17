package com.linkedinagent.service;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.LinkedInProfile;
import com.linkedinagent.domain.LinkedInTokenResponse;
import com.linkedinagent.entity.User;
import com.linkedinagent.exception.LinkedInApiException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.security.JwtTokenProvider;
import com.linkedinagent.util.EncryptionUtil;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkedInOAuthService {

    private static final String AUTHORIZE_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String PROFILE_URL = "https://api.linkedin.com/v2/me";
    private static final String SCOPES = "openid profile email w_member_social";

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    public String buildAuthorizationUrl(UUID userId) {
        String state = jwtTokenProvider.generateAccessToken(userId, "linkedin-oauth-state");

        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", appProperties.getLinkedin().getClientId())
                .queryParam("redirect_uri", appProperties.getLinkedin().getRedirectUri())
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    @Transactional
    @Retry(name = "linkedin")
    public void handleCallback(String code, String state) {
        UUID userId = jwtTokenProvider.getUserIdFromToken(state);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LinkedInTokenResponse tokenResponse = exchangeCodeForToken(code);
        LinkedInProfile profile = fetchProfile(tokenResponse.accessToken());

        user.setLinkedinAccessToken(encryptionUtil.encrypt(tokenResponse.accessToken()));
        user.setLinkedinProfileId(profile.id());
        user.setLinkedinProfileUrl("https://www.linkedin.com/in/" + profile.id());

        try {
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to save LinkedIn credentials for user id={}", userId, e);
            throw new LinkedInApiException("Failed to save LinkedIn connection", e);
        }

        log.info("LinkedIn connected for user id={}, profileId={}", userId, profile.id());
    }

    @Transactional
    public void disconnect(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLinkedinAccessToken(null);
        user.setLinkedinProfileId(null);
        user.setLinkedinProfileUrl(null);

        try {
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to disconnect LinkedIn for user id={}", userId, e);
            throw new LinkedInApiException("Failed to disconnect LinkedIn account", e);
        }

        log.info("LinkedIn disconnected for user id={}", userId);
    }

    @Transactional(readOnly = true)
    public String getDecryptedAccessToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getLinkedinAccessToken() == null || user.getLinkedinAccessToken().isBlank()) {
            throw new LinkedInApiException("LinkedIn account not connected");
        }

        return encryptionUtil.decrypt(user.getLinkedinAccessToken());
    }

    @Retry(name = "linkedin")
    private LinkedInTokenResponse exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", appProperties.getLinkedin().getRedirectUri());
        body.add("client_id", appProperties.getLinkedin().getClientId());
        body.add("client_secret", appProperties.getLinkedin().getClientSecret());

        try {
            ResponseEntity<LinkedInTokenResponse> response = restTemplate.exchange(
                    TOKEN_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    LinkedInTokenResponse.class);

            if (response.getBody() == null || response.getBody().accessToken() == null) {
                throw new LinkedInApiException("LinkedIn returned empty token response");
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("LinkedIn token exchange failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LinkedInApiException("LinkedIn token exchange failed: " + e.getStatusCode());
        } catch (LinkedInApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("LinkedIn token exchange error", e);
            throw new LinkedInApiException("LinkedIn token exchange failed", e);
        }
    }

    @Retry(name = "linkedin")
    private LinkedInProfile fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<LinkedInProfile> response = restTemplate.exchange(
                    PROFILE_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    LinkedInProfile.class);

            if (response.getBody() == null || response.getBody().id() == null) {
                throw new LinkedInApiException("LinkedIn returned empty profile response");
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("LinkedIn profile fetch failed: status={}", e.getStatusCode());
            throw new LinkedInApiException("LinkedIn profile fetch failed: " + e.getStatusCode());
        } catch (LinkedInApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("LinkedIn profile fetch error", e);
            throw new LinkedInApiException("LinkedIn profile fetch failed", e);
        }
    }
}
