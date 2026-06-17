package com.linkedinagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analytics {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "published_post_id", nullable = false)
    private UUID publishedPostId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Integer impressions = 0;

    @Column(name = "likes", nullable = false)
    @Builder.Default
    private Integer likes = 0;

    @Column(name = "comments", nullable = false)
    @Builder.Default
    private Integer comments = 0;

    @Column(name = "shares", nullable = false)
    @Builder.Default
    private Integer shares = 0;

    @Column(name = "engagement_rate", nullable = false)
    @Builder.Default
    private Float engagementRate = 0.0f;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private OffsetDateTime fetchedAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_post_id", insertable = false, updatable = false)
    private PublishedPost publishedPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
