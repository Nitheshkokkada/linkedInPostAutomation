package com.linkedinagent.entity;

import com.linkedinagent.entity.enums.PostingMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "linkedin_access_token", columnDefinition = "TEXT")
    private String linkedinAccessToken;

    @Column(name = "linkedin_profile_id")
    private String linkedinProfileId;

    @Column(name = "linkedin_profile_url", length = 500)
    private String linkedinProfileUrl;

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_mode", nullable = false)
    @Builder.Default
    private PostingMode postingMode = PostingMode.draft;

    @Column(name = "preferred_post_time", nullable = false)
    @Builder.Default
    private LocalTime preferredPostTime = LocalTime.of(9, 0);

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
