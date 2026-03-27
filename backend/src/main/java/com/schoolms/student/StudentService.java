package com.schoolms.student;

import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.common.AppException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;

    public List<StudentDtos.StudentResponse> list(String q) {
        List<Student> students = (q == null || q.isBlank()) ? studentRepository.findAll() :
                studentRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q);
        return students.stream().map(this::map).toList();
    }

    public StudentDtos.StudentResponse create(StudentDtos.StudentRequest request) {
        var schoolClass = classRepository.findById(request.classId()).orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
        Student s = new Student();
        s.setFirstName(request.firstName()); s.setLastName(request.lastName()); s.setAdmissionNumber(request.admissionNumber());
        s.setGender(request.gender()); s.setDateOfBirth(request.dateOfBirth()); s.setGuardianName(request.guardianName());
        s.setGuardianContact(request.guardianContact()); s.setStatus(request.status()); s.setSchoolClass(schoolClass);
        return map(studentRepository.save(s));
    }

    public StudentDtos.StudentResponse update(Long id, StudentDtos.StudentRequest request) {
        Student s = studentRepository.findById(id).orElseThrow(() -> new AppException("Student not found", HttpStatus.NOT_FOUND));
        var schoolClass = classRepository.findById(request.classId()).orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
        s.setFirstName(request.firstName()); s.setLastName(request.lastName()); s.setAdmissionNumber(request.admissionNumber());
        s.setGender(request.gender()); s.setDateOfBirth(request.dateOfBirth()); s.setGuardianName(request.guardianName());
        s.setGuardianContact(request.guardianContact()); s.setStatus(request.status()); s.setSchoolClass(schoolClass);
        return map(studentRepository.save(s));
    }

    private StudentDtos.StudentResponse map(Student s) {
        return new StudentDtos.StudentResponse(s.getId(), s.getFirstName() + " " + s.getLastName(), s.getAdmissionNumber(),
                s.getSchoolClass() != null ? s.getSchoolClass().getName() : null, s.getGuardianContact(), s.getStatus());
    }
}
