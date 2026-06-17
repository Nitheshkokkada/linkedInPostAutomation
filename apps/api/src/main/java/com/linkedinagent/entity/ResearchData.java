package com.linkedinagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "research_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchData {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "source_title", length = 500)
    private String sourceTitle;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_concepts", columnDefinition = "jsonb")
    private List<String> keyConcepts;

    @Column(name = "relevance_score")
    private Float relevanceScore;

    @Column(name = "fetched_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime fetchedAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private Topic topic;
}
