package com.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDTO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long actorId;
    private String actorName;
    private String actionCode;
    private String message;
    private LocalDateTime createdAt;
}
