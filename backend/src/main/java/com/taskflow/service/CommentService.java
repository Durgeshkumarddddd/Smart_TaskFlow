package com.taskflow.service;

import com.taskflow.dto.CommentDTO;
import com.taskflow.dto.WebsocketNotificationDTO;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Task;
import com.taskflow.model.TaskComment;
import com.taskflow.model.User;
import com.taskflow.repository.TaskCommentRepository;
// import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private TaskCommentRepository commentRepository;

    // @Autowired
    // private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private NotificationService notificationService;

    // Get all comments for a task (chronological order).

    public List<CommentDTO> getCommentsByTaskId(Long taskId) {
        // Verify the user has access to this task
        taskService.getAccessibleTask(taskId);

        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Post a new comment on a task.
    // TC-C01: Returns 201 with author + timestamp.(TC-C01 Means Which Features are
    // Expand in second phase of project)

    @Transactional
    public CommentDTO addComment(Long taskId, String body) {
        User currentUser = getCurrentUser();
        Task task = taskService.getAccessibleTask(taskId);

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(currentUser)
                .body(body.trim())
                .build();

        TaskComment saved = commentRepository.save(comment);

        // Log activity
        activityLogService.log(task, currentUser,
                ActivityLogService.COMMENT_ADDED,
                "commented on \"" + task.getTitle() + "\"");

        // Notify the task owner, assignee, and team members (Excluding the commenter)
        var payload = new HashMap<String, Object>();
        payload.put("taskId", task.getId());
        payload.put("taskTitle", task.getTitle());
        payload.put("commentId", saved.getId());
        payload.put("message", currentUser.getUsername() + " commented on \"" + task.getTitle() + "\"");

        WebsocketNotificationDTO notification = WebsocketNotificationDTO.builder()
                .type(ActivityLogService.COMMENT_ADDED)
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        // Notify creator if they aren't the commenter
        if (!task.getUser().getId().equals(currentUser.getId())) {
            notificationService.notifyUser(task.getUser().getEmail(), notification);
        }

        // Notify assignees if they aren't the commenter
        for (User assignee : task.getAssignees()) {
            if (!assignee.getId().equals(currentUser.getId())) {
                notificationService.notifyUser(assignee.getEmail(), notification);
            }
        }

        return toDTO(saved);
    }

    // TC-C01 Means Which Features are Expand in second phase of project
    // TC-C03: Only the author can delete their own comment.
    // TC-C04: Others get 403 Forbidden.

    @Transactional
    public void deleteComment(Long commentId) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        User currentUser = getCurrentUser();

        // Ensure the user has access to the task; if not, treat as forbidden rather
        // than not found
        try {
            taskService.getAccessibleTask(comment.getTask().getId());
        } catch (ResourceNotFoundException ex) {
            throw new ForbiddenException("You do not have access to this comment");
        }

        if (!comment.getAuthor().getId().equals(currentUser.getId())
                && currentUser.getRole() != com.taskflow.model.Role.ADMIN) {
            throw new ForbiddenException("You can only delete your own comments");
        }

        Task task = comment.getTask();
        commentRepository.delete(comment);

        // Log activity
        activityLogService.log(task, currentUser,
                ActivityLogService.COMMENT_DELETED,
                "deleted a comment on \"" + task.getTitle() + "\"");

        var payload = new HashMap<String, Object>();
        payload.put("taskId", task.getId());
        payload.put("taskTitle", task.getTitle());
        payload.put("message", "deleted a comment on \"" + task.getTitle() + "\"");

        WebsocketNotificationDTO notification = WebsocketNotificationDTO.builder()
                .type(ActivityLogService.COMMENT_DELETED)
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        notificationService.notifyUser(task.getUser().getEmail(), notification);
        for (User assignee : task.getAssignees()) {
            if (!assignee.getId().equals(task.getUser().getId())) {
                notificationService.notifyUser(assignee.getEmail(), notification);
            }
        }
        if (task.getTeam() != null) {
            notificationService.notifyTeam(task.getTeam().getId(), notification);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CommentDTO toDTO(TaskComment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getUsername())
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
