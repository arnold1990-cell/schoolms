package com.schoolms.classmanagement;

import com.schoolms.common.AppException;
import com.schoolms.marks.MarkRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolClassService {
    private final SchoolClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final MarkRepository markRepository;

    @Transactional(readOnly = true)
    public List<SchoolClassDtos.SchoolClassSummaryResponse> list() {
        return classRepository.findAll().stream()
                .sorted(Comparator.comparing(SchoolClass::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolClassDtos.ClassRosterResponse getById(Long id) {
        SchoolClass schoolClass = getClassEntity(id);
        return toRoster(schoolClass);
    }

    @Transactional
    public SchoolClassDtos.SchoolClassSummaryResponse create(SchoolClassDtos.SchoolClassUpsertRequest request) {
        SchoolClass schoolClass = new SchoolClass();
        apply(schoolClass, request, null);
        return toSummary(classRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassDtos.SchoolClassSummaryResponse update(Long id, SchoolClassDtos.SchoolClassUpsertRequest request) {
        SchoolClass schoolClass = getClassEntity(id);
        apply(schoolClass, request, id);
        return toSummary(classRepository.save(schoolClass));
    }

    @Transactional
    public void delete(Long id) {
        SchoolClass schoolClass = getClassEntity(id);
        if (studentRepository.findBySchoolClassId(id).size() > 0) {
            throw new AppException("Cannot delete class because learners are still assigned", HttpStatus.CONFLICT);
        }
        if (!schoolClass.getSubjects().isEmpty()) {
            throw new AppException("Cannot delete class because subjects are still assigned", HttpStatus.CONFLICT);
        }
        if (markRepository.existsByStudentSchoolClassId(id)) {
            throw new AppException("Cannot delete class because marks exist for learners in this class", HttpStatus.CONFLICT);
        }
        classRepository.delete(schoolClass);
    }

    @Transactional
    public SchoolClassDtos.ClassRosterResponse assignTeacher(Long classId, Long teacherId) {
        SchoolClass schoolClass = getClassEntity(classId);
        schoolClass.setClassTeacher(teacherId == null ? null : getTeacher(teacherId));
        return toRoster(classRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassDtos.ClassRosterResponse addLearner(Long classId, Long learnerId) {
        Student student = getStudent(learnerId);
        SchoolClass targetClass = getClassEntity(classId);
        if (student.getSchoolClass() != null && classId.equals(student.getSchoolClass().getId())) {
            throw new AppException("Learner is already assigned to this class", HttpStatus.CONFLICT);
        }
        student.setSchoolClass(targetClass);
        studentRepository.save(student);
        return toRoster(targetClass);
    }

    @Transactional
    public SchoolClassDtos.ClassRosterResponse removeLearner(Long classId, Long learnerId) {
        Student student = getStudent(learnerId);
        if (student.getSchoolClass() == null || !classId.equals(student.getSchoolClass().getId())) {
            throw new AppException("Learner is not assigned to this class", HttpStatus.CONFLICT);
        }
        student.setSchoolClass(null);
        studentRepository.save(student);
        return toRoster(getClassEntity(classId));
    }

    @Transactional
    public SchoolClassDtos.ClassRosterResponse transferLearner(Long classId, Long learnerId, Long targetClassId) {
        if (classId.equals(targetClassId)) {
            throw new AppException("Target class must be different from current class", HttpStatus.BAD_REQUEST);
        }
        Student student = getStudent(learnerId);
        if (student.getSchoolClass() == null || !classId.equals(student.getSchoolClass().getId())) {
            throw new AppException("Learner is not assigned to this class", HttpStatus.CONFLICT);
        }
        SchoolClass targetClass = getClassEntity(targetClassId);
        student.setSchoolClass(targetClass);
        studentRepository.save(student);
        return toRoster(targetClass);
    }

    @Transactional
    public SchoolClassDtos.ClassRosterResponse addSubject(Long classId, Long subjectId) {
        SchoolClass schoolClass = getClassEntity(classId);
        Subject subject = getSubject(subjectId);
        if (schoolClass.getSubjects().stream().anyMatch(item -> item.getId().equals(subjectId))) {
            throw new AppException("Subject is already assigned to this class", HttpStatus.CONFLICT);
        }
        schoolClass.getSubjects().add(subject);
        return toRoster(classRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassDtos.ClassRosterResponse removeSubject(Long classId, Long subjectId) {
        SchoolClass schoolClass = getClassEntity(classId);
        boolean removed = schoolClass.getSubjects().removeIf(item -> item.getId().equals(subjectId));
        if (!removed) {
            throw new AppException("Subject is not assigned to this class", HttpStatus.CONFLICT);
        }
        return toRoster(classRepository.save(schoolClass));
    }

    private void apply(SchoolClass schoolClass, SchoolClassDtos.SchoolClassUpsertRequest request, Long id) {
        String name = normalizeRequired(request.name(), "Class name is required");
        if (id == null ? classRepository.existsByNameIgnoreCase(name) : classRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new AppException("Class name already exists", HttpStatus.CONFLICT);
        }

        String stream = normalizeOptional(request.stream());
        String code = normalizeOptional(request.code());
        if (code == null) {
            code = (name + (stream == null ? "" : "-" + stream)).replaceAll("\\s+", "-").toUpperCase();
        }
        if (id == null ? classRepository.existsByCodeIgnoreCase(code) : classRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new AppException("Class code already exists", HttpStatus.CONFLICT);
        }

        schoolClass.setName(name);
        schoolClass.setCode(code);
        schoolClass.setLevel(normalizeOptional(request.level()));
        schoolClass.setAcademicYear(normalizeOptional(request.academicYear()));
        schoolClass.setStream(stream);
        schoolClass.setCapacity(request.capacity());
        schoolClass.setStatus(request.status() == null ? SchoolClassStatus.ACTIVE : request.status());
    }

    private SchoolClass getClassEntity(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
    }

    private Teacher getTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new AppException("Teacher not found", HttpStatus.NOT_FOUND));
    }

    private Student getStudent(Long learnerId) {
        return studentRepository.findById(learnerId)
                .orElseThrow(() -> new AppException("Learner not found", HttpStatus.NOT_FOUND));
    }

    private Subject getSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException("Subject not found", HttpStatus.NOT_FOUND));
    }

    private SchoolClassDtos.SchoolClassSummaryResponse toSummary(SchoolClass schoolClass) {
        long learnerCount = studentRepository.findBySchoolClassId(schoolClass.getId()).size();
        Teacher teacher = schoolClass.getClassTeacher();
        return new SchoolClassDtos.SchoolClassSummaryResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getCode(),
                schoolClass.getLevel(),
                schoolClass.getAcademicYear(),
                schoolClass.getStream(),
                schoolClass.getCapacity(),
                schoolClass.getStatus(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? ((teacher.getFirstName() == null ? "" : teacher.getFirstName()) + " " + (teacher.getLastName() == null ? "" : teacher.getLastName())).trim() : null,
                (int) learnerCount,
                schoolClass.getSubjects().size()
        );
    }

    private SchoolClassDtos.ClassRosterResponse toRoster(SchoolClass schoolClass) {
        List<SchoolClassDtos.LearnerSummary> learners = studentRepository.findBySchoolClassId(schoolClass.getId()).stream()
                .sorted(Comparator.comparing(Student::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(student -> new SchoolClassDtos.LearnerSummary(
                        student.getId(),
                        student.getAdmissionNumber(),
                        ((student.getFirstName() == null ? "" : student.getFirstName()) + " " + (student.getLastName() == null ? "" : student.getLastName())).trim(),
                        SchoolClassDtos.StudentStatusSummary.valueOf(student.getStatus().name())
                ))
                .toList();

        List<SchoolClassDtos.SubjectSummary> subjects = schoolClass.getSubjects().stream()
                .sorted(Comparator.comparing(Subject::getCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(subject -> new SchoolClassDtos.SubjectSummary(subject.getId(), subject.getCode(), subject.getName()))
                .toList();

        Teacher teacher = schoolClass.getClassTeacher();
        SchoolClassDtos.TeacherSummary teacherSummary = teacher == null ? null : new SchoolClassDtos.TeacherSummary(
                teacher.getId(),
                teacher.getEmployeeNumber(),
                ((teacher.getFirstName() == null ? "" : teacher.getFirstName()) + " " + (teacher.getLastName() == null ? "" : teacher.getLastName())).trim()
        );

        int capacityUsage = schoolClass.getCapacity() == null || schoolClass.getCapacity() <= 0
                ? 0
                : (int) Math.round((learners.size() * 100.0) / schoolClass.getCapacity());

        return new SchoolClassDtos.ClassRosterResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getCode(),
                schoolClass.getLevel(),
                schoolClass.getAcademicYear(),
                schoolClass.getStream(),
                schoolClass.getCapacity(),
                schoolClass.getStatus(),
                teacherSummary,
                learners,
                subjects,
                learners.size(),
                capacityUsage
        );
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
