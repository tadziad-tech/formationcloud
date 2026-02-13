package com.formationcloud.platform.controller;

import com.formationcloud.platform.dto.TodoResponseDto;
import com.formationcloud.platform.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/me")
    public ResponseEntity<TodoResponseDto> getForCurrentUser() {
        return ResponseEntity.ok(todoService.getForCurrentUser());
    }
}
