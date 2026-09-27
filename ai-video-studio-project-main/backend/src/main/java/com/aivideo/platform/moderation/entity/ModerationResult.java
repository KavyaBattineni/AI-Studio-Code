package com.aivideo.platform.moderation.entity;

import com.aivideo.platform.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * One row per moderation check performed (both ALLOWED and BLOCKED). inputSnippet is
 * intentionally truncated and is only ever surfaced to admins (see /api/admin/moderation) -
 * never returned to the user who submitted the prompt.
 */
@Entity
@Table(name = "moderation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    @Builder.Default
    private ModerationInputType inputType = ModerationInputType.PROMPT;

    @Column(name = "input_snippet", length = 500)
    private String inputSnippet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationDecision decision;

    @Column(length = 255)
    private String reason;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
