package com.schoolms.marks;

import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.classmanagement.SchoolClassStatus;
import com.schoolms.common.AppException;
import com.schoolms.exam.Exam;
import com.schoolms.exam.ExamRepository;
import com.schoolms.exam.ExamStatus;
import com.schoolms.exam.ExamTerm;
import com.schoolms.grading.GradingService;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkService {
    private static final List<String> EXAM_TYPES = List.of("TEST", "MIDTERM", "EXAM", "ASSIGNMENT");

    private final MarkRepository markRepository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final GradingService gradingService;

    @Transactional(readOnly = true)
    public MarkController.MarksSetupResponse setupData(String userEmail) {
        User user = findUser(userEmail);
        Teacher teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
        if (user.getRole() == Role.TEACHER && teacher == null) {
            return new MarkController.MarksSetupResponse(
                    List.of(),
                    List.of(),
                    EXAM_TYPES,
                    List.of(ExamTerm.TERM_1, ExamTerm.TERM_2, ExamTerm.TERM_3),
                    false,
                    "Your teacher account is not linked to a teacher profile. Please contact an administrator."
            );
        }

        List<SchoolClass> classes = classRepository.findAll().stream()
                .filter(schoolClass -> schoolClass.getStatus() == SchoolClassStatus.ACTIVE)
                .filter(schoolClass -> canAccessClass(user, teacher, schoolClass))
                .sorted(Comparator.comparing(SchoolClass::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<Subject> subjects = subjectRepository.findAll().stream()
                .filter(subject -> canAccessSubject(user, teacher, subject))
                .sorted(Comparator.comparing(Subject::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        String message = classes.isEmpty()
                ? "No classes available for marks entry."
                : "Marks setup loaded successfully.";

        return new MarkController.MarksSetupResponse(
                classes.stream().map(c -> new MarkController.ClassOption(c.getId(), c.getName(), c.getCode())).toList(),
                subjects.stream().map(s -> new MarkController.SubjectOption(s.getId(), s.getName(), s.getCode())).toList(),
                EXAM_TYPES,
                List.of(ExamTerm.TERM_1, ExamTerm.TERM_2, ExamTerm.TERM_3),
                true,
                message
        );
    }

    @Transactional(readOnly = true)
    public List<MarkController.LearnerMarkRow> learnersForSelection(String userEmail, Long classId, Long subjectId, ExamTerm term, String examType) {
        validateSelection(classId, subjectId, term, examType);
        User user = findUser(userEmail);
        Teacher teacher = resolveTeacher(user);

        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException("Subject not found", HttpStatus.NOT_FOUND));
        ensureTeacherScope(user, teacher, schoolClass, subject);

        Exam exam = findExam(classId, subjectId, term, normalizeExamType(examType));

        return studentRepository.findBySchoolClassId(classId).stream()
                .sorted(Comparator.comparing(student -> fullName(student).toLowerCase(Locale.ROOT)))
                .map(student -> toLearnerRow(student, exam))
                .toList();
    }

    @Transactional
    public List<MarkController.LearnerMarkRow> saveDraft(String userEmail, MarkController.BulkMarkRequest request) {
        return saveOrSubmit(userEmail, request);
    }

    @Transactional
    public List<MarkController.LearnerMarkRow> submit(String userEmail, MarkController.BulkMarkRequest request) {
        return saveOrSubmit(userEmail, request);
    }

    @Transactional(readOnly = true)
    public List<Mark> listByExam(Long examId) {
        return markRepository.findByExamId(examId);
    }

    @Transactional
    public Mark createSingle(String userEmail, MarkController.MarkRequest request) {
        User user = findUser(userEmail);
        Teacher teacher = resolveTeacher(user);
        Exam exam = examRepository.findById(request.examId()).orElseThrow(() -> new AppException("Exam not found", HttpStatus.NOT_FOUND));
        Student student = studentRepository.findById(request.studentId()).orElseThrow(() -> new AppException("Learner not found", HttpStatus.NOT_FOUND));
        ensureTeacherScope(user, teacher, exam.getSchoolClass(), exam.getSubject());

        validateScore(request.score(), exam.getTotalMarks());

        markRepository.findByExamIdAndStudentId(request.examId(), request.studentId()).ifPresent(m -> {
            throw new AppException("Duplicate mark entry", HttpStatus.CONFLICT);
        });

        Mark mark = new Mark();
        mark.setTeacher(resolveMarkTeacher(user, teacher, exam));
        mark.setExam(exam);
        mark.setStudent(student);
        mark.setScore(request.score());
        mark.setGrade(gradingService.resolveGrade(request.score()));
        return markRepository.save(mark);
    }

    @Transactional
    public Mark updateSingle(Long id, MarkController.MarkRequest request) {
        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new AppException("Mark not found", HttpStatus.NOT_FOUND));
        Exam exam = examRepository.findById(request.examId())
                .orElseThrow(() -> new AppException("Exam not found", HttpStatus.NOT_FOUND));
        validateScore(request.score(), exam.getTotalMarks());
        mark.setScore(request.score());
        mark.setGrade(gradingService.resolveGrade(request.score()));
        return markRepository.save(mark);
    }

    private List<MarkController.LearnerMarkRow> saveOrSubmit(String userEmail, MarkController.BulkMarkRequest request) {
        validateSelection(request.classId(), request.subjectId(), request.term(), request.examType());
        if (request.entries() == null || request.entries().isEmpty()) {
            throw new AppException("At least one learner mark entry is required", HttpStatus.BAD_REQUEST);
        }

        User user = findUser(userEmail);
        Teacher teacher = resolveTeacher(user);
        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new AppException("Class not found", HttpStatus.NOT_FOUND));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new AppException("Subject not found", HttpStatus.NOT_FOUND));
        ensureTeacherScope(user, teacher, schoolClass, subject);

        String normalizedExamType = normalizeExamType(request.examType());
        Exam exam = findExam(request.classId(), request.subjectId(), request.term(), normalizedExamType);
        if (exam == null) {
            exam = new Exam();
            exam.setTitle(normalizedExamType);
            exam.setExamCode("EXM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
            exam.setSchoolClass(schoolClass);
            exam.setSubject(subject);
            exam.setTerm(request.term());
            exam.setStatus(ExamStatus.DRAFT);
            exam.setTotalMarks(100d);
            exam = examRepository.save(exam);
        }

        Teacher effectiveTeacher = resolveMarkTeacher(user, teacher, exam);

        for (MarkController.LearnerMarkEntry entry : request.entries()) {
            if (entry.learnerId() == null) {
                throw new AppException("Learner id is required", HttpStatus.BAD_REQUEST);
            }
            if (entry.mark() == null) {
                continue;
            }
            validateScore(entry.mark(), exam.getTotalMarks());
            Student learner = studentRepository.findById(entry.learnerId())
                    .orElseThrow(() -> new AppException("Learner not found", HttpStatus.NOT_FOUND));
            if (learner.getSchoolClass() == null || !Objects.equals(learner.getSchoolClass().getId(), schoolClass.getId())) {
                throw new AppException("Learner is not assigned to the selected class", HttpStatus.BAD_REQUEST);
            }

            Mark mark = markRepository.findByExamIdAndStudentId(exam.getId(), learner.getId()).orElseGet(Mark::new);
            mark.setExam(exam);
            mark.setStudent(learner);
            mark.setTeacher(effectiveTeacher);
            mark.setScore(entry.mark());
            mark.setGrade(gradingService.resolveGrade(entry.mark()));
            markRepository.save(mark);
        }

        return learnersForSelection(userEmail, request.classId(), request.subjectId(), request.term(), normalizedExamType);
    }

    private MarkController.LearnerMarkRow toLearnerRow(Student student, Exam exam) {
        if (exam == null) {
            return new MarkController.LearnerMarkRow(student.getId(), fullName(student), null, null);
        }
        return markRepository.findByExamIdAndStudentId(exam.getId(), student.getId())
                .map(mark -> new MarkController.LearnerMarkRow(student.getId(), fullName(student), mark.getScore(), mark.getGrade()))
                .orElseGet(() -> new MarkController.LearnerMarkRow(student.getId(), fullName(student), null, null));
    }

    private Exam findExam(Long classId, Long subjectId, ExamTerm term, String examType) {
        return examRepository.findAll().stream()
                .filter(exam -> exam.getSchoolClass() != null && Objects.equals(exam.getSchoolClass().getId(), classId))
                .filter(exam -> exam.getSubject() != null && Objects.equals(exam.getSubject().getId(), subjectId))
                .filter(exam -> exam.getTerm() == term)
                .filter(exam -> examType.equalsIgnoreCase(exam.getTitle()))
                .findFirst()
                .orElse(null);
    }

    private void validateSelection(Long classId, Long subjectId, ExamTerm term, String examType) {
        if (classId == null || subjectId == null || term == null || examType == null || examType.isBlank()) {
            throw new AppException("Class, subject, exam type, and term are required", HttpStatus.BAD_REQUEST);
        }
        String normalized = normalizeExamType(examType);
        if (!EXAM_TYPES.contains(normalized)) {
            throw new AppException("Unsupported exam type", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeExamType(String examType) {
        return examType == null ? "" : examType.trim().toUpperCase(Locale.ROOT);
    }

    private void validateScore(Double score, Double totalMarks) {
        double maxScore = totalMarks == null || totalMarks <= 0 ? 100d : totalMarks;
        if (score < 0 || score > maxScore) {
            throw new AppException("Score out of range", HttpStatus.BAD_REQUEST);
        }
    }

    private String fullName(Student student) {
        String middleName = student.getMiddleName() == null ? "" : student.getMiddleName().trim();
        return (student.getFirstName() + " " + middleName + " " + student.getLastName()).trim().replaceAll("\\s+", " ");
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    private Teacher resolveTeacher(User user) {
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .or(() -> teacherRepository.findByEmailIgnoreCase(user.getEmail())
                        .filter(candidate -> candidate.getUser() != null
                                && Objects.equals(candidate.getUser().getId(), user.getId())))
                .orElse(null);
        if (user.getRole() == Role.TEACHER && teacher == null) {
            throw new AppException("Teacher profile is not linked to this account", HttpStatus.FORBIDDEN);
        }
        return teacher;
    }

    private void ensureTeacherScope(User user, Teacher teacher, SchoolClass schoolClass, Subject subject) {
        if (user.getRole() != Role.TEACHER) {
            return;
        }
        boolean classAllowed = schoolClass != null && canAccessClass(user, teacher, schoolClass);
        boolean subjectAllowed = subject != null && canAccessSubject(user, teacher, subject);
        if (!classAllowed || !subjectAllowed) {
            throw new AppException("You are not allowed to enter marks for this class/subject", HttpStatus.FORBIDDEN);
        }
    }

    private boolean canAccessClass(User user, Teacher teacher, SchoolClass schoolClass) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (teacher == null || schoolClass == null) {
            return false;
        }
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            return false;
        }
        boolean explicitlyAssignedToClass = teacher.getAssignedClasses().stream()
                .anyMatch(assignedClass -> Objects.equals(assignedClass.getId(), schoolClass.getId()));
        boolean isClassTeacher = schoolClass.getClassTeacher() != null && Objects.equals(schoolClass.getClassTeacher().getId(), teacher.getId());
        boolean teachesAssignedSubject = schoolClass.getSubjects().stream()
                .anyMatch(subject -> subject.getAssignedTeacher() != null && Objects.equals(subject.getAssignedTeacher().getId(), teacher.getId()));
        return explicitlyAssignedToClass || isClassTeacher || teachesAssignedSubject;
    }

    private boolean canAccessSubject(User user, Teacher teacher, Subject subject) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (teacher == null || subject == null) {
            return false;
        }
        boolean directlyAssignedSubject = subject.getAssignedTeacher() != null
                && Objects.equals(subject.getAssignedTeacher().getId(), teacher.getId());
        boolean subjectInTeacherAssignedClass = teacher.getAssignedClasses().stream()
                .anyMatch(assignedClass -> assignedClass.getSubjects().stream()
                        .anyMatch(classSubject -> Objects.equals(classSubject.getId(), subject.getId())));
        return directlyAssignedSubject || subjectInTeacherAssignedClass;
    }

    private Teacher resolveMarkTeacher(User user, Teacher teacher, Exam exam) {
        if (teacher != null) {
            return teacher;
        }
        Teacher fromSubject = exam.getSubject() != null ? exam.getSubject().getAssignedTeacher() : null;
        if (fromSubject == null && user.getRole() != Role.ADMIN) {
            throw new AppException("No teacher is assigned for this mark entry", HttpStatus.BAD_REQUEST);
        }
        return fromSubject;
    }
}
