package com.schoolms.classmanagement;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SchoolClassDtos {

    public record SchoolClassUpsertRequest(
            @NotBlank(message = "Standard is required")
            @JsonAlias("gradeLevel")
            String standard,
            @NotBlank(message = "Stream / Section is required") String streamSection,
            @NotBlank(message = "Academic Year is required") String academicYear,
            @Min(value = 1, message = "Capacity must be a positive number") Integer capacity,
            SchoolClassStatus status
    ) {
    }

    public record AssignTeacherRequest(Long teacherId) {
    }

    public record SchoolClassSummaryResponse(
            Long id,
            String name,
            String code,
            String level,
            String academicYear,
            String stream,
            Integer capacity,
            SchoolClassStatus status,
            Long classTeacherId,
            String classTeacherName,
            int learnerCount,
            int subjectCount
    ) {
    }

    public record ClassRosterResponse(
            Long id,
            String name,
            String code,
            String level,
            String academicYear,
            String stream,
            Integer capacity,
            SchoolClassStatus status,
            TeacherSummary classTeacher,
            java.util.List<LearnerSummary> learners,
            java.util.List<SubjectSummary> subjects,
            int learnerCount,
            int capacityUsagePercent
    ) {
    }

    public record TeacherSummary(Long id, String employeeNumber, String fullName) {
    }

    public record LearnerSummary(Long id, String admissionNumber, String fullName, StudentStatusSummary status) {
    }

    public record SubjectSummary(Long id, String code, String name) {
    }

    public enum StudentStatusSummary {
        ACTIVE,
        PENDING,
        SUSPENDED,
        TRANSFERRED,
        GRADUATED
    }
}
