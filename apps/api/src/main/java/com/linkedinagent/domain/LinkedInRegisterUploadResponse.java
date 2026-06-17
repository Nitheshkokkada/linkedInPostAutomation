package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LinkedInRegisterUploadResponse(
        Value value
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            UploadMechanism uploadMechanism,
            String asset
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UploadMechanism(
            @JsonProperty("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest")
            MediaUploadHttpRequest mediaUploadHttpRequest
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MediaUploadHttpRequest(
            String uploadUrl,
            String headers
    ) {
    }
}
