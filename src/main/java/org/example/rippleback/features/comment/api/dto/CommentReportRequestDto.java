package org.example.rippleback.features.comment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.rippleback.features.comment.domain.ReportCategory;

public record CommentReportRequestDto(
        @NotNull
        ReportCategory category,

        @NotBlank
        @Size(max = 255)
        String reason
) {
}
