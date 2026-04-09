package com.taskflow.service;

import com.taskflow.model.RevokedToken;
import com.taskflow.model.UserSession;
import com.taskflow.repository.RevokedTokenRepository;
import com.taskflow.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserSessionService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    public UserSession createSession(Long userId, String jti, String userAgent, String platform) {
        LocalDateTime now = LocalDateTime.now();
        UserSession session = UserSession.builder()
                .userId(userId)
                .jti(jti)
                .userAgent(userAgent)
                .platform(platform)
                .createdAt(now)
                .lastActive(now)
                .status("ACTIVE")
                .build();
        return userSessionRepository.save(session);
    }

    public List<UserSession> getSessions(Long userId, String status) {
        if (status == null || status.isBlank()) {
            return userSessionRepository.findByUserId(userId);
        }
        return userSessionRepository.findByUserIdAndStatus(userId, status.toUpperCase());
    }

    public UserSession markRevoked(Long userId, Long sessionId) {
        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        session.setStatus("INACTIVE");
        session.setLastActive(LocalDateTime.now());
        UserSession updated = userSessionRepository.save(session);

        if (!revokedTokenRepository.existsByJti(session.getJti())) {
            revokedTokenRepository.save(RevokedToken.builder()
                    .jti(session.getJti())
                    .revokedAt(LocalDateTime.now())
                    .build());
        }

        return updated;
    }

    public void updateLastActive(String jti) {
        userSessionRepository.findByJti(jti).ifPresent(session -> {
            session.setLastActive(LocalDateTime.now());
            session.setStatus("ACTIVE");
            userSessionRepository.save(session);
        });
    }

    public boolean isRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }
}
