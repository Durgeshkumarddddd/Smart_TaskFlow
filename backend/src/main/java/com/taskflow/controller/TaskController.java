package com.taskflow.controller;

import com.taskflow.dto.TaskDTO;
import com.taskflow.dto.TaskSummaryDTO;
import com.taskflow.model.TaskPriority;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    
      // GET /api/tasks
      //  Optional filters: ?status=, ?priority=, both combined
     
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {

        List<TaskDTO> tasks;
        if (status != null && priority != null) {
            tasks = taskService.getTasksByStatusAndPriority(status, priority);
        } else if (status != null) {
            tasks = taskService.getTasksByStatus(status);
        } else if (priority != null) {
            tasks = taskService.getTasksByPriority(priority);
        } else {
            tasks = taskService.getAllTasksForCurrentUser();
        }
        return ResponseEntity.ok(tasks);
    }

    //  GET /api/tasks/assigned-to-me
     //  F-EXT-02 TC-A04: Only tasks assigned to current user.
     
    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<TaskDTO>> getAssignedToMe() {
                      return ResponseEntity.ok(taskService.getTasksAssignedToMe());
    }

    // GET /api/tasks/summary
    //   F-EXT-04: Dashboard analytics summary.
    
    @GetMapping("/summary")
    public ResponseEntity<TaskSummaryDTO> getTaskSummary() {
               return ResponseEntity.ok(taskService.getTaskSummary());
    }

   
    //  GET /api/tasks/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    //  POST /api/tasks
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO taskDTO) {
        TaskDTO created = taskService.createTask(taskDTO);
              return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

     //  PUT /api/tasks/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskDTO taskDTO) {
        TaskDTO updated = taskService.updateTask(id, taskDTO);
               return ResponseEntity.ok(updated);
    }

// DELETE /api/tasks/{id}
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
    }
}
