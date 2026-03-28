package com.schoolms.subject;

public record SubjectResponse(
        Long id,
        String code,
        String name,
        Long assignedTeacherId
) {}
