package com.linkedinagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedImage {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "public_url", length = 2000)
    private String publicUrl;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 1080;

    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 1080;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private GeneratedPost post;
}
