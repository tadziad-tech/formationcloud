package com.formationcloud.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProgressionRequest {
    @NotNull
    @Min(0)
    @Max(100)
    private Integer progression;
}
