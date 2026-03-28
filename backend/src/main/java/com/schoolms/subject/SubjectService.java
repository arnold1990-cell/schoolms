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

    @Transactional(readOnly = true)
    public List<SubjectResponse> list() {
        return subjectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        String normalizedCode = normalizeCode(request.code());
        ensureCodeUniqueForCreate(normalizedCode);

        Subject subject = new Subject();
        apply(subject, request, normalizedCode);
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = getSubject(id);
        String normalizedCode = normalizeCode(request.code());
        ensureCodeUniqueForUpdate(normalizedCode, id);

        apply(subject, request, normalizedCode);
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = getSubject(id);
        subjectRepository.delete(subject);
    }

    @Transactional
    public SubjectResponse assignTeacher(Long subjectId, Long teacherId) {
        Subject subject = getSubject(subjectId);
        subject.setAssignedTeacher(teacherId == null ? null : getTeacher(teacherId));
        return toResponse(subjectRepository.save(subject));
    }

    private void apply(Subject subject, SubjectRequest request, String normalizedCode) {
        subject.setCode(normalizedCode);
        subject.setName(normalizeName(request.name()));
        subject.setAssignedTeacher(request.assignedTeacherId() == null ? null : getTeacher(request.assignedTeacherId()));
    }

    private void ensureCodeUniqueForCreate(String normalizedCode) {
        if (subjectRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new AppException("Subject code already exists", HttpStatus.CONFLICT);
        }
    }

    private void ensureCodeUniqueForUpdate(String normalizedCode, Long id) {
        if (subjectRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new AppException("Subject code already exists", HttpStatus.CONFLICT);
        }
    }

    private Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new AppException("Subject not found", HttpStatus.NOT_FOUND));
    }

    private Teacher getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new AppException("Teacher not found", HttpStatus.NOT_FOUND));
    }

    private SubjectResponse toResponse(Subject subject) {
        Long assignedTeacherId = subject.getAssignedTeacher() == null ? null : subject.getAssignedTeacher().getId();
        return new SubjectResponse(subject.getId(), subject.getCode(), subject.getName(), assignedTeacherId);
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (normalized.isBlank()) {
            throw new AppException("Code is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new AppException("Name is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }
}
