package com.schoolms.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class TeacherDtos {
    public record CreateTeacherRequest(@NotBlank String firstName, @NotBlank String lastName, @NotBlank String staffCode,
                                       String phone, @Email String email, @NotBlank String password) {}
    public record TeacherResponse(Long id, String firstName, String lastName, String staffCode, String phone, String email, boolean enabled) {}
}
