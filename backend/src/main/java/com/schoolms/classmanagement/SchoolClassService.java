package com.schoolms.classmanagement;

import com.schoolms.common.AppException;
import com.schoolms.exam.ExamRepository;
import com.schoolms.marks.MarkRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public List<SchoolClassDtos.SchoolClassSummaryResponse> list(boolean includeInactive) {
        List<SchoolClass> classes = includeInactive
                ? classRepository.findAll()
                : classRepository.findByStatus(SchoolClassStatus.ACTIVE);
        return classes.stream()
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
        if (examRepository.existsBySchoolClassId(id)) {
            throw new AppException("Cannot delete class because exams are assigned to it", HttpStatus.CONFLICT);
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
        String level = normalizeRequired(request.standard(), "Standard is required");
        String stream = normalizeRequired(request.streamSection(), "Stream / Section is required").toUpperCase(Locale.ROOT);
        String academicYear = normalizeRequired(request.academicYear(), "Academic Year is required");
        if (!academicYear.matches("\\d{4}")) {
            throw new AppException("Academic Year must be a 4-digit year", HttpStatus.BAD_REQUEST);
        }
        String className = "Standard " + level + stream;

        boolean duplicateExists = id == null
                ? classRepository.existsByLevelIgnoreCaseAndStreamIgnoreCaseAndAcademicYearIgnoreCase(level, stream, academicYear)
                : classRepository.existsByLevelIgnoreCaseAndStreamIgnoreCaseAndAcademicYearIgnoreCaseAndIdNot(level, stream, academicYear, id);
        if (duplicateExists) {
            throw new AppException(
                    "Class already exists for Standard " + level + ", Stream " + stream + ", Academic Year " + academicYear,
                    HttpStatus.CONFLICT
            );
        }

        String code = generateClassCode(level, stream, academicYear);
        if (id == null ? classRepository.existsByCodeIgnoreCase(code) : classRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new AppException("Class code already exists for generated class values", HttpStatus.CONFLICT);
        }

        schoolClass.setName(className);
        schoolClass.setCode(code);
        schoolClass.setLevel(level);
        schoolClass.setAcademicYear(academicYear);
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
        String standard = resolveStandard(schoolClass);
        String stream = resolveStream(schoolClass);
        String year = resolveAcademicYear(schoolClass);
        return new SchoolClassDtos.SchoolClassSummaryResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getCode(),
                standard,
                year,
                stream,
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

        String standard = resolveStandard(schoolClass);
        String stream = resolveStream(schoolClass);
        String year = resolveAcademicYear(schoolClass);

        return new SchoolClassDtos.ClassRosterResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getCode(),
                standard,
                year,
                stream,
                schoolClass.getCapacity(),
                schoolClass.getStatus(),
                teacherSummary,
                learners,
                subjects,
                learners.size(),
                capacityUsage
        );
    }

    private String generateClassCode(String level, String stream, String academicYear) {
        return ("STANDARD-" + level + "-" + stream + "-" + academicYear).replaceAll("\\s+", "-").toUpperCase(Locale.ROOT);
    }

    private String resolveStandard(SchoolClass schoolClass) {
        String direct = normalizeOptional(schoolClass.getLevel());
        if (direct != null) {
            return direct;
        }
        String fromName = extractNamePart(schoolClass.getName(), 1);
        if (fromName != null) {
            return fromName;
        }
        String[] codeParts = splitCode(schoolClass.getCode());
        return codeParts.length > 1 ? codeParts[1] : null;
    }

    private String resolveStream(SchoolClass schoolClass) {
        String direct = normalizeOptional(schoolClass.getStream());
        if (direct != null) {
            return direct;
        }
        String fromName = extractNamePart(schoolClass.getName(), 2);
        if (fromName != null) {
            return fromName.toUpperCase(Locale.ROOT);
        }
        String[] codeParts = splitCode(schoolClass.getCode());
        return codeParts.length > 2 ? codeParts[2] : null;
    }

    private String resolveAcademicYear(SchoolClass schoolClass) {
        String direct = normalizeOptional(schoolClass.getAcademicYear());
        if (direct != null) {
            return direct;
        }
        String className = normalizeOptional(schoolClass.getName());
        if (className != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(className);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        String[] codeParts = splitCode(schoolClass.getCode());
        return codeParts.length > 3 ? codeParts[3] : null;
    }

    private String extractNamePart(String name, int group) {
        String normalizedName = normalizeOptional(name);
        if (normalizedName == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:grade|standard)\\s*([a-z0-9]+)\\s*([a-z])?", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(normalizedName);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(group);
        return normalizeOptional(value);
    }

    private String[] splitCode(String code) {
        String normalized = normalizeOptional(code);
        if (normalized == null) {
            return new String[0];
        }
        return normalized.split("-");
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
