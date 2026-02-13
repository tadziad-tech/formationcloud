package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoItemDto {
    private String type;
    private String title;
    private String message;
    private String link;
    /** INFO, WARN, URGENT */
    private String severity;
}
