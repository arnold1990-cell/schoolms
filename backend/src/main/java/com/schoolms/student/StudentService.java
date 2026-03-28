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
        validateAdmissionNumber(request.admissionNumber(), null);
        SchoolClass schoolClass = findClass(request.classId());

        Student student = new Student();
        copyFromRequest(student, request, schoolClass);
        return map(studentRepository.save(student));
    }

    @Transactional
    public StudentDtos.StudentResponse update(Long id, StudentDtos.StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));
        validateAdmissionNumber(request.admissionNumber(), id);
        SchoolClass schoolClass = findClass(request.classId());

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
        return classRepository.findById(classId)
                .orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
    }

    private void copyFromRequest(Student student, StudentDtos.StudentRequest request, SchoolClass schoolClass) {
        student.setAdmissionNumber(request.admissionNumber().trim());
        student.setFirstName(request.firstName().trim());
        student.setMiddleName(trimToNull(request.middleName()));
        student.setLastName(request.lastName().trim());
        student.setPreferredName(trimToNull(request.preferredName()));
        student.setGender(request.gender().trim());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGrade(request.grade().trim());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setGuardianName(request.guardianName().trim());
        student.setGuardianRelationship(request.guardianRelationship().trim());
        student.setGuardianPhone(request.guardianPhone().trim());

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
