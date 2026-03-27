package com.schoolms.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class StudentDtos {
    public record StudentRequest(@NotBlank String firstName, @NotBlank String lastName, @NotBlank String admissionNumber,
                                 String gender, LocalDate dateOfBirth, String guardianName, String guardianContact,
                                 String status, @NotNull Long classId) {}
    public record StudentResponse(Long id, String fullName, String admissionNumber, String className, String guardianContact, String status) {}
}
