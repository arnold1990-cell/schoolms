package com.schoolms.subject;

import com.schoolms.common.AppException;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public List<Subject> list() {
        return subjectRepository.findAll();
    }

    @Transactional
    public Subject create(String code, String name) {
        Subject subject = new Subject();
        applyUpsert(subject, code, name);
        return subjectRepository.save(subject);
    }

    @Transactional
    public Subject update(Long id, String code, String name) {
        Subject subject = getSubject(id);
        applyUpsert(subject, code, name);
        return subjectRepository.save(subject);
    }

    @Transactional
    public void delete(Long id) {
        subjectRepository.delete(getSubject(id));
    }

    @Transactional
    public Subject assignTeacher(Long subjectId, Long teacherId) {
        Subject subject = getSubject(subjectId);
        if (teacherId == null) {
            subject.setAssignedTeacher(null);
        } else {
            subject.setAssignedTeacher(getTeacher(teacherId));
        }
        return subjectRepository.save(subject);
    }

    private void applyUpsert(Subject subject, String code, String name) {
        subject.setCode(normalizeCode(code));
        subject.setName(normalize(name));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeCode(String value) {
        return normalize(value).toUpperCase();
    }

    private Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new AppException("Subject not found", HttpStatus.NOT_FOUND));
    }

    private Teacher getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new AppException("Teacher not found", HttpStatus.NOT_FOUND));
    }
}
