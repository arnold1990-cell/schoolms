package com.schoolms.config;

import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSeederIntegrationTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void seederLinksTeacherAccountAndPopulatesRequiredTeacherFields() {
        User teacherUser = userRepository.findByEmail("teacher@schoolms.com").orElseThrow();
        Teacher teacher = teacherRepository.findByUserId(teacherUser.getId()).orElseThrow();

        assertThat(teacher.getEmployeeNumber()).isNotBlank();
        assertThat(teacher.getFirstName()).isNotBlank();
        assertThat(teacher.getLastName()).isNotBlank();
        assertThat(teacher.getTitle()).isNotNull();
        assertThat(teacher.getGender()).isNotNull();
        assertThat(teacher.getPhoneNumber()).isNotBlank();
        assertThat(teacher.getEmail()).isEqualTo("teacher@schoolms.com");
        assertThat(teacher.getDepartment()).isNotBlank();
        assertThat(teacher.getSpecialization()).isNotBlank();
        assertThat(teacher.getEmploymentType()).isNotNull();
        assertThat(teacher.getHireDate()).isNotNull();
        assertThat(teacher.getStatus()).isNotNull();
        assertThat(teacher.getAddress()).isNotBlank();
    }

    @Test
    void runningSeederAgainDoesNotCreateDuplicateTeacherProfileForDemoAccount() {
        User teacherUser = userRepository.findByEmail("teacher@schoolms.com").orElseThrow();

        dataSeeder.run();

        List<Teacher> teacherProfiles = teacherRepository.findAll().stream()
                .filter(teacher -> teacher.getUser() != null && Objects.equals(teacher.getUser().getId(), teacherUser.getId()))
                .toList();

        assertThat(teacherProfiles).hasSize(1);
    }
}
