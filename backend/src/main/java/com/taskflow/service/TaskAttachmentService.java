package com.taskflow.service;

import com.taskflow.dto.TaskAttachmentDTO;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Task;
import com.taskflow.model.TaskAttachment;
import com.taskflow.model.User;
import com.taskflow.repository.TaskAttachmentRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskAttachmentService {

    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024L * 1024L; // 5MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip",
            "application/octet-stream"
    );

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAttachmentRepository attachmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamsService teamsService;

    public TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException("File size exceeds maximum allowed size of 5MB");
        }

        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Disallowed file type");
        }

        Task task = findTaskForCurrentUser(taskId);

        long existing = attachmentRepository.findByTaskId(taskId).size();
        if (existing >= 5) {
            throw new BadRequestException("Only 5 attachments are allowed per task");
        }

        User uploader = getCurrentUser();

        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .uploader(uploader)
                .originalName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .fileData(getBytes(file))
                .uploadedAt(LocalDateTime.now())
                .build();

        TaskAttachment saved = attachmentRepository.save(attachment);
        return toDTO(saved);
    }

    public List<TaskAttachmentDTO> listAttachments(Long taskId) {
        // Ensure user can see the task first
        findTaskForCurrentUser(taskId);

        return attachmentRepository.findByTaskId(taskId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TaskAttachment getAttachment(Long attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        // Verify access
        findTaskForCurrentUser(attachment.getTask().getId());
        return attachment;
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        User current = getCurrentUser();
        boolean isUploader = attachment.getUploader().getId().equals(current.getId());
        boolean isAdminOrManager = current.getRole().name().equals("ADMIN") || current.getRole().name().equals("MANAGER");

        if (!isUploader && !isAdminOrManager) {
            throw new ForbiddenException("Not allowed to delete this attachment");
        }

        attachmentRepository.delete(attachment);
    }

    // helpers

    private Task findTaskForCurrentUser(Long taskId) {
        User user = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Admin can access everything
        if (user.getRole().name().equals("ADMIN")) {
            return task;
        }

        boolean isOwner = task.getUser().getId().equals(user.getId());
        boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId().equals(user.getId()));
        boolean isTeamMember = task.getTeam() != null &&
                teamsService.getTeamsForCurrentUser().stream()
                        .anyMatch(team -> team.getId().equals(task.getTeam().getId()));

        if (isOwner || isAssignee || isTeamMember) {
            return task;
        }

        throw new ForbiddenException("You do not have access to this task");
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("User is not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private TaskAttachmentDTO toDTO(TaskAttachment attachment) {
        return TaskAttachmentDTO.builder()
                .id(attachment.getId())
                .taskId(attachment.getTask().getId())
                .uploaderId(attachment.getUploader().getId())
                .originalName(attachment.getOriginalName())
                .mimeType(attachment.getMimeType())
                .fileSizeBytes(attachment.getFileSizeBytes())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to read uploaded file");
        }
    }
}
