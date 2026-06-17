package com.linkedinagent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "gemini_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiUsage {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "usage_date", nullable = false, unique = true)
    private LocalDate usageDate;

    @Column(name = "call_count", nullable = false)
    @Builder.Default
    private Integer callCount = 0;

    @Column(name = "last_updated", nullable = false)
    @Builder.Default
    private OffsetDateTime lastUpdated = OffsetDateTime.now();
}
