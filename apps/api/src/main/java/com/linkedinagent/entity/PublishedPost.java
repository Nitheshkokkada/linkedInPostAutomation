package com.linkedinagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "published_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishedPost {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scheduled_post_id", nullable = false)
    private UUID scheduledPostId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "linkedin_post_id", unique = true)
    private String linkedinPostId;

    @Column(name = "linkedin_post_url", length = 2000)
    private String linkedinPostUrl;

    @Column(name = "published_at", nullable = false)
    @Builder.Default
    private OffsetDateTime publishedAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_post_id", insertable = false, updatable = false)
    private ScheduledPost scheduledPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
