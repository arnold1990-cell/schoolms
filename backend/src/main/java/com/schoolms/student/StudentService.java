package com.schoolms.student;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.common.AppException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        String admissionNumber = normalizeRequired(request.admissionNumber(), "Required field missing: admissionNumber");
        ensureAdmissionNumberUnique(admissionNumber, null);

        Student student = new Student();
        applyCreateOrUpdate(student, request, request.schoolClassId());
        student.setAdmissionNumber(admissionNumber);

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));

        String admissionNumber = normalizeRequired(request.admissionNumber(), "Required field missing: admissionNumber");
        ensureAdmissionNumberUnique(admissionNumber, id);

        applyCreateOrUpdate(student, request, request.schoolClassId());
        student.setAdmissionNumber(admissionNumber);

        return toResponse(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void confirmAndDeleteStudent(Long id, StudentDeleteConfirmationRequest confirmation) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));

        boolean matches = normalizeRequired(confirmation.firstName(), "First name is required").equals(student.getFirstName())
                && normalizeRequired(confirmation.lastName(), "Last name is required").equals(student.getLastName())
                && normalizeRequired(confirmation.admissionNumber(), "Admission number is required").equals(student.getAdmissionNumber())
                && normalizeRequired(confirmation.gender(), "Gender is required").equals(student.getGender())
                && normalizeRequired(confirmation.grade(), "Grade is required").equals(student.getGrade())
                && confirmation.enrollmentDate().equals(student.getEnrollmentDate())
                && confirmation.status() == student.getStatus();

        if (!matches) {
            throw new AppException("Student delete confirmation failed. Core student details do not match.", HttpStatus.BAD_REQUEST);
        }

        studentRepository.delete(student);
    }

    private void applyCreateOrUpdate(Student student, StudentCreateRequest request, Long schoolClassId) {
        student.setFirstName(normalizeRequired(request.firstName(), "Required field missing: firstName"));
        student.setMiddleName(trimToNull(request.middleName()));
        student.setLastName(normalizeRequired(request.lastName(), "Required field missing: lastName"));
        student.setPreferredName(trimToNull(request.preferredName()));
        student.setGender(normalizeRequired(request.gender(), "Required field missing: gender"));
        student.setDateOfBirth(requireValue(request.dateOfBirth(), "Required field missing: dateOfBirth"));
        student.setGrade(normalizeRequired(request.grade(), "Required field missing: grade"));
        student.setEnrollmentDate(requireValue(request.enrollmentDate(), "Required field missing: enrollmentDate"));
        student.setGuardianName(normalizeRequired(request.guardianName(), "Required field missing: guardianName"));
        student.setGuardianRelationship(normalizeRequired(request.guardianRelationship(), "Required field missing: guardianRelationship"));
        student.setGuardianPhone(normalizeRequired(request.guardianPhone(), "Required field missing: guardianPhone"));
        student.setAddress(normalizeRequired(request.address(), "Required field missing: address"));
        student.setStatus(requireValue(request.status(), "Required field missing: status"));
        student.setNationality(trimToNull(request.nationality()));
        student.setNationalId(trimToNull(request.nationalId()));
        student.setPassportNumber(trimToNull(request.passportNumber()));
        student.setPreviousSchool(trimToNull(request.previousSchool()));
        student.setPhoneNumber(trimToNull(request.phoneNumber()));
        student.setAlternativePhoneNumber(trimToNull(request.alternativePhoneNumber()));
        student.setEmail(trimToNull(request.email()));
        student.setAddressLine1(trimToNull(request.addressLine1()));
        student.setAddressLine2(trimToNull(request.addressLine2()));
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
        student.setUsesTransport(request.usesTransport());
        student.setPickupPoint(trimToNull(request.pickupPoint()));
        student.setRouteName(trimToNull(request.routeName()));
        student.setDriverAssignment(trimToNull(request.driverAssignment()));
        student.setReligion(trimToNull(request.religion()));
        student.setHomeLanguage(trimToNull(request.homeLanguage()));
        student.setResidencyType(trimToNull(request.residencyType()));
        student.setSponsorshipStatus(trimToNull(request.sponsorshipStatus()));
        student.setFeeCategory(trimToNull(request.feeCategory()));
        student.setNotes(trimToNull(request.notes()));
        student.setSchoolClass(resolveSchoolClass(requireValue(schoolClassId, "Required field missing: classId")));
    }

    private void applyCreateOrUpdate(Student student, StudentUpdateRequest request, Long schoolClassId) {
        applyCreateOrUpdate(student, new StudentCreateRequest(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.preferredName(),
                request.admissionNumber(),
                request.gender(),
                request.dateOfBirth(),
                request.grade(),
                request.enrollmentDate(),
                request.guardianName(),
                request.guardianRelationship(),
                request.guardianPhone(),
                request.address(),
                request.status(),
                request.nationality(),
                request.nationalId(),
                request.passportNumber(),
                request.previousSchool(),
                request.phoneNumber(),
                request.alternativePhoneNumber(),
                request.email(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.district(),
                request.postalCode(),
                request.country(),
                request.guardianAltPhone(),
                request.guardianEmail(),
                request.guardianOccupation(),
                request.guardianAddress(),
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                request.emergencyContactRelationship(),
                request.bloodGroup(),
                request.allergies(),
                request.medicalConditions(),
                request.disabilities(),
                request.medication(),
                request.hospitalName(),
                request.doctorName(),
                request.doctorPhone(),
                request.usesTransport(),
                request.pickupPoint(),
                request.routeName(),
                request.driverAssignment(),
                request.religion(),
                request.homeLanguage(),
                request.residencyType(),
                request.sponsorshipStatus(),
                request.feeCategory(),
                request.notes(),
                request.schoolClassId()
        ), schoolClassId);
    }

    private SchoolClass resolveSchoolClass(Long schoolClassId) {
        return schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new AppException("School class not found", HttpStatus.NOT_FOUND));
    }

    private void ensureAdmissionNumberUnique(String admissionNumber, Long existingId) {
        boolean exists = existingId == null
                ? studentRepository.existsByAdmissionNumberIgnoreCase(admissionNumber)
                : studentRepository.existsByAdmissionNumberIgnoreCaseAndIdNot(admissionNumber, existingId);

        if (exists) {
            throw new AppException("Admission number already exists", HttpStatus.CONFLICT);
        }
    }

    private <T> T requireValue(T value, String message) {
        if (value == null) {
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private StudentResponse toResponse(Student student) {
        SchoolClass schoolClass = student.getSchoolClass();
        String fullName = List.of(student.getFirstName(), student.getMiddleName(), student.getLastName())
                .stream()
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getMiddleName(),
                student.getLastName(),
                fullName,
                student.getPreferredName(),
                student.getAdmissionNumber(),
                student.getGender(),
                student.getDateOfBirth(),
                student.getGrade(),
                student.getEnrollmentDate(),
                student.getGuardianName(),
                student.getGuardianRelationship(),
                student.getGuardianPhone(),
                student.getAddress(),
                student.getStatus(),
                student.getNationality(),
                student.getNationalId(),
                student.getPassportNumber(),
                student.getPreviousSchool(),
                student.getPhoneNumber(),
                student.getAlternativePhoneNumber(),
                student.getEmail(),
                student.getAddressLine1(),
                student.getAddressLine2(),
                student.getCity(),
                student.getDistrict(),
                student.getPostalCode(),
                student.getCountry(),
                student.getGuardianAltPhone(),
                student.getGuardianEmail(),
                student.getGuardianOccupation(),
                student.getGuardianAddress(),
                student.getEmergencyContactName(),
                student.getEmergencyContactPhone(),
                student.getEmergencyContactRelationship(),
                student.getBloodGroup(),
                student.getAllergies(),
                student.getMedicalConditions(),
                student.getDisabilities(),
                student.getMedication(),
                student.getHospitalName(),
                student.getDoctorName(),
                student.getDoctorPhone(),
                student.getUsesTransport(),
                student.getPickupPoint(),
                student.getRouteName(),
                student.getDriverAssignment(),
                student.getReligion(),
                student.getHomeLanguage(),
                student.getResidencyType(),
                student.getSponsorshipStatus(),
                student.getFeeCategory(),
                student.getNotes(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                schoolClass != null ? schoolClass.getStream() : null,
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                schoolClass != null ? schoolClass.getStream() : null
        );
    }
}
