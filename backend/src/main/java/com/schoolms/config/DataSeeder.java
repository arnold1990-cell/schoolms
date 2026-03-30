package com.schoolms.config;

import com.schoolms.academicsession.AcademicSession;
import com.schoolms.academicsession.AcademicSessionRepository;
import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.classmanagement.SchoolClassStatus;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.student.StudentStatus;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.EmploymentType;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherGender;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.teacher.TeacherStatus;
import com.schoolms.teacher.TeacherTitle;
import com.schoolms.term.Term;
import com.schoolms.term.TermRepository;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final TermRepository termRepository;

    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedTeacherAccount();
        if (seedDemoData) {
            seedDemoRecords();
        }
    }

    private void seedAdmin() {
        userRepository.findByEmail("admin@schoolms.com").ifPresentOrElse(admin -> {
            boolean passwordMatches = passwordEncoder.matches("Admin123!", admin.getPassword());
            boolean passwordMismatch = !passwordMatches;
            boolean roleMismatch = admin.getRole() != Role.ADMIN;
            boolean disabled = !admin.isEnabled();

            log.info("Admin seed check: email={}, passwordMatches={}, role={}, enabled={}",
                    admin.getEmail(), passwordMatches, admin.getRole(), admin.isEnabled());

            if (passwordMismatch || roleMismatch || disabled) {
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
                log.warn("Admin account data mismatch detected. Password/role/enabled values were reset to defaults.");
            }
        }, () -> {
            User admin = new User();
            admin.setEmail("admin@schoolms.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Admin account created with default credentials.");
        });
    }

    private void seedDemoRecords() {
        if (teacherRepository.count() > 0 || studentRepository.count() > 0 || classRepository.count() > 0 || subjectRepository.count() > 0) {
            log.info("Demo data seeding skipped because records already exist.");
            return;
        }

        SchoolClass classOne = createClass("7", "A", "2026", 45);
        SchoolClass classTwo = createClass("8", "B", "2026", 45);

        Teacher linkedTeacher = ensureTeacherProfileForDemoAccount();
        Teacher t1 = createTeacher("Alice", "Johnson", "TCH-1001", "alice.johnson@schoolms.com", "555-0101");
        Teacher t2 = createTeacher("Brian", "Kim", "TCH-1002", "brian.kim@schoolms.com", "555-0102");

        Subject math = createSubject("MATH101", "Mathematics", linkedTeacher);
        createSubject("ENG101", "English Language", t2);
        createSubject("SCI101", "Integrated Science", t1);

        classOne.setClassTeacher(linkedTeacher);
        classOne.getSubjects().add(math);
        classRepository.save(classOne);
        linkedTeacher.getAssignedClasses().add(classOne);
        teacherRepository.save(linkedTeacher);

        createStudent("Liam", "Walker", "ADM-001", classOne, "ACTIVE", "555-0201");
        createStudent("Emma", "Davis", "ADM-002", classOne, "ACTIVE", "555-0202");
        createStudent("Noah", "Brown", "ADM-003", classTwo, "ACTIVE", "555-0203");

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setActive(true);
        session = sessionRepository.save(session);

        Term term = new Term();
        term.setName("Term 1");
        term.setActive(true);
        term.setAcademicSession(session);
        termRepository.save(term);

        log.info("Demo data seeded successfully (teachers, students, classes, subjects, session, term).");
    }

    private void seedTeacherAccount() {
        userRepository.findByEmail("teacher@schoolms.com").ifPresentOrElse(teacher -> {
            boolean passwordMatches = passwordEncoder.matches("Teacher123!", teacher.getPassword());
            boolean roleMismatch = teacher.getRole() != Role.TEACHER;
            boolean disabled = !teacher.isEnabled();

            if (!passwordMatches || roleMismatch || disabled) {
                teacher.setPassword(passwordEncoder.encode("Teacher123!"));
                teacher.setRole(Role.TEACHER);
                teacher.setEnabled(true);
                userRepository.save(teacher);
                log.warn("Teacher account data mismatch detected. Password/role/enabled values were reset to defaults.");
            }
        }, () -> {
            User teacher = new User();
            teacher.setEmail("teacher@schoolms.com");
            teacher.setPassword(passwordEncoder.encode("Teacher123!"));
            teacher.setRole(Role.TEACHER);
            teacher.setEnabled(true);
            userRepository.save(teacher);
            log.info("Teacher account created with default credentials.");
        });

        ensureTeacherProfileForDemoAccount();
    }

    private Teacher createTeacher(String firstName, String lastName, String staffCode, String email, String phone) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Teacher123!"));
        user.setRole(Role.TEACHER);
        user.setEnabled(true);
        user = userRepository.save(user);

        return teacherRepository.save(buildTeacherProfile(new Teacher(), user, firstName, lastName, staffCode, phone, email));
    }

    private Teacher ensureTeacherProfileForDemoAccount() {
        User teacherUser = userRepository.findByEmail("teacher@schoolms.com")
                .orElseThrow(() -> new IllegalStateException("Teacher seed account was not created"));

        Teacher teacher = teacherRepository.findByUserId(teacherUser.getId())
                .or(() -> teacherRepository.findByEmailIgnoreCase(teacherUser.getEmail()))
                .orElseGet(Teacher::new);
        buildTeacherProfile(teacher, teacherUser, "Demo", "Teacher", "TCH-DEMO", "555-0000", teacherUser.getEmail());
        return teacherRepository.save(teacher);
    }

    private Subject createSubject(String code, String name, Teacher teacher) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        subject.setAssignedTeacher(teacher);
        return subjectRepository.save(subject);
    }

    private Teacher buildTeacherProfile(Teacher teacher, User user, String firstName, String lastName, String employeeNumber, String phoneNumber, String email) {
        teacher.setUser(user);
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmployeeNumber(employeeNumber);
        teacher.setPhoneNumber(phoneNumber);
        teacher.setEmail(email);
        teacher.setTitle(teacher.getTitle() == null ? TeacherTitle.MR : teacher.getTitle());
        teacher.setGender(teacher.getGender() == null ? TeacherGender.OTHER : teacher.getGender());
        teacher.setDepartment(isBlank(teacher.getDepartment()) ? "Academics" : teacher.getDepartment());
        teacher.setSpecialization(isBlank(teacher.getSpecialization()) ? "General Studies" : teacher.getSpecialization());
        teacher.setEmploymentType(teacher.getEmploymentType() == null ? EmploymentType.FULL_TIME : teacher.getEmploymentType());
        teacher.setHireDate(teacher.getHireDate() == null ? LocalDate.now() : teacher.getHireDate());
        teacher.setStatus(teacher.getStatus() == null ? TeacherStatus.ACTIVE : teacher.getStatus());
        teacher.setAddress(isBlank(teacher.getAddress()) ? "School Campus" : teacher.getAddress());
        teacher.setStaffRole(isBlank(teacher.getStaffRole()) ? "SUBJECT_TEACHER" : teacher.getStaffRole());
        return teacher;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void createStudent(String firstName, String lastName, String admissionNumber, SchoolClass schoolClass, String status, String guardianPhone) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setAdmissionNumber(admissionNumber);
        student.setGender("MALE");
        student.setDateOfBirth(java.time.LocalDate.of(2014, 1, 1));
        student.setGrade(schoolClass.getName());
        student.setEnrollmentDate(java.time.LocalDate.now());
        student.setGuardianName("Parent of " + firstName);
        student.setGuardianRelationship("PARENT");
        student.setGuardianPhone(guardianPhone);
        student.setAddress("Unknown");
        student.setStatus(StudentStatus.valueOf(status));
        student.setUsesTransport(false);
        student.setSchoolClass(schoolClass);
        studentRepository.save(student);
    }

    private SchoolClass createClass(String level, String stream, String academicYear, Integer capacity) {
        SchoolClass schoolClass = new SchoolClass();
        String normalizedStream = stream == null ? null : stream.trim().toUpperCase(Locale.ROOT);
        schoolClass.setLevel(level);
        schoolClass.setStream(normalizedStream);
        schoolClass.setAcademicYear(academicYear);
        schoolClass.setName("Grade " + level + normalizedStream);
        schoolClass.setCode(("GRADE-" + level + "-" + normalizedStream + "-" + academicYear).replaceAll("\\s+", "-").toUpperCase(Locale.ROOT));
        schoolClass.setCapacity(capacity);
        schoolClass.setStatus(SchoolClassStatus.ACTIVE);
        return classRepository.save(schoolClass);
    }
}
