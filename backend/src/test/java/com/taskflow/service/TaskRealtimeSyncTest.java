package com.taskflow.service;

import com.taskflow.dto.TaskDTO;
import com.taskflow.dto.WebsocketNotificationDTO;
import com.taskflow.model.Team;
import com.taskflow.model.User;
import com.taskflow.model.Task;
import com.taskflow.model.Role;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TaskRealtimeSyncTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamsService teamsService;

    @InjectMocks
    private TaskService taskService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void testTaskCreationBroadcastsToTeam() {
        // Setup mock user
        User user = User.builder().id(1L).username("testuser").email("test@test.com").role(Role.ADMIN).build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        // Setup team and task
        Team team = Team.builder().id(10L).name("Alpha").build();
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTitle("Realtime Sync Test");
        taskDTO.setTeamId(10L);
        taskDTO.setAssigneeIds(Collections.emptyList());

        Task savedTask = new Task();
        savedTask.setId(100L);
        savedTask.setTitle(taskDTO.getTitle());
        savedTask.setTeam(team);
        savedTask.setAssignees(Collections.emptySet());
        savedTask.setUser(user);

        // Define repository interactions
        when(teamsService.getTeamById(10L)).thenReturn(team);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Execute
        taskService.createTask(taskDTO);

        // Verify that notifyTeam was called!
        verify(notificationService, times(1)).notifyTeam(eq(10L), any(WebsocketNotificationDTO.class));
        System.out.println("VERIFIED: TaskService.createTask called notifyTeam for Alpha Team.");
    }
}
