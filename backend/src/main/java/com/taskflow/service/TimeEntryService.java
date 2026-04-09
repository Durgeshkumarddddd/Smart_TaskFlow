package com.taskflow.service;

import com.taskflow.dto.ActiveTimerDTO;
import com.taskflow.dto.TimeEntryDTO;
import com.taskflow.dto.TimeEntryRequest;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.ActiveTimer;
import com.taskflow.model.Task;
import com.taskflow.model.TimeEntry;
import com.taskflow.model.User;
import com.taskflow.repository.ActiveTimerRepository;
import com.taskflow.repository.TimeEntryRepository;
import com.taskflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimeEntryService {

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private ActiveTimerRepository activeTimerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TeamsService teamsService;

    @Autowired
    private UserService userService;

    @Transactional
    public TimeEntryDTO logTime(Long taskId, TimeEntryRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User current = userService.getCurrentUser();
        ensureTimeLoggingAllowed(task, current);

        TimeEntry entry = TimeEntry.builder()
                .task(task)
                .user(current)
                .minutes(request.getMinutes())
                .logDate(request.getLogDate())
                .isManual(true)
                .description(request.getNote())
                .build();
        TimeEntry saved = timeEntryRepository.save(entry);
        return toDTO(saved);
    }

    public List<TimeEntryDTO> listByTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User current = userService.getCurrentUser();
        ensureTaskAccess(task, current);

        return timeEntryRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public int getTotalMinutes(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User current = userService.getCurrentUser();
        ensureTaskAccess(task, current);

        return timeEntryRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .mapToInt(TimeEntry::getMinutes)
                .sum();
    }

    public ActiveTimerDTO getActiveTimer(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User current = userService.getCurrentUser();
        ensureTaskAccess(task, current);

        return activeTimerRepository.findByTaskIdAndUserId(taskId, current.getId())
                .map(timer -> {
                    long elapsed = Duration.between(timer.getStartTime(), LocalDateTime.now()).getSeconds();
                    return ActiveTimerDTO.builder()
                            .id(timer.getId())
                            .taskId(taskId)
                            .userId(current.getId())
                            .username(current.getUsername())
                            .startTime(timer.getStartTime())
                            .elapsedSeconds(elapsed)
                            .build();
                })
                .orElse(null);
    }

    @Transactional
    public TimeEntryDTO startTimer(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        User current = userService.getCurrentUser();
        ensureTimeLoggingAllowed(task, current);

        // Ensure no active timer already exists for this user and task
        if (activeTimerRepository.findByTaskIdAndUserId(taskId, current.getId()).isPresent()) {
            throw new com.taskflow.exception.ConflictException("A timer is already running for this task");
        }

        ActiveTimer timer = ActiveTimer.builder()
                .task(task)
                .user(current)
                .startTime(LocalDateTime.now())
                .build();
        ActiveTimer saved = activeTimerRepository.save(timer);

        return TimeEntryDTO.builder()
                .id(saved.getId())
                .taskId(taskId)
                .userId(current.getId())
                .username(current.getUsername())
                .minutes(0)
                .logDate(LocalDate.now())
                .isManual(false)
                .description("Timer started")
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public TimeEntryDTO stopTimer(Long taskId) {
        User current = userService.getCurrentUser();
        ActiveTimer timer = activeTimerRepository.findByTaskIdAndUserId(taskId, current.getId())
                .orElseThrow(() -> new BadRequestException("No active timer found for this task"));

        ensureTimeLoggingAllowed(timer.getTask(), current);

        Duration duration = Duration.between(timer.getStartTime(), LocalDateTime.now());
        int minutes = (int) Math.max(1, duration.toMinutes());

        TimeEntry entry = TimeEntry.builder()
                .task(timer.getTask())
                .user(current)
                .minutes(minutes)
                .logDate(LocalDate.now())
                .isManual(false)
                .description("Timer session")
                .build();
        TimeEntry saved = timeEntryRepository.save(entry);

        activeTimerRepository.delete(timer);

        return toDTO(saved);
    }

    @Transactional
    public void stopActiveTimersForTask(Task task) {
        List<ActiveTimer> timers = activeTimerRepository.findByTaskId(task.getId());
        for (ActiveTimer timer : timers) {
            Duration duration = Duration.between(timer.getStartTime(), LocalDateTime.now());
            int minutes = (int) Math.max(1, duration.toMinutes());
            TimeEntry entry = TimeEntry.builder()
                    .task(timer.getTask())
                    .user(timer.getUser())
                    .minutes(minutes)
                    .logDate(LocalDate.now())
                    .isManual(false)
                    .description("Auto-stopped due to task completion")
                    .build();
            timeEntryRepository.save(entry);
            activeTimerRepository.delete(timer);
        }
    }

    @Transactional
    public void deleteTimeEntry(Long id) {
        User current = userService.getCurrentUser();
        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time entry not found"));

        if (!entry.getIsManual()) {
            throw new BadRequestException("Cannot delete timer-generated entries");
        }

        if (!entry.getUser().getId().equals(current.getId()) && current.getRole() != com.taskflow.model.Role.ADMIN) {
            throw new ForbiddenException("Cannot delete another user's time entry");
        }

        timeEntryRepository.delete(entry);
    }

    public List<TimeEntryDTO> listByUser(Long userId) {
        User current = userService.getCurrentUser();
        if (!current.getId().equals(userId) && current.getRole() != com.taskflow.model.Role.ADMIN) {
            throw new ForbiddenException("Cannot view other user's time entries");
        }
        return timeEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private void ensureTaskAccess(Task task, User user) {
        if (user.getRole() == com.taskflow.model.Role.ADMIN)
            return;

        boolean isOwner = task.getUser().getId().equals(user.getId());
        boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId().equals(user.getId()));
        boolean isTeamMember = task.getTeam() != null &&
                teamsService.getTeamsForCurrentUser().stream()
                        .anyMatch(t -> t.getId().equals(task.getTeam().getId()));
        boolean isTeamManager = task.getTeam() != null &&
                task.getTeam().getManager() != null &&
                task.getTeam().getManager().getId().equals(user.getId());
        if (!(isOwner || isAssignee || isTeamMember || isTeamManager)) {
            throw new ForbiddenException("You do not have access to this task");
        }
    }

    private void ensureTimeLoggingAllowed(Task task, User user) {
        ensureTaskAccess(task, user);
        if (user.getRole() == com.taskflow.model.Role.VIEWER) {
            throw new ForbiddenException("Viewer role users cannot log time");
        }
    }

    private TimeEntryDTO toDTO(TimeEntry entry) {
        return TimeEntryDTO.builder()
                .id(entry.getId())
                .taskId(entry.getTask().getId())
                .userId(entry.getUser().getId())
                .username(entry.getUser().getUsername())
                .minutes(entry.getMinutes())
                .logDate(entry.getLogDate())
                .isManual(entry.getIsManual())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
