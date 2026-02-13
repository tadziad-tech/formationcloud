package com.formationcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoResponseDto {
    /** Max 5 */
    private List<UpcomingSeanceDto> upcomingSeances;
    /** Max 10 */
    private List<TodoItemDto> todo;
    /** Max 10 */
    private List<TodoItemDto> overdue;
}
