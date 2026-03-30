package com.schoolms.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record StudentDeleteConfirmationRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @NotBlank(message = "Admission number is required") String admissionNumber,
        @NotBlank(message = "Gender is required") String gender,
        @NotBlank(message = "Grade is required") String grade,
        @NotNull(message = "Enrollment date is required") LocalDate enrollmentDate,
        @NotNull(message = "Status is required") StudentStatus status
) {
}
