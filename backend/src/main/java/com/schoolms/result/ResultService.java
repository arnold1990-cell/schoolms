package com.schoolms.result;

import com.schoolms.marks.Mark;
import com.schoolms.marks.MarkRepository;
import com.schoolms.student.StudentRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResultService {
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;

    public List<Map<String, Object>> studentResult(Long studentId) {
        return markRepository.findByStudentId(studentId).stream().map(this::toRow).toList();
    }

    public List<Map<String, Object>> classResult(Long classId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        studentRepository.findBySchoolClassId(classId).forEach(student -> {
            double total = markRepository.findByStudentId(student.getId()).stream().mapToDouble(Mark::getScore).sum();
            double avg = markRepository.findByStudentId(student.getId()).stream().mapToDouble(Mark::getScore).average().orElse(0);
            rows.add(Map.of("studentId", student.getId(), "name", student.getFirstName() + " " + student.getLastName(), "total", total, "average", avg));
        });
        rows.sort(Comparator.comparingDouble(r -> -((Number) r.get("total")).doubleValue()));
        for (int i = 0; i < rows.size(); i++) rows.get(i).put("rank", i + 1);
        return rows;
    }

    private Map<String, Object> toRow(Mark mark) {
        return Map.of("exam", mark.getExam().getTitle(), "subject", mark.getExam().getSubject().getName(), "score", mark.getScore(), "grade", mark.getGrade());
    }
}
