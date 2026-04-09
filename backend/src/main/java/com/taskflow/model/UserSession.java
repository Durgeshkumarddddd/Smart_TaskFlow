package com.taskflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String jti;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(length = 100)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active", nullable = false)
    private LocalDateTime lastActive;

    @Column(length = 20, nullable = false)
    private String status; // ACTIVE or INACTIVE
}
