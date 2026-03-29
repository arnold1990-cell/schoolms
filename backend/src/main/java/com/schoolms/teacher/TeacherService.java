package com.schoolms.teacher;

import com.schoolms.common.AppException;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TeacherDtos.TeacherResponse> list() {
        return teacherRepository.findAll().stream().map(this::map).toList();
    }

    public List<TeacherDtos.TeacherResponse> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            return list();
        }
        return teacherRepository.search(normalized).stream().map(this::map).toList();
    }

    public TeacherDtos.TeacherResponse get(Long id) {
        return map(getTeacher(id));
    }

    @Transactional
    public TeacherDtos.TeacherResponse create(TeacherDtos.UpsertTeacherRequest request) {
        validateDuplicates(request, null);

        User savedUser = resolveUserForCreate(request);

        Teacher teacher = new Teacher();
        teacher.setUser(savedUser);
        applyUpsert(request, teacher);
        return map(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherDtos.TeacherResponse update(Long id, TeacherDtos.UpsertTeacherRequest request) {
        Teacher teacher = getTeacher(id);
        validateDuplicates(request, teacher.getId());

        applyUpsert(request, teacher);

        teacher.getUser().setEmail(normalize(request.email()));
        if (request.password() != null && !request.password().isBlank()) {
            teacher.getUser().setPassword(passwordEncoder.encode(request.password().trim()));
        }
        if (request.enabled() != null) {
            teacher.getUser().setEnabled(request.enabled());
        }
        userRepository.save(teacher.getUser());
        return map(teacherRepository.save(teacher));
    }

    @Transactional
    public void deleteOrDeactivate(Long id) {
        Teacher teacher = getTeacher(id);
        teacher.getUser().setEnabled(false);
        teacher.setStatus(TeacherStatus.INACTIVE);
        userRepository.save(teacher.getUser());
        teacherRepository.save(teacher);
    }

    public TeacherDtos.TeacherResponse setStatus(Long id, boolean enabled) {
        Teacher teacher = getTeacher(id);
        teacher.getUser().setEnabled(enabled);
        if (!enabled) {
            teacher.setStatus(TeacherStatus.INACTIVE);
        } else if (teacher.getStatus() == TeacherStatus.INACTIVE) {
            teacher.setStatus(TeacherStatus.ACTIVE);
        }
        userRepository.save(teacher.getUser());
        return map(teacherRepository.save(teacher));
    }

    private Teacher getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new AppException("Teacher not found", HttpStatus.NOT_FOUND));
    }

    private void validateDuplicates(TeacherDtos.UpsertTeacherRequest request, Long id) {
        String employeeNumber = normalize(request.employeeNumber());
        if (id == null ? teacherRepository.existsByEmployeeNumberIgnoreCase(employeeNumber)
                : teacherRepository.existsByEmployeeNumberIgnoreCaseAndIdNot(employeeNumber, id)) {
            throw new AppException("Employee number already exists", HttpStatus.CONFLICT);
        }

        String email = normalize(request.email());
        if (id == null ? teacherRepository.existsByEmailIgnoreCase(email)
                : teacherRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new AppException("Teacher email already exists", HttpStatus.CONFLICT);
        }

        Long linkedUserId = id == null ? null : getTeacher(id).getUser().getId();
        if (linkedUserId == null) {
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !canLinkExistingUser(existingUser.get())) {
                throw new AppException("User email already exists", HttpStatus.CONFLICT);
            }
        } else if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, linkedUserId)) {
            throw new AppException("User email already exists", HttpStatus.CONFLICT);
        }

        String nationalId = normalizeNullable(request.nationalId());
        if (nationalId != null && (id == null ? teacherRepository.existsByNationalIdIgnoreCase(nationalId)
                : teacherRepository.existsByNationalIdIgnoreCaseAndIdNot(nationalId, id))) {
            throw new AppException("National ID already exists", HttpStatus.CONFLICT);
        }
    }

    private void applyUpsert(TeacherDtos.UpsertTeacherRequest request, Teacher teacher) {
        teacher.setEmployeeNumber(normalize(request.employeeNumber()));
        teacher.setFirstName(normalize(request.firstName()));
        teacher.setMiddleName(normalizeNullable(request.middleName()));
        teacher.setLastName(normalize(request.lastName()));
        teacher.setTitle(request.title() == null ? TeacherTitle.MR : request.title());
        teacher.setGender(request.gender());
        teacher.setDateOfBirth(request.dateOfBirth());
        teacher.setPhoneNumber(normalize(request.phoneNumber()));
        teacher.setAlternativePhoneNumber(normalizeNullable(request.alternativePhoneNumber()));
        teacher.setEmail(normalize(request.email()));
        teacher.setNationalId(normalizeNullable(request.nationalId()));
        teacher.setPassportNumber(normalizeNullable(request.passportNumber()));
        teacher.setDepartment(normalize(request.department()));
        teacher.setSpecialization(normalize(request.specialization()));
        teacher.setEmploymentType(request.employmentType());
        teacher.setHireDate(request.hireDate());
        teacher.setStatus(request.status());
        teacher.setAddress(normalize(request.address()));
        teacher.setEmergencyContactName(normalizeNullable(request.emergencyContactName()));
        teacher.setEmergencyContactPhone(normalizeNullable(request.emergencyContactPhone()));
        teacher.setEmergencyContactRelationship(normalizeNullable(request.emergencyContactRelationship()));
        teacher.setQualification(normalizeNullable(request.qualification()));
        teacher.setHighestEducationLevel(normalizeNullable(request.highestEducationLevel()));
        teacher.setYearsOfExperience(request.yearsOfExperience());
        teacher.setStaffRole(normalizeNullable(request.staffRole()));
        teacher.setSalaryGrade(normalizeNullable(request.salaryGrade()));
        teacher.setNotes(normalizeNullable(request.notes()));
        teacher.setProfilePhotoUrl(normalizeNullable(request.profilePhotoUrl()));
    }

    private TeacherDtos.TeacherResponse map(Teacher teacher) {
        String fullName = String.join(" ", List.of(
                safe(teacher.getFirstName()),
                safe(teacher.getMiddleName()),
                safe(teacher.getLastName())
        )).trim().replaceAll("\\s+", " ");

        return new TeacherDtos.TeacherResponse(
                teacher.getId(),
                teacher.getEmployeeNumber(),
                teacher.getEmployeeNumber(),
                teacher.getFirstName(),
                teacher.getMiddleName(),
                teacher.getLastName(),
                fullName,
                teacher.getGender(),
                teacher.getDateOfBirth(),
                teacher.getPhoneNumber(),
                teacher.getPhoneNumber(),
                teacher.getAlternativePhoneNumber(),
                teacher.getEmail(),
                teacher.getNationalId(),
                teacher.getPassportNumber(),
                teacher.getDepartment(),
                teacher.getSpecialization(),
                teacher.getEmploymentType(),
                teacher.getHireDate(),
                teacher.getStatus(),
                teacher.getAddress(),
                teacher.getTitle(),
                teacher.getEmergencyContactName(),
                teacher.getEmergencyContactPhone(),
                teacher.getEmergencyContactRelationship(),
                teacher.getQualification(),
                teacher.getHighestEducationLevel(),
                teacher.getYearsOfExperience(),
                teacher.getStaffRole(),
                teacher.getSalaryGrade(),
                teacher.getNotes(),
                teacher.getProfilePhotoUrl(),
                teacher.getUser().isEnabled()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolvePassword(String password) {
        String normalized = normalizeNullable(password);
        return normalized == null ? "Teacher123!" : normalized;
    }

    private User resolveUserForCreate(TeacherDtos.UpsertTeacherRequest request) {
        String email = normalize(request.email());
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!canLinkExistingUser(user)) {
                throw new AppException("User email already exists", HttpStatus.CONFLICT);
            }
            user.setRole(Role.TEACHER);
            if (request.password() != null && !request.password().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.password().trim()));
            }
            if (request.enabled() != null) {
                user.setEnabled(request.enabled());
            }
            return userRepository.save(user);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(resolvePassword(request.password())));
        user.setRole(Role.TEACHER);
        user.setEnabled(request.enabled() == null || request.enabled());
        return userRepository.save(user);
    }

    private boolean canLinkExistingUser(User user) {
        if (user.getRole() != Role.TEACHER) {
            return false;
        }
        return teacherRepository.findByUserId(user.getId()).isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
