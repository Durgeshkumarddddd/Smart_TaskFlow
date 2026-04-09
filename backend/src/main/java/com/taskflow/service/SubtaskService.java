package com.taskflow.service;

import com.taskflow.dto.SubtaskDTO;
import com.taskflow.dto.SubtaskSummaryDTO;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Subtask;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.SubtaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubtaskService {

    @Autowired
    private SubtaskRepository subtaskRepository;


    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    public SubtaskDTO createSubtask(Long taskId, String title, Long assignedToId) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Subtask title is required");
        }
        User current = getCurrentUser();
        Task task = taskService.getAccessibleTask(taskId);

        if (current.getRole().name().equals("VIEWER")) {
            throw new ForbiddenException("Viewer cannot create subtasks");
        }

        Subtask.SubtaskBuilder builder = Subtask.builder()
                .title(title.trim())
                .isComplete(false)
                .task(task)
                .createdBy(current);

        if (assignedToId != null) {
            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found"));
            builder.assignedTo(assignee);
        }

        Subtask saved = subtaskRepository.save(builder.build());
        return toDTO(saved);
    }

    public List<SubtaskDTO> listSubtasks(Long taskId) {
        requireTaskAccess(taskId);
        return subtaskRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SubtaskSummaryDTO getSummary(Long taskId) {
        requireTaskAccess(taskId);
        List<Subtask> list = subtaskRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        long total = list.size();
        long completed = list.stream().filter(Subtask::isComplete).count();
        return SubtaskSummaryDTO.builder()
                .total(total)
                .completed(completed)
                .allDone(total > 0 && completed == total)
                .build();
    }

    @Transactional
    public SubtaskDTO toggleComplete(Long subtaskId) {
        User current = getCurrentUser();
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
        requireTaskAccess(subtask.getTask().getId());

        if (current.getRole().name().equals("VIEWER")) {
            throw new ForbiddenException("Viewer cannot modify subtasks");
        }

        boolean isCreator = subtask.getCreatedBy().getId().equals(current.getId());
        boolean isManager = current.getRole().name().equals("MANAGER");
        boolean isAdmin = current.getRole().name().equals("ADMIN");
        if (!(isCreator || isManager || isAdmin)) {
            throw new ForbiddenException("Not allowed to modify this subtask");
        }

        boolean newComplete = !subtask.isComplete();
        subtask.setComplete(newComplete);
        subtask.setCompletedAt(newComplete ? LocalDateTime.now() : null);

        return toDTO(subtaskRepository.save(subtask));
    }

    public void deleteSubtask(Long subtaskId) {
        User current = getCurrentUser();
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
        requireTaskAccess(subtask.getTask().getId());
        if (current.getRole().name().equals("VIEWER")) {
            throw new ForbiddenException("Viewer cannot delete subtasks");
        }

        boolean isCreator = subtask.getCreatedBy().getId().equals(current.getId());
        boolean isManager = current.getRole().name().equals("MANAGER");
        boolean isAdmin = current.getRole().name().equals("ADMIN");
        if (!(isCreator || isManager || isAdmin)) {
            throw new ForbiddenException("Not allowed to delete this subtask");
        }

        subtaskRepository.delete(subtask);
    }

    public SubtaskDTO getById(Long id) {
        Subtask subtask = subtaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
        requireTaskAccess(subtask.getTask().getId());
        return toDTO(subtask);
    }

    @Transactional
    public SubtaskDTO updateSubtask(Long id, String title, Long assignedToId) {
        User current = getCurrentUser();
        Subtask subtask = subtaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
        requireTaskAccess(subtask.getTask().getId());

        boolean isCreator = subtask.getCreatedBy().getId().equals(current.getId());
        boolean isManager = current.getRole().name().equals("MANAGER");
        boolean isAdmin = current.getRole().name().equals("ADMIN");
        if (!(isCreator || isManager || isAdmin)) {
            throw new ForbiddenException("Not allowed to modify this subtask");
        }

        if (title != null && !title.isBlank()) {
            subtask.setTitle(title.trim());
        }
        if (assignedToId != null) {
            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found"));
            subtask.setAssignedTo(assignee);
        } else {
            subtask.setAssignedTo(null);
        }

        return toDTO(subtaskRepository.save(subtask));
    }

    private void requireTaskAccess(Long taskId) {
        // Will throw if the current user cannot see this task
        taskService.getAccessibleTask(taskId);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("User is not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private SubtaskDTO toDTO(Subtask s) {
        SubtaskDTO.SubtaskDTOBuilder builder = SubtaskDTO.builder()
                .id(s.getId())
                .title(s.getTitle())
                .isComplete(s.isComplete())
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt());

        if (s.getAssignedTo() != null) {
            builder.assignedToId(s.getAssignedTo().getId())
                   .assignedToName(s.getAssignedTo().getUsername());
        }
        if (s.getCreatedBy() != null) {
            builder.createdById(s.getCreatedBy().getId())
                   .createdByName(s.getCreatedBy().getUsername());
        }

        return builder.build();
    }
}

