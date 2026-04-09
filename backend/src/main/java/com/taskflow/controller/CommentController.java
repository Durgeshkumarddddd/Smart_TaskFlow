package com.taskflow.controller;

import com.taskflow.dto.CommentDTO;
import com.taskflow.dto.CommentRequest;
import com.taskflow.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;


    //  GET /api/tasks/{taskId}/comments
     // TC-C05: View all comments in chronological order.
    
    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getCommentsByTaskId(taskId));
    }

    
    //  POST /api/tasks/{taskId}/comments
    //   TC-C01: Post valid comment → 201 Created.
     
    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request) {
        CommentDTO comment = commentService.addComment(taskId, request.getBody());
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }


    //  DELETE /api/comments/{commentId}
     //  TC-C03: Delete own comment → 204. TC-C04: Others → 403.
     
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
