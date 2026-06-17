package com.linkedinagent.entity;

import com.linkedinagent.entity.enums.PatternType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "learning_patterns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPattern {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type", nullable = false)
    private PatternType patternType;

    @Column(name = "topic_category", length = 50)
    private String topicCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_features", columnDefinition = "jsonb")
    private Map<String, Object> contentFeatures;

    @Column(name = "avg_engagement_rate", nullable = false)
    @Builder.Default
    private Float avgEngagementRate = 0.0f;

    @Column(name = "sample_size", nullable = false)
    @Builder.Default
    private Integer sampleSize = 0;

    @Column(name = "insight", columnDefinition = "TEXT")
    private String insight;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
