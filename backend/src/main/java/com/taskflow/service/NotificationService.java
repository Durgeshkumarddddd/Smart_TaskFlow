package com.taskflow.service;

import com.taskflow.dto.WebsocketNotificationDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Helper service for sending WebSocket notifications to connected clients.
 */
@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send a notification to all connected clients subscribed to the topic.
     */
    public void broadcast(String topic, WebsocketNotificationDTO notification) {
        messagingTemplate.convertAndSend(topic, notification);
    }

    /**
     * Send a notification to a specific user by their email (principal name).
     * Spring's convertAndSendToUser matches the principal name set during
     * WebSocket CONNECT authentication.
     */
    public void notifyUser(String userEmail, WebsocketNotificationDTO notification) {
        if (userEmail == null || userEmail.isBlank()) return;
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification);
    }

    /**
     * Send a notification to a team channel (e.g. /topic/teams/{teamId}).
     */
    public void notifyTeam(Long teamId, WebsocketNotificationDTO notification) {
        if (teamId == null) return;
        messagingTemplate.convertAndSend("/topic/teams/" + teamId, notification);
    }
}
