package com.schoolms.student;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.classmanagement.SchoolClassStatus;
import com.schoolms.common.AppException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;

    @Transactional(readOnly = true)
    public List<StudentDtos.StudentResponse> list(String keyword, String grade, Long classId, StudentStatus status) {
        return studentRepository.search(keyword, grade, classId, status).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public StudentDtos.StudentResponse getById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));
        return map(student);
    }

    @Transactional
    public StudentDtos.StudentResponse create(StudentDtos.StudentRequest request) {
        String admissionNumber = normalizeRequired(request.admissionNumber(), "Admission number is required");
        validateAdmissionNumber(admissionNumber, null);
        SchoolClass schoolClass = findClass(request.classId());
        validateBusinessRules(request, schoolClass);

        Student student = new Student();
        copyFromRequest(student, request, schoolClass);
        return map(studentRepository.save(student));
    }

    @Transactional
    public StudentDtos.StudentResponse update(Long id, StudentDtos.StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));
        String admissionNumber = normalizeRequired(request.admissionNumber(), "Admission number is required");
        validateAdmissionNumber(admissionNumber, id);
        SchoolClass schoolClass = findClass(request.classId());
        validateBusinessRules(request, schoolClass);

        copyFromRequest(student, request, schoolClass);
        return map(studentRepository.save(student));
    }

    @Transactional
    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new AppException("Student not found", HttpStatus.NOT_FOUND);
        }
        studentRepository.deleteById(id);
    }

    private void validateAdmissionNumber(String admissionNumber, Long id) {
        boolean exists = id == null
                ? studentRepository.existsByAdmissionNumberIgnoreCase(admissionNumber)
                : studentRepository.existsByAdmissionNumberIgnoreCaseAndIdNot(admissionNumber, id);
        if (exists) {
            throw new AppException("Admission number already exists", HttpStatus.CONFLICT);
        }
    }

    private SchoolClass findClass(Long classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
        if (schoolClass.getStatus() == SchoolClassStatus.INACTIVE) {
            throw new AppException("Class is inactive and cannot accept learners", HttpStatus.BAD_REQUEST);
        }
        return schoolClass;
    }

    private void validateBusinessRules(StudentDtos.StudentRequest request, SchoolClass schoolClass) {
        LocalDate today = LocalDate.now();
        if (request.dateOfBirth() != null && !request.dateOfBirth().isBefore(today)) {
            throw new AppException("Date of birth must be in the past", HttpStatus.BAD_REQUEST);
        }
        if (request.enrollmentDate() != null && request.enrollmentDate().isAfter(today)) {
            throw new AppException("Enrollment date cannot be in the future", HttpStatus.BAD_REQUEST);
        }
        if (request.dateOfBirth() != null && request.enrollmentDate() != null
                && request.enrollmentDate().isBefore(request.dateOfBirth())) {
            throw new AppException("Enrollment date cannot be before date of birth", HttpStatus.BAD_REQUEST);
        }
        String normalizedGrade = normalizeRequired(request.grade(), "Grade is required");
        String classLevel = schoolClass.getLevel() == null ? "" : schoolClass.getLevel().trim();
        if (!normalizedGrade.equalsIgnoreCase(classLevel)) {
            throw new AppException("Selected class does not belong to the specified grade", HttpStatus.BAD_REQUEST);
        }
    }

    private void copyFromRequest(Student student, StudentDtos.StudentRequest request, SchoolClass schoolClass) {
        student.setAdmissionNumber(normalizeRequired(request.admissionNumber(), "Admission number is required"));
        student.setFirstName(normalizeRequired(request.firstName(), "First name is required"));
        student.setMiddleName(trimToNull(request.middleName()));
        student.setLastName(normalizeRequired(request.lastName(), "Last name is required"));
        student.setPreferredName(trimToNull(request.preferredName()));
        student.setGender(normalizeRequired(request.gender(), "Gender is required"));
        student.setDateOfBirth(request.dateOfBirth());
        student.setGrade(normalizeRequired(request.grade(), "Grade is required"));
        student.setEnrollmentDate(request.enrollmentDate());
        student.setGuardianName(normalizeRequired(request.guardianName(), "Guardian name is required"));
        student.setGuardianRelationship(normalizeRequired(request.guardianRelationship(), "Guardian relationship is required"));
        student.setGuardianPhone(normalizeRequired(request.guardianPhone(), "Guardian phone is required"));

        String normalizedAddress = trimToNull(request.address());
        student.setAddress(normalizedAddress);
        student.setAddressLine1(trimToNull(request.addressLine1()) != null ? trimToNull(request.addressLine1()) : normalizedAddress);
        student.setAddressLine2(trimToNull(request.addressLine2()));

        student.setStatus(request.status());
        student.setNationality(trimToNull(request.nationality()));
        student.setNationalId(trimToNull(request.nationalId()));
        student.setPassportNumber(trimToNull(request.passportNumber()));
        student.setPreviousSchool(trimToNull(request.previousSchool()));
        student.setPhoneNumber(trimToNull(request.phoneNumber()));
        student.setAlternativePhoneNumber(trimToNull(request.alternativePhoneNumber()));
        student.setEmail(trimToNull(request.email()));
        student.setCity(trimToNull(request.city()));
        student.setDistrict(trimToNull(request.district()));
        student.setPostalCode(trimToNull(request.postalCode()));
        student.setCountry(trimToNull(request.country()));
        student.setGuardianAltPhone(trimToNull(request.guardianAltPhone()));
        student.setGuardianEmail(trimToNull(request.guardianEmail()));
        student.setGuardianOccupation(trimToNull(request.guardianOccupation()));
        student.setGuardianAddress(trimToNull(request.guardianAddress()));
        student.setEmergencyContactName(trimToNull(request.emergencyContactName()));
        student.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));
        student.setEmergencyContactRelationship(trimToNull(request.emergencyContactRelationship()));
        student.setBloodGroup(trimToNull(request.bloodGroup()));
        student.setAllergies(trimToNull(request.allergies()));
        student.setMedicalConditions(trimToNull(request.medicalConditions()));
        student.setDisabilities(trimToNull(request.disabilities()));
        student.setMedication(trimToNull(request.medication()));
        student.setHospitalName(trimToNull(request.hospitalName()));
        student.setDoctorName(trimToNull(request.doctorName()));
        student.setDoctorPhone(trimToNull(request.doctorPhone()));
        student.setUsesTransport(request.usesTransport() != null ? request.usesTransport() : Boolean.FALSE);
        student.setPickupPoint(trimToNull(request.pickupPoint()));
        student.setRouteName(trimToNull(request.routeName()));
        student.setDriverAssignment(trimToNull(request.driverAssignment()));
        student.setReligion(trimToNull(request.religion()));
        student.setHomeLanguage(trimToNull(request.homeLanguage()));
        student.setResidencyType(trimToNull(request.residencyType()));
        student.setSponsorshipStatus(trimToNull(request.sponsorshipStatus()));
        student.setFeeCategory(trimToNull(request.feeCategory()));
        student.setNotes(trimToNull(request.notes()));
        student.setSchoolClass(schoolClass);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private StudentDtos.StudentResponse map(Student s) {
        SchoolClass c = s.getSchoolClass();
        StringBuilder fullNameBuilder = new StringBuilder();
        if (s.getFirstName() != null && !s.getFirstName().isBlank()) {
            fullNameBuilder.append(s.getFirstName().trim());
        }
        if (s.getMiddleName() != null && !s.getMiddleName().isBlank()) {
            if (!fullNameBuilder.isEmpty()) {
                fullNameBuilder.append(' ');
            }
            fullNameBuilder.append(s.getMiddleName().trim());
        }
        if (s.getLastName() != null && !s.getLastName().isBlank()) {
            if (!fullNameBuilder.isEmpty()) {
                fullNameBuilder.append(' ');
            }
            fullNameBuilder.append(s.getLastName().trim());
        }
        String fullName = fullNameBuilder.toString();

        return new StudentDtos.StudentResponse(
                s.getId(),
                s.getAdmissionNumber(),
                s.getFirstName(),
                s.getMiddleName(),
                s.getLastName(),
                fullName,
                s.getPreferredName(),
                s.getGender(),
                s.getDateOfBirth(),
                s.getGrade(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                c != null ? c.getStream() : null,
                s.getEnrollmentDate(),
                s.getGuardianName(),
                s.getGuardianRelationship(),
                s.getGuardianPhone(),
                s.getAddress(),
                s.getStatus(),
                s.getNationality(),
                s.getNationalId(),
                s.getPassportNumber(),
                s.getPreviousSchool(),
                s.getPhoneNumber(),
                s.getAlternativePhoneNumber(),
                s.getEmail(),
                s.getAddressLine1(),
                s.getAddressLine2(),
                s.getCity(),
                s.getDistrict(),
                s.getPostalCode(),
                s.getCountry(),
                s.getGuardianAltPhone(),
                s.getGuardianEmail(),
                s.getGuardianOccupation(),
                s.getGuardianAddress(),
                s.getEmergencyContactName(),
                s.getEmergencyContactPhone(),
                s.getEmergencyContactRelationship(),
                s.getBloodGroup(),
                s.getAllergies(),
                s.getMedicalConditions(),
                s.getDisabilities(),
                s.getMedication(),
                s.getHospitalName(),
                s.getDoctorName(),
                s.getDoctorPhone(),
                s.getUsesTransport(),
                s.getPickupPoint(),
                s.getRouteName(),
                s.getDriverAssignment(),
                s.getReligion(),
                s.getHomeLanguage(),
                s.getResidencyType(),
                s.getSponsorshipStatus(),
                s.getFeeCategory(),
                s.getNotes()
        );
    }
}
