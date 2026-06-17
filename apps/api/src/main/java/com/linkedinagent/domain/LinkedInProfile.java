package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LinkedInProfile(
        String id,
        String localizedFirstName,
        String localizedLastName
) {
    public String fullName() {
        return ((localizedFirstName != null ? localizedFirstName : "") + " "
                + (localizedLastName != null ? localizedLastName : "")).trim();
    }
}
