package com.taskflow.service;

import com.taskflow.dto.ActivityLogDTO;
import com.taskflow.model.ActivityLog;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.ActivityLogRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    // Action codes 
    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_DELETED = "TASK_DELETED";
    public static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String COMMENT_ADDED = "COMMENT_ADDED";
    public static final String COMMENT_DELETED = "COMMENT_DELETED";

    // Log an activity against a task( For Assign to More User)
    @Transactional
    public void log(Task task, User actor, String actionCode, String message) {
        ActivityLog entry = ActivityLog.builder()
                .task(task)
                .actor(actor)
                .actionCode(actionCode)
                .message(message)
                .build();
        activityLogRepository.save(entry);
    }
 
    // Overload: log with null task (e.g., after deletion) For Each Task
 
    @Transactional
    public void log(User actor, String actionCode, String message) {
        ActivityLog entry = ActivityLog.builder()
                .task(null)
                .actor(actor)
                .actionCode(actionCode)
                .message(message)
                .build();
        activityLogRepository.save(entry);
    }

    // activity feed: notificationsOnly ? (others' actions on my tasks) : (my history + others' actions)
    public List<ActivityLogDTO> getFeedForCurrentUser(boolean notificationsOnly) {
        Long userId = getCurrentUserId();
        
        List<ActivityLog> logs;
        if (notificationsOnly) {
            // Notification Bell: Only others' actions on my tasks (assigned or created)
            logs = activityLogRepository.findNotificationsForUser(userId, PageRequest.of(0, 20));
        } else {
            // Sidebar History: My actions + others' actions on tasks assigned to me
            List<ActivityLog> ownLogs = activityLogRepository.findByActorIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20));
            List<ActivityLog> assigneeLogs = activityLogRepository.findByTaskAssigneeId(userId, PageRequest.of(0, 20));

            // Merge, deduplicate, sort, limit
            java.util.Map<Long, ActivityLog> merged = new java.util.LinkedHashMap<>();
            for (ActivityLog log : ownLogs) {
                merged.put(log.getId(), log);
            }
            for (ActivityLog log : assigneeLogs) {
                merged.put(log.getId(), log);
            }
            logs = merged.values().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(20)
                    .collect(Collectors.toList());
        }

        return logs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Get activity feed by task.

    public List<ActivityLogDTO> getFeedByTask(Long taskId) {
        List<ActivityLog> logs = activityLogRepository
                .findByTaskIdOrderByCreatedAtDesc(taskId);
        return logs.stream().map(this::toDTO).collect(Collectors.toList());
    }


    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private ActivityLogDTO toDTO(ActivityLog log) {
        return ActivityLogDTO.builder()
                .id(log.getId())
                .taskId(log.getTask() != null ? log.getTask().getId() : null)
                .taskTitle(log.getTask() != null ? log.getTask().getTitle() : null)
                .actorId(log.getActor().getId())
                .actorName(log.getActor().getUsername())
                .actionCode(log.getActionCode())
                .message(log.getMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
