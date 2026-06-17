package com.linkedinagent.entity;

import com.linkedinagent.entity.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "generated_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPost {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_id")
    private UUID topicId;

    @Column(name = "research_id")
    private UUID researchId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "hook", columnDefinition = "TEXT")
    private String hook;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_takeaways", columnDefinition = "jsonb")
    private List<String> keyTakeaways;

    @Column(name = "call_to_action", columnDefinition = "TEXT")
    private String callToAction;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_feedback", columnDefinition = "jsonb")
    private Map<String, Object> qualityFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PostStatus status = PostStatus.draft;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_id", insertable = false, updatable = false)
    private ResearchData researchData;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
