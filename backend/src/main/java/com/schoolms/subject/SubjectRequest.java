package com.schoolms.subject;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank(message = "Code is required") String code,
        @NotBlank(message = "Name is required") String name,
        Long assignedTeacherId
) {}
